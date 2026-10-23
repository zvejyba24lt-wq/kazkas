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
shot() { sleep "${2:-2}"; timeout 30 adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1 (top: $(adb shell dumpsys activity activities | grep -m1 topResumedActivity | tr -s ' '))"; }
demo() { timeout 30 adb shell am start -n "$ACT" --ez noConsent true "$@" > /dev/null; }
tap() { timeout 20 adb shell input tap "$1" "$2"; }

sleep 45
adb install -r "$APK" || exit 1
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
adb logcat -c
# AppCompat syncs its stored app locale once on the first start, so start once, then switch to Lithuanian.
timeout 60 adb shell am start -W -n "$ACT" --ez noConsent true; sleep 12
adb shell am force-stop "$PKG"
adb shell cmd locale set-app-locales "$PKG" --locales lt || true
timeout 60 adb shell am start -W -n "$ACT" --ez noConsent true
for i in 1 2 3; do sleep 3; adb shell dumpsys activity activities | grep -m1 topResumedActivity; done
FAIL=0
adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -q "$PKG" || { echo "App is not in the foreground after start"; FAIL=1; }
SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1); W=${SIZE%x*}; H=${SIZE#*x}
echo "screen $W x $H"

shot 01_sveiki 4
demo -e place Vilnius; shot 02_serijos_dovana 10
tap $((W/2)) $((H/2)); shot 03_ezeras_tikras_oras 3
# Hold the cast button (bottom centre) for ~1 s, then watch the float.
timeout 30 adb shell input swipe $((W/2)) $((H*84/100)) $((W/2)) $((H*84/100)) 1000; shot 04_uzmesta 3
shot 05_laukiam 6
demo -e fight pike; shot 06_kova 3
demo -e fight pike --ez net true; shot 06d_graibstas 3
# The other rods: a feeder on its rest watching the tip, the tackle box, a spinning lure coming back.
demo -e screen lake -e tackle feeder; sleep 6
demo -e cast 0.6; shot 06a_fideris 9
demo -e screen lake; sleep 2
demo -e screen lake; sleep 4
demo --ez box true; shot 06b_dezute 6
demo -e screen lake -e tackle spin; sleep 6
demo -e cast 0.8; shot 06c_spiningas 5
demo -e screen lake -e tackle float; sleep 2

demo --ei coins 25000 --ei xp 11000 -e scene storm; shot 07_perkunija 4
demo -e scene dawnfog; shot 08_rukas_ausra 4
demo -e scene night; shot 09_naktis 4
demo -e scene snow; shot 10_sniegas 4
demo -e scene rain; shot 11_lietus 4
demo -e scene dusk; shot 12_sutemos 4
demo -e scene day -e water pond; shot 13_tvenkinys 5
demo -e water river; shot 14_upe 5
demo -e scene dusk -e water sea; shot 15_jura_sutemos 5
demo -e scene day; shot 16_jura_diena 4
demo -e scene night -e trip ruegen; shot 17_riugenas_naktis 5
demo -e scene day -e trip bodensee; shot 18_bodensee 5
demo -e water lake; sleep 5
demo -e catch moon_pike; shot 19_auksine_lydeka 5
demo --ez sharepreview true; shot 19b_pasigyrimas 4
demo --ez sharepreview false; sleep 1
demo -e catch trout; shot 20_upetakis 5
demo -e catch grayling; shot 21_kirslys 5
demo -e catch flounder; shot 22_pleksne 5
demo -e catch cod; shot 23_menke 5
demo -e catch huchen; shot 24_dunojaus_lasisa 5
demo -e water lake; sleep 3
demo -e scene live -e screen book; shot 25_zuvu_knyga 4
adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/5)) 400; shot 26_knyga_toliau 2
demo -e screen shop; shot 27_parduotuve 4
adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/5)) 400; shot 28_parduotuve_toliau 2
demo -e screen map; shot 29_zemelapis 5
demo -e screen lake; sleep 2
demo -e friend QWERTY; shot 29b_draugo_dovana 4
tap $((W/2)) $((H/2)); sleep 1
demo -e screen quests; shot 30_uzduotys 3
adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/8)) 400; shot 30b_trofejai 2
adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/8)) 400; shot 30c_pasiekimai 2
demo -e screen forecast; shot 31_prognoze 6
demo -e screen settings; shot 32_nustatymai 3
adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/5)) 400; shot 33_nustatymai_toliau 2
demo -e screen lake; sleep 2

adb shell am force-stop "$PKG"
adb shell cmd locale set-app-locales "$PKG" --locales de; sleep 2
timeout 60 adb shell am start -W -n "$ACT" --ez noConsent true; shot 34_deutsch 10
adb shell am force-stop "$PKG"
adb shell cmd locale set-app-locales "$PKG" --locales en; sleep 2
timeout 60 adb shell am start -W -n "$ACT" --ez noConsent true; shot 35_english 10

if ! timeout 20 adb get-state > /dev/null 2>&1; then echo "The emulator died during the test"; exit 1; fi
timeout 60 adb logcat -d > "$OUT/logcat_full.txt"
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
