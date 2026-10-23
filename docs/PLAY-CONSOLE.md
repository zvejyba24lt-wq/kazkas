# Kimba ir Google Play Console

Viskas, kas susiję su Play, jau įdiegta žaidime. Kol `app/src/main/res/values/games-ids.xml` faile ID tušti, žaidimas Play Games visai neliečia ir veikia kaip anksčiau.

## Kas jau padaryta kode

- Play Games lyderių lentelės ir pasiekimai (`play/PlayGames.kt`). Mygtukai atsiranda Užduočių ekrane, kai įrašytas projekto ID.
- Rezultatai siunčiami po kiekvienos žuvies, o pasiekimai po kiekvieno atrakinimo. Kas pasiekta prieš prisijungimą, išsiunčiama vėliau.
- Play įvertinimo kortelė (`play/PlayReview.kt`): vieną kartą, po rekordo arba naujos rūšies, kai žaidėjas jau sugavęs bent 15 žuvų.
- Pirkimas „be reklamų“ (Billing) ir reklama (AdMob) buvo ir anksčiau, jų ID ateina per CI paslaptis.

## Ką reikės padaryti Play Console (kai žaidimas bus baigtas)

1. Play Console → Play Games Services → Setup: sukurti žaidimo projektą ir susieti su Kimba programėle (SHA-1 iš įkėlimo rakto ir iš Play App Signing).
2. Sukurti 4 lyderių lenteles ir 27 pasiekimus, aprašytus žemiau.
3. Play Console mygtukas „Get resources“ duoda XML. Iš jo ID nukopijuojami į `games-ids.xml` tais pačiais pavadinimais. Tai padarysiu aš, kai atsiųsi tą XML.
4. Publish Play Games pakeitimus (pirmiausia testuotojams).

## Lyderių lentelės

| Failo pavadinimas | Pavadinimas (LT / DE / EN) | Formatas | Rikiavimas |
| --- | --- | --- | --- |
| `leaderboard_biggest_fish` | Didžiausia žuvis / Größter Fisch / Biggest fish | Skaičius, gramai (rodyti kaip „g“) | Didesnis geriau |
| `leaderboard_daily_challenge` | Dienos iššūkis / Tages-Challenge / Daily challenge | Skaičius, gramai | Didesnis geriau (žiūrėti „Šiandien“) |
| `leaderboard_fish_caught` | Sugauta žuvų / Gefangene Fische / Fish caught | Skaičius | Didesnis geriau |
| `leaderboard_experience` | Patirtis / Erfahrung / Experience | Skaičius | Didesnis geriau |

## Pasiekimai

Play leidžia iš viso 1000 taškų; žemiau jie paskirstyti pagal žaidimo atlygį. Kiekvienam reikia 512×512 px ikonos.

| Failo pavadinimas | LT | DE | EN | Aprašymas (EN) | Taškai |
| --- | --- | --- | --- | --- | --- |
| `achievement_first_fish` | Pirmoji žuvis | Der erste Fisch | First fish | Catch your first fish | 5 |
| `achievement_fish_25` | Žvejys | Angler | Angler | Catch 25 fish | 5 |
| `achievement_fish_100` | Patyręs žvejys | Erfahrener Angler | Seasoned angler | Catch 100 fish | 20 |
| `achievement_fish_500` | Ežerų legenda | Legende der Seen | Legend of the lakes | Catch 500 fish | 90 |
| `achievement_species_5` | Smalsuolis | Neugierig | Curious | Catch 5 different species | 10 |
| `achievement_species_12` | Gamtininkas | Naturkundler | Naturalist | Catch 12 different species | 30 |
| `achievement_species_all` | Visos rūšys | Alle Arten | Every species | Catch every species | 175 |
| `achievement_kilo` | Kilogramas | Ein Kilo | One kilo | Catch a fish of 1 kg or more | 5 |
| `achievement_five_kilo` | Stambi žuvis | Großfisch | Big one | Catch a fish of 5 kg or more | 25 |
| `achievement_ten_kilo` | Pabaisa | Monster | Monster | Catch a fish of 10 kg or more | 70 |
| `achievement_golden` | Aukso blizgesys | Goldglanz | Golden glint | Catch a golden fish | 35 |
| `achievement_legend` | Legendų medžiotojas | Legendenjäger | Legend hunter | Catch a legendary fish | 70 |
| `achievement_all_waters` | Visi vandenys | Alle Gewässer | Every water | Catch fish in a lake, pond, river and the sea | 30 |
| `achievement_rain` | Lietaus žvejys | Regenangler | Rain angler | Catch a fish while it really rains where you are | 5 |
| `achievement_storm` | Perkūno draugas | Gewitterfreund | Storm friend | Catch a fish during a real thunderstorm | 20 |
| `achievement_fog` | Rūko žvejys | Nebelangler | Fog angler | Catch a fish in real fog | 15 |
| `achievement_snow` | Žiemos žvejys | Winterangler | Winter angler | Catch a fish while it really snows | 15 |
| `achievement_ice_fish` | Ledo žvejys | Eisangler | Ice angler | Catch a fish through the ice | 20 |
| `achievement_stamps_10` | Visų orų žvejys | Allwetter-Angler | All-weather angler | Collect 10 weather stamps in the fish book | 25 |
| `achievement_stamps_40` | Orų metraštininkas | Wetterchronist | Weather chronicler | Collect 40 weather stamps | 100 |
| `achievement_traveler` | Keliautojas | Reisender | Traveller | Visit 3 places on the map | 20 |
| `achievement_streak_7` | Ištikimas | Treu | Faithful | Fish 7 days in a row | 20 |
| `achievement_release_10` | Gero žvejo ženklas | Fairer Angler | Good angler | Earn 10 good-angler points by releasing fish | 20 |
| `achievement_release_50` | Gamtos sargas | Naturschützer | Guardian of the water | Earn 50 good-angler points | 70 |
| `achievement_boots` | Batų kolekcininkas | Stiefelsammler | Boot collector | Pull out 3 old boots | 10 |
| `achievement_treasure` | Lobių ieškotojas | Schatzsucher | Treasure hunter | Find a message in a bottle | 35 |
| `achievement_level_10` | Meistras | Meister | Master | Reach level 10 | 55 |
