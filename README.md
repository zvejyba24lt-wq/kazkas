# Skaitau Pats

Android programėlė, mokanti 5–7 metų vaikus skaityti lietuviškai: raidės → skiemenys → žodžiai → sakiniai → pasakaitės.

- `app/src/main/assets/kursas.json` – visas turinys (raidės, žodžiai su skiemenimis, sakiniai, pasakaitės).
- `core/` – pamokų sudarymas ir patikros (vaikas niekada nemato dar neišmoktos raidės): `gradle -PcoreOnly :core:test`.
- Garsai: GitHub Actions `voice` darbas įrašo kiekvieną naują tekstą Onos balsu (edge-tts, `lt-LT-OnaNeural`) į `app/src/main/assets/audio/`.
- APK: kiekvienas push surenka `SkaitauPats.apk` ir įkelia į pre-release `apk-latest`.

Testinės versijos (pasirašytos `sideload.keystore`) atrakina visas pamokas. Google Play versijai reikia `RELEASE_KEYSTORE_*` paslapčių; tada atrakinta tik 8 pamokos, o likusios – nupirkus `visas_kursas`.
