#!/usr/bin/env python3
"""Day-ahead price feed for the Strompreisuhr Android app.

Primary source: ENTSO-E Transparency Platform (repo secret ENTSOE_API_KEY).
Fallback source: Energy-Charts API by Fraunhofer ISE (no key, CC BY 4.0).

Output (written into the site directory that is published on gh-pages):
  v1/<zone>/latest.json   yesterday, today and (once published) tomorrow, 15-min prices
  v1/<zone>/history.json  last 30 days: daily min/avg/max and hourly averages
  v1/status.json          what the last run did, for debugging

Prices stay exactly as published: EUR/MWh, without VAT. The app converts.
Only the standard library is used, so the workflow needs no installs.
"""
import argparse
import json
import os
import sys
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

TZ = ZoneInfo("Europe/Berlin")  # DE-LU and AT both use CET/CEST
ZONES = {
    "de-lu": {"name": "DE-LU", "eic": "10Y1001A1001A82H", "ec": "DE-LU"},
    "at": {"name": "AT", "eic": "10YAT-APG------L", "ec": "AT"},
}
ENTSOE_URL = "https://web-api.tp.entsoe.eu/api"
EC_URL = "https://api.energy-charts.info/price"
SOURCES = {
    "entsoe": "ENTSO-E Transparency Platform",
    "ec": "Energy-Charts (Fraunhofer ISE), CC BY 4.0",
}
UA = "Strompreisuhr-feed/1.0 (+https://github.com/zvejyba24lt-wq/kazkas)"
STEP = 900
HISTORY_DAYS = 30
PRICE_MIN, PRICE_MAX = -500.0, 4000.0  # SDAC harmonised price limits


def log(*a):
    print(*a, file=sys.stderr, flush=True)


def http_get(url, timeout=60, tries=4):
    last = None
    for attempt in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=timeout) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001 - retry anything network related
            last = e
            time.sleep(3 * 2**attempt)
    raise last


def day_start(d):
    return datetime.combine(d, datetime.min.time(), TZ)


def day_bounds(d):
    """UTC epoch seconds of local midnight and the next local midnight (DST aware)."""
    return int(day_start(d).timestamp()), int(day_start(d + timedelta(days=1)).timestamp())


def parse_duration(res):
    if res.startswith("PT") and res.endswith("M"):
        return int(res[2:-1]) * 60
    if res.startswith("PT") and res.endswith("H"):
        return int(res[2:-1]) * 3600
    if res == "P1D":
        return 86400
    raise ValueError("unsupported resolution " + res)


