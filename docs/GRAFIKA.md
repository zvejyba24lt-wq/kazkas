# Kimba grafika: kaip ji sudaryta ir kaip ją pakeisti

Šis failas skirtas žmogui arba kitam DI, kuris keis žaidimo grafiką. Visa scena piešiama kodu
(Jetpack Compose `Canvas` + AGSL vandens šešėliuoklis), todėl žaidimas veikia ir be jokių
paveikslėlių. Beveik kiekvieną dalį galima pakeisti paveikslėliu: tereikia įdėti failą
tinkamu vardu, kodo keisti nereikia.

## 1. Greičiausias kelias: pakeisti paveikslėliu (be kodo)

Įdėkite failą į `app/src/main/assets/art/` vardu `<vieta>.webp` (arba `.png`, `.jpg`).
Fonas turi būti permatomas. Jei failo nėra, ta dalis piešiama kodu kaip anksčiau.
Paveikslėliai automatiškai tamsinami naktį ir šiltinami saulėlydžio metu (tas pats
`SceneColors.lit()` tonavimas, kaip ir kodu pieštoms dalims), todėl pieškite juos
**dienos šviesoje**, be nakties ar saulėlydžio atspalvių.

Koordinatės žemiau pateiktos ekrano dalimis: `w` – ekrano plotis, `h` – aukštis,
`hy = 0,44·h` – horizonto linija (kur vanduo susitinka su krantu). Etaloninis ekranas
1080×2400 px, todėl `w` = 1080 px, `h` = 2400 px, `hy` = 1056 px.

