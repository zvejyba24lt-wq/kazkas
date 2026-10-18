# Skaitau Pats

Android programėlė, mokanti 5–7 metų vaikus skaityti lietuviškai: raidės → skiemenys → žodžiai → sakiniai → pasakaitės.

- `app/src/main/assets/kursas.json` – visas turinys (raidės, žodžiai su skiemenimis, sakiniai, pasakaitės).
- `core/` – pamokų sudarymas ir patikros (vaikas niekada nemato dar neišmoktos raidės): `gradle -PcoreOnly :core:test`.
- Garsai: GitHub Actions `voice` darbas įrašo kiekvieną naują tekstą Onos balsu (edge-tts, `lt-LT-OnaNeural`) į `app/src/main/assets/audio/`.
- APK: kiekvienas push surenka `SkaitauPats.apk` ir įkelia į pre-release `apk-latest`.

Modelis: nemokamai visas kursas, bet ne daugiau kaip 2 naujos pamokos per dieną ir vaikams skirta AdMob reklama po baigtos pamokos. Vienkartinis pirkimas `visas_kursas` (4,99 €) panaikina reklamas ir ribą. AdMob id nustatomi paslaptimis `ADMOB_APP_ID` ir `ADMOB_INTERSTITIAL_ID`; be jų rodomos Google bandomosios reklamos.

Testinės versijos (pasirašytos `sideload.keystore`) leidžia atidaryti pamokas bet kuria tvarka, o tėvų skiltyje turi jungiklį „apsimesti, kad nupirkta“. Google Play versijai reikia `RELEASE_KEYSTORE_*` paslapčių.
