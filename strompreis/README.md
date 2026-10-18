# Strompreisuhr data feed

Daily day-ahead electricity prices for the Strompreisuhr Android app (DE-LU and AT).

- `scripts/fetch_prices.py` fetches from ENTSO-E (secret `ENTSOE_API_KEY`), falling back to
  Energy-Charts (Fraunhofer ISE, CC BY 4.0) when the key is missing or ENTSO-E fails.
- `.github/workflows/strompreis-feed.yml` runs it every 15 min between 11:00 and 15:45 UTC
  and publishes to the `gh-pages` branch.
- `static/` holds files copied as-is: `v1/meta.json` (defaults for "Mein Endpreis", Austrian
  grid areas, forced-update switch) and `datenschutz.html` (privacy policy + imprint).

Feed URLs (the app tries them in this order):
1. `https://zvejyba24lt-wq.github.io/kazkas/v1/de-lu/latest.json` (after enabling Pages)
2. `https://raw.githubusercontent.com/zvejyba24lt-wq/kazkas/gh-pages/v1/de-lu/latest.json`

Update `static/v1/meta.json` every January (AT grid fees) and October (DE levies).