| Vieta (failo vardas) | Kas tai | Kur ir kokio dydžio | Pastabos |
|---|---|---|---|
| `shore_lake` | Ežero krantai: kalvos, miškas | Visas plotis, nuo `hy − 0,24h` iki `hy + 0,006h` (1080×590 px) | Apatinis kraštas = vandens linija. Iš jo pačio piešiamas ir atspindys vandenyje. |
| `shore_pond` | Tvenkinio krantai su gluosniais | Kaip `shore_lake` | Kai šis failas yra, kodu pieštų gluosnių nebėra. |
| `shore_river` | Upės krantas | Kaip `shore_lake` | |
| `shore_sea` | Jūros horizontas, kopos, švyturys | Kaip `shore_lake` | Laivas ir švyturio šviesa tada nepiešiami. Jūroje atspindžio nėra. |
| `pier` | Medinis lieptas (ežeras, tvenkinys, upė) | x nuo −0,06w iki 0,44w, y nuo 0,68h iki h (540×768 px) | Lieptas eina iš apatinio kairio kampo į vandenį; jo tolimasis kraštas yra ties 0,80h. |
| `pier_sea` | Ilgas jūros tiltas (kaip Palangoje) | x nuo −0,12w iki 0,5w, y nuo 0,46h iki h (670×1300 px) | Tolimasis tilto galas ties 0,66h, tarp 0,06w ir 0,2w. Turėklai ir žibintai turi būti paveikslėlyje. |
| `bait_tin` | Kirmėlių dėžutė ant liepto | 0,08w × 0,054h, centras (0,14w; 0,9h) | |
| `float_classic`, `float_feather`, `float_glow`, `float_gold` | Plūdės | Santykis 1:2,6 (stačias) | Vandens linija 65 % aukščio nuo viršaus. Plūdė su antenomis. |
| `lily`, `lily_flower` | Vandens lelijų lapas, lapas su žiedu | Santykis apie 3:1 (žiūrint iš šono) | Apatinis kraštas guli ant vandens. |
| `stone` | Akmuo upėje | Santykis 2:0,9 | Puta aplink piešiama kodu. |
| `buoy` | Jūros plūduras | Santykis 1:2, vandens linija apačioje | Mirksinti šviesa naktį piešiama kodu. |
| `cloud_1`, `cloud_2`, `cloud_3` | Debesys | Bet koks santykis, plotis ≈ 0,4w | Jų skaičius priklauso nuo tikro debesuotumo; jie dreifuoja su vėju. |
| `feeder_cage` | Fiderio šėryklėlė su jaukais | Santykis 1:1,6 (stačia) | Valas pririštas viršuje per vidurį. |
| `spoon` | Blizgė su trišakiu kabliuku | Santykis 1:2,4 (stačia) | Žiedelis viršuje. Žaidimas pats ją „mirksina“ sukdamas. |
| `rod_rest` | Meškerės stovas (šakutė) | Santykis 1:1 | Meškerė guli viršuje per vidurį. Stiebas piešiamas kodu. |
| `bite_alarm` | Elektroninis signalizatorius | Santykis 1:1 | Meškerė guli viršuje. Raudona lemputė mirksi kodu, kairėje apačioje. |
| `reel` | Ritė po meškere | Santykis 1:1, šonu | Kojelė viršuje per vidurį, špūlė į dešinę. Žaidimas pasuka ją pagal meškerę. |
| `reeds` | Nendrių kuokštas apatiniame dešiniajame kampe | x nuo 0,62w iki 1,04w, y nuo 0,66h iki h (apie 1:1,85) | Pakeičia abi kodu pieštas nendrių eiles, siūbuoja pagal vėją. |
| `ice` | Užšalęs ežeras: ledas su sniegu | Visas plotis, nuo horizonto `hy` (0,44h) iki apačios (1080×1210 px, nepermatomas) | Rodomas, kai ledas ≥ 10 cm. Viršutinis kraštas = tolimasis krantas. Senas eketes, plyšius ir taką gali įpiešti pats. |
| `ice_hole` | Žvejo eketė su išgręžto ledo trupiniais | Santykis apie 3:1,3, centras ties (0,5w; 0,667h) | Žiūrint įstrižai iš viršaus. Vidurys tamsus vanduo; plūdė ir ratilai piešiami kodu ant viršaus. |
| `ice_angler` | Tolimas žvejys ant dėžės (iš nugaros) | Stačias, apatinis kraštas ant ledo | Piešiami 3 skirtingais atstumais. |
| `bank` | Nendrių krantas: žolėtas šlaitas su akmenimis | Visas plotis, apačioje (apie 1080×560 px), viršus permatomas | Vieta „Nendrynas“. Vanduo matosi pro permatomą viršų. |
| `boat` | Medinės valties priekis (žiūrint nuo galinio suolo) | Plotis 1,1w, apačioje (apie 1190×700 px), aplink permatoma | Vieta „Valtis“. Laivagalis į viršų ties 0,42w, 0,72h; irklai šonuose. |
| `camp_bg` | Tuščia stovyklos laukymė prie ežero | Platus 16:10 (1600×1000 px) | Fonas stovyklos ekrane. |
| `camp_fire`, `camp_chair`, `camp_tent`, `camp_lantern`, `camp_dog`, `camp_smoker`, `camp_flag`, `camp_cabin` | Stovyklos daiktai | Kiekvienas atskirai, permatomas fonas, apačia = žemė | Laužavietė, žvejo kėdė, palapinė, žibintas ant stulpelio, šuo, rūkykla, vėliava, žvejo namelis. |
| `pond_bg` | Tvenkinio vidus per stiklą iš šono | Platus (1600×700 px), nepermatomas | Stovyklos tvenkinys, kuriame plaukioja geriausios žuvys. Vidurys tuščias. |
| `lantern` | Senas žibalinis žibintas, stovintis ant žemės | Stačias (apie 300×450 px), permatomas fonas | Naktį dega šalia žvejo. Liepsną ir šviesą prideda kodas. |
| `angler` | Žvejys iki juosmens, laikantis žuvį | Stačias 4:5 (apie 800×1000 px), permatomas fonas | Trofėjaus nuotrauka laimikio kortelėje ir pasigyrimo paveikslėlyje. Delnai atkišti į priekį ties 22 % ir 78 % pločio, 62 % aukščio. Žuvį į rankas įdeda kodas, todėl paveikslėlyje žuvies nėra. |
| `surprise_boot`, `surprise_crayfish`, `surprise_rod`, `surprise_bottle` | Netikėti laimikiai | Kvadratas (apie 512×512 px), permatomas fonas | Senas batas, vėžys, nuskendusi meškerė su masalu, butelis su laišku. Kabo ant valo: viršutinis vidurys yra kabinimo taškas. |
| `leaf_1`, `leaf_2`, … | Rudens lapai | 256×256 px kiekvienas, permatomas fonas | Kiek nori skirtingų (klevo, beržo, ąžuolo, drebulės; raudoni, geltoni, oranžiniai, rudi). Rodomi rugsėjo–lapkričio mėn. |
| `<bet kuris>_spring`, `_autumn`, `_winter` | Metų laikų versijos | Kaip originalas | Bet kuris paveikslėlis gali turėti metų laiko versiją, pvz. `shore_lake_autumn`, `pier_winter`, `reeds_autumn`, `camp_bg_winter`. Pavasaris kovo–gegužės, ruduo rugsėjo–lapkričio, žiema gruodžio–vasario mėn. |
| `jonas_1`, `jonas_2`, `jonas_3` | Senasis žvejys Jonas | Kvadratas (600×600 px), permatomas fonas | 1 draugiškas, 2 pasakoja paslaptį, 3 didžiuojasi. Ta pati poza, keičiasi tik veidas. |
| `angler_winter` | Žvejys žiemos drabužiais | Kaip `angler` | Trofėjaus nuotraukai gruodžio–vasario mėn. Ta pati poza ir delnų vieta. |
| `widget_bg` | Pagrindinio ekrano valdiklio fonas | 4:2 (apie 800×400 px), nepermatomas | Ramus ežeras sutemus, be teksto. Apačia tamsėja savaime, ant jos rašomas kibimas. Be jo – tamsiai mėlynas gradientas. |
| `item_<rūšis>_<vardas>` | Parduotuvės ikonos | Kvadratas | Pvz. `item_lure_worm`, `item_rod_carbon`, `item_float_glow`, `item_up_boat`. Vardai yra `core/.../Gear.kt` (`Lure`, `Rod`, `Reel`, `Line`, `FloatStyle`, `Upgrade`). |

