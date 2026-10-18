#!/usr/bin/env bash
# Installs the APK on the emulator, walks through the first lessons and the parents' area,
# saves screenshots and fails if the app crashed or its process died.
set -u
APK="$1"
OUT="$2"
PKG=lt.marcomultiservices.skaitaupats
mkdir -p "$OUT"

shot() { sleep "${2:-2}"; python3 .github/scripts/tap.py; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
tap() { python3 .github/scripts/tap.py "$@"; }

sleep 45; python3 .github/scripts/tap.py
adb install -r "$APK" || exit 1
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
FAIL=0
sleep 8
# On a fresh emulator Google Play services restarts itself once and Android kills apps bound to it
# (logged as "depends on provider ... in dying proc"). Give it time, then bring the app back.
sleep 25
adb shell am start -W -n "$PKG/.MainActivity"; sleep 6
adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -q "$PKG" || { echo "App is not in the foreground after start"; FAIL=1; }
shot 01_zemelapis 2

# Lesson 1 (A): intro, find "a", find "A".
tap "Pamoka 1"; shot 02_raide_a 3
tap "Toliau"; shot 03_surask 2
tap "o"; shot 04_klaida 1
tap "a"; sleep 3
tap "A"; sleep 3; shot 05_pabaiga 2
# The free version shows a (test) ad here; restart the app to leave it.
tap "Į žemėlapį"; shot 05b_reklama 6
adb shell am force-stop "$PKG"; adb shell am start -W -n "$PKG/.MainActivity"; sleep 6

# Lesson 2 (M): intro, find, then join a syllable.
tap "Pamoka 2"; sleep 3
tap "Toliau"; sleep 2
tap "m"; sleep 3
tap "M"; sleep 3; shot 06_sujunk 1
tap "m"; shot 07_sujungta 3
tap "Uždaryti"; sleep 2

# A reading lesson and a story are open in test builds.
swipe() { adb shell input swipe 540 1700 540 700 500; sleep 1; }
swipe
tap "Pamoka 8"; sleep 3; shot 08_skaitome 1
tap "Toliau"; sleep 2; shot 08b_zodis 1
tap "Uždaryti"; sleep 2
swipe; swipe; swipe; swipe; swipe
tap "Pamoka 19"; sleep 3; shot 09_pasakaite 2
tap "Klausyk"; sleep 1; shot 10_skaitymas 1
tap "Toliau"; sleep 2; shot 10b_puslapis 1
tap "Uždaryti"; sleep 2

# Parents' area behind the multiplication gate.
tap "Tėvams"; shot 11_vartai 2
python3 - <<'PY'
import re, subprocess, time
subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
xml = subprocess.run(["adb", "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout
m = re.search(r"(\d+) × (\d+)", xml)
if m:
    ans = int(m.group(1)) * int(m.group(2))
    subprocess.run(["python3", ".github/scripts/tap.py", "Atsakymas", "Answer"])
    time.sleep(1)
    subprocess.run(["adb", "shell", "input", "text", str(ans)])
    time.sleep(1)
    subprocess.run(["adb", "shell", "input", "keyevent", "KEYCODE_BACK"])
    time.sleep(1)
    subprocess.run(["python3", ".github/scripts/tap.py", "Gerai", "OK"])
else:
    print("gate question not found")
PY
shot 12_tevams 3
adb shell input swipe 540 1600 540 600 400; shot 13_tevams_apacia 2

adb logcat -d > "$OUT/logcat_full.txt"
grep -E "$PKG|AndroidRuntime|Fatal signal|DEBUG +:|ANR in|has died|Speaker|Billing|Ads" "$OUT/logcat_full.txt" | tail -n 400 > "$OUT/logcat.txt"
awk -v pkg="$PKG" '
  /FATAL EXCEPTION/ { head=$0; want=1; next }
  want==1 { want=0; if (index($0, "Process: " pkg)) { print head; left=40 } }
  left>0 { print; left--; next }
  index($0, ">>> " pkg " <<<") { print "Native crash:"; print }
  index($0, "ANR in " pkg) { print }
' "$OUT/logcat_full.txt" > "$OUT/crash.txt" || true
gzip -f "$OUT/logcat_full.txt"
if [ -s "$OUT/crash.txt" ]; then
  echo "App crashed:"; head -n 80 "$OUT/crash.txt"; exit 1
fi
[ "$FAIL" = 0 ] || exit 1
echo "Smoke test passed"
