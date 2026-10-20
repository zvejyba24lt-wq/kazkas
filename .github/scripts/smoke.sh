#!/usr/bin/env bash
# Installs the APK on the emulator, walks through the screens and weather scenes, saves
# screenshots and fails if the app crashed (Java or native) or its process died.
set -u
APK="$1"
OUT="$2"
PKG=lt.marcomultiservices.kimba
ACT="$PKG/.MainActivity"
mkdir -p "$OUT"

# The lake animates every frame, so uiautomator never sees an idle UI: no node lookups here,
# only intents and screen coordinates.
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1 (top: $(adb shell dumpsys activity activities | grep -m1 topResumedActivity | tr -s ' '))"; }
demo() { adb shell am start -n "$ACT" "$@" > /dev/null; }

sleep 45
adb install -r "$APK" || exit 1
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
adb logcat -c
# AppCompat syncs its stored app locale once on the first start, so start once, then switch to Lithuanian.
adb shell am start -W -n "$ACT"; sleep 12
adb shell am force-stop "$PKG"
adb shell cmd locale set-app-locales "$PKG" --locales lt || true
adb shell am start -W -n "$ACT"
for i in 1 2 3; do sleep 3; adb shell dumpsys activity activities | grep -m1 topResumedActivity; done
FAIL=0
adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -q "$PKG" || { echo "App is not in the foreground after start"; FAIL=1; }
SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1); W=${SIZE%x*}; H=${SIZE#*x}
echo "screen $W x $H"

shot 01_sveiki 4
demo -e place Vilnius; shot 02_ezeras_tikras_oras 10
# Hold the cast button (bottom centre) for ~1 s, then watch the float.
adb shell input swipe $((W/2)) $((H*84/100)) $((W/2)) $((H*84/100)) 1000; shot 03_uzmesta 3
shot 04_laukiam 4
demo -e fight pike; shot 05_kova 1.5

demo -e scene storm; shot 06_perkunija 4
demo -e scene dawnfog; shot 07_rukas_ausra 4
demo -e scene night; shot 08_naktis 4
demo -e scene snow; shot 09_sniegas 4
demo -e scene rain; shot 10_lietus 4
demo -e scene dusk; shot 11_sutemos 4
demo -e scene day; shot 12_diena 4
demo -e catch moon_pike; shot 13_auksine_lydeka 3
demo -e catch perch; shot 14_eserys 3
demo -e catch carp; shot 15_karpis 3
demo -e catch catfish; shot 16_samas 3
demo -e scene live -e screen book; shot 17_zuvu_knyga 4
adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/5)) 400; shot 18_auksines_zuvys 2
demo -e screen forecast; shot 19_prognoze 6
demo -e screen settings; shot 20_nustatymai 3
demo -e screen lake; sleep 2

adb shell am force-stop "$PKG"
adb shell cmd locale set-app-locales "$PKG" --locales de; sleep 2
adb shell am start -W -n "$ACT"; shot 21_deutsch 10
adb shell am force-stop "$PKG"
adb shell cmd locale set-app-locales "$PKG" --locales en; sleep 2
adb shell am start -W -n "$ACT"; shot 22_english 10

adb logcat -d > "$OUT/logcat_full.txt"
grep -E "$PKG|AndroidRuntime|Fatal signal|DEBUG +:|ANR in|has died|RuntimeShader|WM-" "$OUT/logcat_full.txt" | tail -n 400 > "$OUT/logcat.txt"
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