Žuvų nuotraukos dedamos atskirai: `app/src/main/assets/fish/<rūšies id>.webp`, galva į kairę,
permatomas fonas (žr. `assets/fish/README.txt`). Jos rodomos laimikio kortelėje ir žuvų knygoje.
Auksinės žuvys gauna aukso atspalvį automatiškai. Rūšių id yra `core/.../Species.kt`
(pvz. `roach`, `perch`, `pike`, `cod`, `sea_trout`).

Rekomenduojamas formatas: WebP su permatomumu, ne didesnis nei 2× etaloninio dydžio.

## 2. Spalvos be kodo logikos keitimo

Visos fiksuotos scenos spalvos yra vienoje vietoje: `app/src/main/java/lt/marcomultiservices/kimba/ui/scene/Palette.kt`.
Pakeitus spalvą ten, ji pasikeičia visur. Dangaus ir vandens spalvos, kurios keičiasi kartu
su saule, yra `SceneColors.kt` (`stops` – dangaus spalvos pagal saulės aukštį).

## 3. Kodo sandara

```
ui/scene/
  LakeScene.kt     dirigentas: apskaičiuoja plūdės, meškerės galo, raibulių padėtį ir piešia sluoksnius eilės tvarka
  Geometry.kt      viskas, kas priklauso tik nuo ekrano dydžio: miško siluetai, nendrės, lelijos, liepto forma (PierShape), lašai
  SceneColors.kt   šviesa pagal tikrą saulės aukštį, debesis, rūką; lit() ir far() tonavimas
  Palette.kt       visos fiksuotos spalvos pagal pavadinimą
  ArtPack.kt       paveikslėlių pakeitimai iš assets/art ir drawArt()
  WaterShader.kt   AGSL vandens paviršius (bangos, atspindžiai, saulės takas, srovė, jūros bangavimas)
  SceneTime.kt     animacijos laikrodis
  layers/
    Sky.kt         dangus, žvaigždės, saulė, mėnulis su tikra faze, debesys, žaibas
    Shore.kt       kalvos, miškai, upės krantas, gluosniai, jūros horizontas su švyturiu ir laivu, atspindžiai, rūkas
    Water.kt       lelijos, upės akmenys ir lapai, kirai, plūduras, atsarginis vanduo senesniems telefonams
    Tackle.kt      meškerė, valas, plūdė ore ir vandenyje, taikinys užmetant
    FishFx.kt      žuvies šešėlis po plūde, kova su purslais, žuvies šuoliai
    Pier.kt        lieptas ir jūros tiltas (lentos, siūlės, vinys, poliai, turėklai, žibintai), kirmėlių dėžutė, nendrės
    Weather.kt     lietus ir rateliai vandenyje, sniegas, jonvabaliai, laumžirgiai, žaibo blyksnis, vinjetė
ui/fish/
  FishArt.kt       kodu pieštos žuvys (28 rūšys + auksinės)
  FishPicture.kt   žuvies nuotrauka, jei yra, kitaip FishArt
```

### Sluoksnių tvarka (iš galo į priekį), `LakeScene.kt`

