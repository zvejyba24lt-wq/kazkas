# Kimba! / Wetterbiss

Žvejybos žaidimas Android telefonams, kuriame žuvys kimba pagal **tikrą orą, Mėnulį ir laiką** žaidėjo vietovėje.
Prototipas: vienas ežeras, 13 rūšių, 6 auksinės žuvys, užmetimas, kirtimas ir traukimas, žuvų knyga, prognozė, LT/DE/EN.

- `core/`: gryna Kotlin logika (astronomija, MET Norway prognozės skaitymas, žuvų aktyvumas, kibimas, traukimas). Testai: `./gradlew -PcoreOnly :core:test`.
- `app/`: Jetpack Compose programa. Ežero scena piešiama kodu, vanduo yra AGSL šešėliuoklis (Android 13+, senesniems supaprastintas).
- Orai: [MET Norway Locationforecast](https://api.met.no/) (CC BY 4.0), talpinama 45 min.
- APK kuria GitHub Actions: kiekvienas push į `claude/**` atnaujina pre-release `apk-latest` (Kimba.apk ir screenshots.zip).

Demonstracinės komandos (ekrano nuotraukoms):
`adb shell am start -n lt.marcomultiservices.kimba/.MainActivity -e scene storm|dawnfog|night|snow|rain|dusk|day|live -e catch moon_pike -e fight pike -e screen book|forecast|settings|lake`