def fetch_entsoe(zone, key, start, end):
    eic = ZONES[zone]["eic"]
    fmt = "%Y%m%d%H%M"
    params = {
        "securityToken": key,
        "documentType": "A44",
        "in_Domain": eic,
        "out_Domain": eic,
        "periodStart": datetime.fromtimestamp(start, timezone.utc).strftime(fmt),
        "periodEnd": datetime.fromtimestamp(end, timezone.utc).strftime(fmt),
    }
    raw = http_get(ENTSOE_URL + "?" + urllib.parse.urlencode(params))
    root = ET.fromstring(raw)
    ns = root.tag.split("}")[0] + "}" if root.tag.startswith("{") else ""
    if root.tag.endswith("Acknowledgement_MarketDocument"):
        raise RuntimeError("ENTSO-E: " + (root.findtext(f".//{ns}Reason/{ns}text") or "no data"))
    slots = {}
    for ts in root.iter(f"{ns}TimeSeries"):
        contract = ts.findtext(f"{ns}contract_MarketAgreement.type")
        if contract and contract != "A01":  # A01 = day-ahead auction
            continue
        for period in ts.iter(f"{ns}Period"):
            p_start = datetime.strptime(
                period.findtext(f"{ns}timeInterval/{ns}start"), "%Y-%m-%dT%H:%MZ"
            ).replace(tzinfo=timezone.utc)
            p_end = datetime.strptime(
                period.findtext(f"{ns}timeInterval/{ns}end"), "%Y-%m-%dT%H:%MZ"
            ).replace(tzinfo=timezone.utc)
            step = parse_duration(period.findtext(f"{ns}resolution"))
            count = int((p_end - p_start).total_seconds() // step)
            points = {
                int(pt.findtext(f"{ns}position")): float(pt.findtext(f"{ns}price.amount"))
                for pt in period.iter(f"{ns}Point")
            }
            last = None
            for pos in range(1, count + 1):
                # Curve type A03: a missing position repeats the previous price.
                last = points.get(pos, last)
                if last is None:
                    continue
                t = int(p_start.timestamp()) + (pos - 1) * step
                for sub in range(0, step, STEP):
                    slots.setdefault(t + sub, last)
    return slots


def fetch_energy_charts(zone, start, end):
    params = {
        "bzn": ZONES[zone]["ec"],
        "start": datetime.fromtimestamp(start, TZ).strftime("%Y-%m-%d"),
        "end": datetime.fromtimestamp(end - 1, TZ).strftime("%Y-%m-%d"),
    }
    data = json.loads(http_get(EC_URL + "?" + urllib.parse.urlencode(params)))
    secs, prices = data.get("unix_seconds") or [], data.get("price") or []
    step = secs[1] - secs[0] if len(secs) > 1 else STEP
    slots = {}
    for t, p in zip(secs, prices):
        if p is None:
            continue
        for sub in range(0, max(step, STEP), STEP):
            slots.setdefault(int(t) + sub, float(p))
    return slots


def fetch(zone, key, start, end):
    errors = []
    if key:
        try:
            slots = fetch_entsoe(zone, key, start, end)
            if slots:
                return slots, SOURCES["entsoe"], errors
            errors.append("ENTSO-E returned no data")
        except Exception as e:  # noqa: BLE001
            errors.append(f"ENTSO-E failed: {e}")
    else:
        errors.append("ENTSOE_API_KEY not set, using fallback")
    try:
        return fetch_energy_charts(zone, start, end), SOURCES["ec"], errors
    except Exception as e:  # noqa: BLE001
        errors.append(f"Energy-Charts failed: {e}")
        return {}, None, errors


def day_prices(slots, d):
    """Complete, validated list of 15-min prices for local day d, or None."""
    lo, hi = day_bounds(d)
    expected = (hi - lo) // STEP  # 96, or 92/100 on DST change days
    prices = [slots.get(t) for t in range(lo, hi, STEP)]
    if len(prices) != expected or any(p is None for p in prices):
        return None
    if any(p < PRICE_MIN or p > PRICE_MAX for p in prices):
        return None
    return [round(p, 2) for p in prices]


def iso_utc(epoch):
    return datetime.fromtimestamp(epoch, timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def read_json(path):
    try:
        with open(path) as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def write_json(path, doc):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp"
    with open(tmp, "w") as f:
        json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
    os.replace(tmp, path)


def hourly(prices, d):
    lo, _ = day_bounds(d)
    hours = {}
    for i, p in enumerate(prices):
        hours.setdefault((lo + i * STEP) // 3600, []).append(p)
    return [round(sum(v) / len(v), 2) for _, v in sorted(hours.items())]


def process_zone(zone, key, site, today, force, now_iso):
    tomorrow = today + timedelta(days=1)
    latest_path = os.path.join(site, "v1", zone, "latest.json")
    old = read_json(latest_path)
    if old and not force:
        have = {d["date"] for d in old.get("days", [])}
        if tomorrow.isoformat() in have and today.isoformat() in have:
            log(f"[{zone}] tomorrow already published, nothing to do")
            return {"ok": True, "skipped": True}

    start = day_bounds(today - timedelta(days=HISTORY_DAYS))[0]
    end = day_bounds(tomorrow)[1]
    slots, source, errors = fetch(zone, key, start, end)
    for e in errors:
        log(f"[{zone}] {e}")
    if not slots:
        return {"ok": False, "errors": errors}

    days = []
    for d in (today - timedelta(days=1), today, tomorrow):
        prices = day_prices(slots, d)
        if prices is None:
            if d != tomorrow:
                log(f"[{zone}] {d} incomplete or invalid")
            continue
        days.append({"date": d.isoformat(), "start": iso_utc(day_bounds(d)[0]), "prices": prices})
    # A tomorrow identical to today is a stale copy, not a new auction result.
    if len(days) >= 2 and days[-1]["date"] == tomorrow.isoformat() and days[-1]["prices"] == days[-2]["prices"]:
        log(f"[{zone}] tomorrow equals today, rejected as duplicate")
        days.pop()
    if not any(d["date"] == today.isoformat() for d in days):
        return {"ok": False, "errors": errors + ["today missing or invalid, kept previous file"]}

    latest = {
        "schema": 1,
        "zone": ZONES[zone]["name"],
        "currency": "EUR",
        "unit": "EUR/MWh",
        "vat_included": False,
        "resolution": "PT15M",
        "generated_at": now_iso,
        "source": source,
        "days": days,
    }
    changed = not old or old.get("days") != days or old.get("source") != source
    if changed:
        write_json(latest_path, latest)

    hist_days = []
    for i in range(HISTORY_DAYS, -1, -1):
        d = today - timedelta(days=i)
        prices = day_prices(slots, d)
        if prices is None:
            continue
        hist_days.append({
            "date": d.isoformat(),
            "min": min(prices),
            "max": max(prices),
            "avg": round(sum(prices) / len(prices), 2),
            "hourly": hourly(prices, d),
        })
    if hist_days:
        old_hist = read_json(os.path.join(site, "v1", zone, "history.json"))
        if not old_hist or old_hist.get("days") != hist_days:
            write_json(os.path.join(site, "v1", zone, "history.json"), {
                "schema": 1, "zone": ZONES[zone]["name"], "unit": "EUR/MWh",
                "vat_included": False, "generated_at": now_iso, "source": source,
                "days": hist_days,
            })
    log(f"[{zone}] {source}: days {[d['date'] for d in days]}, changed={changed}")
    return {
        "ok": True,
        "source": source,
        "days": [d["date"] for d in days],
        "has_tomorrow": days[-1]["date"] == tomorrow.isoformat(),
        "changed": changed,
        "errors": errors,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("site", help="directory that is published (gh-pages checkout)")
    ap.add_argument("--force", action="store_true", help="fetch even if tomorrow is present")
    ap.add_argument("--today", help="override local date (YYYY-MM-DD), for testing")
    args = ap.parse_args()
    key = os.environ.get("ENTSOE_API_KEY", "").strip()
    today = date.fromisoformat(args.today) if args.today else datetime.now(TZ).date()
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    status = {"run_at": now_iso, "zones": {}}
    for zone in ZONES:
        status["zones"][zone] = process_zone(zone, key, args.site, today, args.force, now_iso)
    write_json(os.path.join(args.site, "v1", "status.json"), status)
    print(json.dumps(status, indent=1))
    # Red workflow (and GitHub's failure e-mail) when a zone has no valid data for today.
    return 0 if all(z["ok"] for z in status["zones"].values()) else 1


if __name__ == "__main__":
    sys.exit(main())