1. `drawSky` – dangus ir debesys
2. `drawShores` – krantai pagal vandens tipą (ežeras / tvenkinys / upė / jūra)
3. `drawWillows` – tik tvenkinyje
4. vanduo – `WaterShader` (Android 13+) arba `drawFallbackWater`
5. `drawReflections` – kranto atspindys ir šviesi vandens linija
6. `drawMist` – rūkas ir ryto migla
7. vandens objektai – lelijos (ežeras, tvenkinys), akmenys ir lapai (upė), plūduras ir kirai (jūra)
8. `drawAim` – taikinys, kol laikomas užmetimo mygtukas
9. `drawFishShadow`, `drawFloatOnWater` – laukiant kibimo
10. `drawFightSplash`, `drawFishJump` – kovos metu
11. `drawPier` – lieptas arba jūros tiltas
12. galinės nendrės (ne jūroje)
13. `drawRod`, `drawLine`, plūdė ore
14. priekinės nendrės (ne jūroje)
15. `drawWeather`, `drawVignette`

Kiekvienas sluoksnis yra `DrawScope` plėtinio funkcija, gaunanti `SceneGeometry` (vietos),
`SceneColors` (šviesa) ir laiką `t` sekundėmis (animacijai). Norint perpiešti dalį kodu,
užtenka perrašyti vieną funkciją jos faile; kitų failų liesti nereikia.

### Taisyklės, kurių reikia laikytis

- **Jokių kvadratų.** Savininkas nenori matyti stačiakampių formų gamtoje. Lapija, debesys,
  akmenys piešiami kreivėmis ir gradientais.
- **Path kryptis.** Compose `Path.addOval` / `addRect` eina prieš laikrodžio rodyklę. Jei jie
  maišomi viename `Path` su ranka pieštais kontūrais pagal laikrodžio rodyklę, persidengimose
  atsiranda skylės. Kontūrus piešti ranka, visus ta pačia kryptimi (žr. `Geometry.roundTree`).
- Spalvas imti iš `Palette` ir leisti per `colors.lit()` (artimi daiktai) arba `colors.far()`
  (tolimi, blunka į horizonto miglą). Taip naktis, saulėlydis ir audra veikia automatiškai.
- Matmenis rašyti ekrano dalimis (`geo.w * 0.1f`), ne pikseliais, ir perspektyvai naudoti
  `geo.scaleAt(y)` (toli mažiau, arti daugiau).
- Krantai (išskyrus jūrą) ir lieptas yra nejudantys: `LakeScene` juos nupiešia vieną kartą į
  paveikslėlį (`LayerCache`) ir perpiešia tik pasikeitus šviesai (kas minutę). Todėl ten
  nedėkite animacijos; judančius dalykus dėkite į kitus sluoksnius.
- Kiekvienas kitas kadras piešiamas iš naujo (60 FPS). Sunkius skaičiavimus dėti į `SceneGeometry`
  (skaičiuojama vieną kartą), ne į piešimo funkcijas.

## 4. Kaip patikrinti

Android SDK reikia tik CI. Kiekvienas `git push` į šaką paleidžia `.github/workflows/android.yml`:
testai, APK, emuliatoriuje padaromos 35 ekrano nuotraukos (`.github/scripts/smoke.sh`).
Rezultatas: GitHub leidimas `kimba-apk-latest` su `Kimba.apk` ir `screenshots.zip`.

Nuotraukose matomos visos scenos: tikras oras, perkūnija, rūkas, naktis, sniegas, lietus,
sutemos, tvenkinys, upė, jūra (diena ir sutemos), Riugenas, Bodenas, laimikio kortelės,
žuvų knyga, parduotuvė, žemėlapis, užduotys, prognozė, nustatymai ir vokiška bei angliška versija.

Derinimo komandos (veikia tik derinimo versijoje), pvz.:

```
adb shell am start -n lt.marcomultiservices.kimba/.MainActivity -e scene night -e water sea
adb shell am start -n lt.marcomultiservices.kimba/.MainActivity -e catch cod
```

`scene`: `live`, `day`, `dawnfog`, `dusk`, `night`, `rain`, `storm`, `snow`;
`water`: `lake`, `pond`, `river`, `sea`; `trip`: kelionės id (`palanga`, `ruegen`, `bodensee`…);
`catch` / `fight`: rūšies arba auksinės žuvies id; `screen`: `book`, `shop`, `map`, `quests`, `forecast`, `settings`, `lake`.
