package lt.marcomultiservices.kimba.ui

import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.L
import lt.marcomultiservices.kimba.core.Plural
import lt.marcomultiservices.kimba.core.Sky

/** All player-facing text of the app. */
object S {
    val appName = L("Kimba!", "Wetterbiss", "Kimba!")
    val tagline = L("Žvejyba pagal tikrą orą", "Angeln mit echtem Wetter", "Fishing with real weather")

    // Welcome
    val welcomeTitle = L("Žuvys kimba pagal tikrą orą", "Die Fische beißen nach echtem Wetter", "Fish bite by the real weather")
    val welcomeBody = L(
        "Lyja pas tave – lyja ir ant ežero. Sterkai mėgsta lietų, lydekos – krentantį slėgį, o auksinės žuvys pasirodo tik per tikrą pilnatį, perkūniją ar šalną.",
        "Regnet es bei dir, regnet es auch am See. Zander lieben Regen, Hechte fallenden Luftdruck, und goldene Fische zeigen sich nur bei echtem Vollmond, Gewitter oder Frost.",
        "If it rains where you are, it rains on the lake. Zander love rain, pike love falling pressure, and golden fish only show up at a real full moon, in real thunder or frost.",
    )
    val useMyPlace = L("Naudoti mano vietovę", "Meinen Ort verwenden", "Use my location")
    val chooseCity = L("Pasirinkti miestą", "Stadt wählen", "Choose a city")
    val placeNote = L(
        "Vietovė naudojama tik orui sužinoti, miesto tikslumu. Ji niekur nesiunčiama, išskyrus orų tarnybą.",
        "Der Ort dient nur dem Wetter, auf Stadtebene. Er geht nirgendwohin außer an den Wetterdienst.",
        "Your location is only used for the weather, at city level. It goes nowhere except the weather service.",
    )
    val myPlace = L("Tavo vieta", "Dein Ort", "Your place")
    val locating = L("Ieškoma vietovės…", "Ort wird gesucht…", "Finding your location…")
    val noLocation = L("Vietovės rasti nepavyko – pasirink miestą.", "Ort nicht gefunden – bitte Stadt wählen.", "Couldn't find your location – pick a city.")

    // Lake HUD
    val biteIndex = L("Kibimas", "Beißindex", "Bite index")
    val casts = L("Užmetimai", "Würfe", "Casts")
    val nextCastIn = L("+1 po %s", "+1 in %s", "+1 in %s")
    val holdToCast = L("Laikyk ir paleisk – užmesk", "Halten und loslassen – auswerfen", "Hold and release to cast")
    val cast = L("Užmesti", "Auswerfen", "Cast")
    val waitForBite = L("Stebėk plūdę…", "Beobachte die Pose…", "Watch the float…")
    val strike = L("KIRSK!", "ANHIEB!", "STRIKE!")
    val tooEarly = L("Per anksti! Palauk, kol plūdė panirs.", "Zu früh! Warte, bis die Pose abtaucht.", "Too early! Wait until the float goes under.")
    val reelHint = L("Laikyk – vyniok. Paleisk, kai žuvis tempia!", "Halten – kurbeln. Loslassen, wenn der Fisch zieht!", "Hold to reel. Let go when the fish runs!")
    val fishRuns = L("Tempia!", "Er zieht!", "It runs!")
    val steerLeft = L("◀ Kreipk meškerę kairėn", "◀ Rute nach links", "◀ Rod to the left")
    val steerRight = L("Kreipk meškerę dešinėn ▶", "Rute nach rechts ▶", "Rod to the right ▶")
    val netLead = L("Graibštas! Vesk žuvį į vidurį", "Kescher! Führ den Fisch zur Mitte", "Net! Lead the fish to the middle")
    val netCalm = L("Palauk, kol nurims…", "Warte, bis er ruhig wird…", "Wait until it calms…")
    val steerGood = L("Gerai! Žuvis pavargsta", "Gut! Der Fisch ermüdet", "Good! The fish is tiring")
    val noCasts = L("Užmetimai baigėsi", "Keine Würfe mehr", "Out of casts")
    val noCastsBody = L("Naujas užmetimas atsiras po %s.", "Ein neuer Wurf kommt in %s.", "A new cast arrives in %s.")
    val bonusCasts = L("+5 užmetimai", "+5 Würfe", "+5 casts")
    val bonusNote = L("Už trumpą reklamos vaizdo įrašą", "Für ein kurzes Werbevideo", "For a short video ad")
    val lure = L("Masalas", "Köder", "Bait")

    // First-play tips
    val tipStep = L("Patarimas %d iš 4", "Tipp %d von 4", "Tip %d of 4")
    val tipCastTitle = L("Užmesk meškerę", "Wirf die Angel aus", "Cast your line")
    val tipCast = L(
        "Laikyk mygtuką: meškerė atsilenkia, o žiedas pildosi. Paleisk, ir plūdė nuskris. Kuo pilnesnis žiedas, tuo toliau.",
        "Halte den Knopf: Die Rute holt aus und der Ring füllt sich. Lass los, und die Pose fliegt. Je voller der Ring, desto weiter.",
        "Hold the button: the rod swings back and the ring fills. Let go and the float flies. The fuller the ring, the farther.",
    )
    val tipWaitTitle = L("Kantrybės", "Geduld", "Patience")
    val tipWait = L(
        "Kai plūdė tik virpa ir trūkčioja, žuvis dar tik uostinėja masalą. Nekirsk! Lauk, kol plūdė staiga panirs.",
        "Zittert und zuckt die Pose nur, knabbert der Fisch erst am Köder. Nicht anschlagen! Warte, bis sie plötzlich abtaucht.",
        "When the float only trembles and twitches, the fish is just nibbling. Don't strike! Wait until it suddenly goes under.",
    )
    val tipStrikeTitle = L("Dabar!", "Jetzt!", "Now!")
    val tipStrike = L("Plūdė paniro. Bakstelk bet kur ekrane ir kirsk!", "Die Pose ist weg. Tippe irgendwo auf den Bildschirm!", "The float went under. Tap anywhere on the screen!")
    val tipReelTitle = L("Ištrauk žuvį", "Hol den Fisch raus", "Land the fish")
    val tipReel = L(
        "Laikyk pirštą ir vyniok. Slink pirštu į šoną taip, kad geltonas taškas būtų žaliame lauke: meškerė traukia prieš žuvį, ir ji pavargsta. Kai „Tempia!“, atleisk.",
        "Finger halten zum Kurbeln. Schieb ihn zur Seite, bis der gelbe Punkt im grünen Feld ist: die Rute zieht gegen den Fisch und er ermüdet. Bei „Er zieht!“ loslassen.",
        "Hold to reel. Slide sideways until the yellow dot sits in the green zone: the rod pulls against the fish and tires it. When “It runs!”, let go.",
    )

    val tipWaitFeeder = L(
        "Masalas guli ant dugno. Stebėk ryškią meškerės galūnę: kai ji tik linkteli, žuvis dar uostinėja. Kai galūnė smarkiai nusilenkia, kirsk!",
        "Der Köder liegt am Grund. Beobachte die leuchtende Rutenspitze: Nickt sie nur, knabbert der Fisch erst. Biegt sie sich kräftig, schlag an!",
        "The bait lies on the bottom. Watch the bright rod tip: a small nod is only a nibble. When it bends right over, strike!",
    )
    val tipWaitSpin = L(
        "Laikyk pirštą, ir blizgė vyniojasi. Retkarčiais atleisk: blizgė plazdėdama krenta, ir plėšrūnas puola. Stuktelėjimai reiškia, kad žuvis seka.",
        "Finger halten, dann läuft der Blinker. Lass ab und zu los: er taumelt ab, und der Räuber schlägt zu. Zupfer heißen, ein Fisch folgt.",
        "Hold your finger to wind the spoon in. Let go now and then: it flutters down and predators strike. Taps mean a fish is following.",
    )
    val waitFeeder = L("Stebėk meškerės galūnę…", "Beobachte die Rutenspitze…", "Watch the rod tip…")
    val waitSpin = L("Laikyk – vyniok, atleisk – pauzė", "Halten – kurbeln, loslassen – Pause", "Hold to wind, let go to pause")
    val tooEarlyTip = L("Per anksti! Palauk, kol galūnė smarkiai nusilenks.", "Zu früh! Warte, bis sich die Spitze kräftig biegt.", "Too early! Wait until the tip bends right over.")

    // Tackle box
    val tackleBox = L("Masalų dėžutė", "Köderbox", "Tackle box")
    val rodKind = L("Meškerė", "Rute", "Rod")
    val baitsTitle = L("Masalai", "Köder", "Baits")
    val onlySpin = L("Tik spiningui", "Nur Spinnrute", "Spinning rod only")
    val onlyFeeder = L("Tik dugninei", "Nur Grundrute", "Bottom rod only")
    val floatOrFeeder = L("Plūdinei ar dugninei", "Pose oder Grund", "Float or bottom rod")
    val inShop = L("Parduotuvėje", "Im Shop", "In the shop")
    val close = L("Uždaryti", "Schließen", "Close")
    val rodKinds = L("Meškerių tipai", "Rutenarten", "Rod types")

    // Goals
    val nextUnlock = L("%d lygyje: %s", "Ab Level %d: %s", "At level %d: %s")
    val achievements = L("Pasiekimai", "Erfolge", "Achievements")
    val achievementsDone = L("%d iš %d", "%d von %d", "%d of %d")
    val achieved = L("Pasiekimas!", "Erfolg!", "Achievement!")
    val trophies = L("Trofėjų siena", "Trophäenwand", "Trophy wall")
    val trophiesEmpty = L("Čia kabės tavo didžiausios žuvys.", "Hier hängen deine größten Fische.", "Your biggest fish will hang here.")

    // Results
    val caught = L("Pagavai!", "Gefangen!", "Caught!")
    val newSpecies = L("Nauja rūšis!", "Neue Art!", "New species!")
    val newRecord = L("Naujas rekordas!", "Neuer Rekord!", "New record!")
    val goldenCaught = L("AUKSINĖ ŽUVIS!", "GOLDENER FISCH!", "GOLDEN FISH!")
    val snapped = L("Valas nutrūko!", "Die Schnur ist gerissen!", "The line snapped!")
    val snappedHint = L("Kai žuvis tempia, paleisk ritę.", "Wenn der Fisch zieht, lass die Rolle los.", "When the fish runs, let go of the reel.")
    val escaped = L("Žuvis nusikabino", "Der Fisch ist ausgeschlitzt", "The fish got off")
    val escapedHint = L("Neleisk valui per ilgai kabėti laisvai.", "Lass die Schnur nicht zu lange locker.", "Don't leave the line slack for too long.")
    val empty = L("Blizgė grįžo tuščia", "Der Blinker kam leer zurück", "The spoon came back empty")
    val emptyHint = L("Vyniok su pauzėmis: plėšrūnas puola krentančią blizgę.", "Mit Pausen kurbeln: Räuber packen den absinkenden Blinker.", "Wind with pauses: predators take a spoon as it flutters down.")
    val missed = L("Nespėjai kirsti", "Anhieb verpasst", "Missed the strike")
    val missedHint = L("Kai plūdė panyra – kirsk iškart.", "Taucht die Pose ab – sofort anschlagen.", "When the float goes under, strike at once.")
    val again = L("Užmesti dar", "Nochmal", "Cast again")
    val length = L("Ilgis", "Länge", "Length")
    val weight = L("Svoris", "Gewicht", "Weight")

    // Book
    val book = L("Žuvų knyga", "Fangbuch", "Fish book")
    val bookProgress = L("Pagauta %d iš %d", "%d von %d gefangen", "%d of %d caught")
    val allFish = L("Visos", "Alle", "All")
    val goldenFish = L("Auksinės žuvys", "Goldene Fische", "Golden fish")
    val best = L("Rekordas", "Rekord", "Record")
    val count = L("Pagauta: %d", "Gefangen: %d", "Caught: %d")
    val unknown = L("Dar nepagauta", "Noch nicht gefangen", "Not caught yet")

    // Forecast
    val forecast = L("Prognozė", "Vorhersage", "Forecast")
    val now = L("Dabar", "Jetzt", "Now")
    val activeNow = L("Dabar aktyviausios", "Gerade am aktivsten", "Most active now")
    val goldenWindows = L("Auksinių žuvų langai", "Zeitfenster für goldene Fische", "Golden fish windows")
    val noWindows = L(
        "Per artimiausias 2 dienas auksinėms žuvims sąlygų nebus. Iki pilnaties liko %d d., iki jaunaties – %d d.",
        "In den nächsten 2 Tagen keine Bedingungen für goldene Fische. Nächster Vollmond in %d T., Neumond in %d T.",
        "No golden fish conditions in the next 2 days. Next full moon in %d d, new moon in %d d.",
    )
    val activeWindow = L("VYKSTA DABAR", "JETZT", "HAPPENING NOW")
    val water = L("Vanduo", "Wasser", "Water")
    val air = L("Oras", "Luft", "Air")
    val pressure = L("Slėgis", "Luftdruck", "Pressure")
    val wind = L("Vėjas", "Wind", "Wind")
    val moon = L("Mėnulis", "Mond", "Moon")
    val falling = L("krenta", "fällt", "falling")
    val risingP = L("kyla", "steigt", "rising")
    val steady = L("stabilus", "stabil", "steady")
    val metCredit = L("Orų duomenys: MET Norway (CC BY 4.0)", "Wetterdaten: MET Norway (CC BY 4.0)", "Weather data: MET Norway (CC BY 4.0)")
    val offline = L("Nėra ryšio – rodomas paskutinis žinomas oras.", "Offline – letztes bekanntes Wetter.", "Offline – showing the last known weather.")
    val tipFalling = L("Slėgis krenta – plėšrūnai aktyvėja.", "Der Luftdruck fällt – Raubfische werden aktiv.", "Pressure is falling – predators wake up.")
    val tipRain = L("Lietus drumsčia vandenį – sterkai drąsesni.", "Regen trübt das Wasser – Zander werden mutiger.", "Rain clouds the water – zander get bolder.")
    val tipDusk = L("Sutemos – geriausias laikas.", "Dämmerung – die beste Zeit.", "Twilight – the best time.")
    val tipCold = L("Vanduo šaltas – žuvys tingios, bet vėgėlės budi.", "Kaltes Wasser – Fische sind träge, aber die Quappe ist wach.", "Cold water – fish are sluggish, but burbot are awake.")
    val tipSun = L("Saulėta – smulkios žuvys prie paviršiaus.", "Sonnig – Kleinfische an der Oberfläche.", "Sunny – small fish near the surface.")
    val nearFar = L("Arti – nendrių žuvys, toli – gilumos plėšrūnai.", "Nah – Schilffische, weit – Räuber im Tiefen.", "Near – reed fish, far – deep-water predators.")

    // Settings
    val settings = L("Nustatymai", "Einstellungen", "Settings")
    val place = L("Vietovė", "Ort", "Location")
    val language = L("Kalba", "Sprache", "Language")
    val phoneLanguage = L("Kaip telefone", "Wie am Handy", "Phone language")
    val sound = L("Garsai", "Töne", "Sound")
    val notifications = L("Pranešimai apie auksinių žuvų langus", "Hinweise auf Zeitfenster für goldene Fische", "Alerts for golden fish windows")
    val noAds = L("Be reklamos", "Werbefrei", "No ads")
    val noAdsBody = L(
        "Vienkartinis pirkinys: jokių reklamų tarp žuvų. Papildomi užmetimai ir dvigubos monetos lieka – be vaizdo įrašų.",
        "Einmalkauf: keine Werbung zwischen den Fängen. Extra-Würfe und doppelte Münzen gibt es weiter – ohne Video.",
        "One-time purchase: no ads between catches. Extra casts and double coins stay – without videos.",
    )
    val buy = L("Pirkti", "Kaufen", "Buy")
    val bought = L("Nupirkta – ačiū!", "Gekauft – danke!", "Purchased – thank you!")
    val notAvailable = L("Pirkimas galimas tik įdiegus iš Google Play.", "Kauf nur in der Version aus Google Play möglich.", "Purchase only available when installed from Google Play.")
    val privacyOptions = L("Reklamos sutikimo nustatymai", "Werbe-Einwilligung ändern", "Ad consent settings")
    val privacyPolicy = L("Privatumo politika", "Datenschutzerklärung", "Privacy policy")
    val back = L("Atgal", "Zurück", "Back")

    // Progress
    val level = L("Lygis %d", "Level %d", "Level %d")
    val coins = L("Monetos", "Münzen", "Coins")
    val levelUp = L("Naujas lygis!", "Neues Level!", "Level up!")
    val levelUpBody = L("Pasiekei %d lygį. Parduotuvėje ir žemėlapyje atsirado naujų dalykų.", "Du hast Level %d erreicht. Im Shop und auf der Karte gibt es Neues.", "You reached level %d. New things are waiting in the shop and on the map.")
    val streakTitle = L("Jau %d-a diena iš eilės!", "Tag %d in Folge!", "Day %d in a row!")
    val firstDayTitle = L("Dienos dovana!", "Tagesgeschenk!", "Daily gift!")
    val streakBody = L("Grįžimo dovana: +%d monetų ir +%d užmetimai. Ateik ir rytoj – dovana augs iki 7 dienos.", "Willkommensgeschenk: +%d Münzen und +%d Würfe. Komm morgen wieder – es wächst bis Tag 7.", "Welcome back gift: +%d coins and +%d casts. Come back tomorrow – it grows until day 7.")
    val questsDone = L("Užduotis įvykdyta!", "Aufgabe erledigt!", "Quest complete!")
    val allQuestsDone = L("Visos dienos užduotys įvykdytos: +%d monetų ir +%d užmetimai!", "Alle Tagesaufgaben erledigt: +%d Münzen und +%d Würfe!", "All daily quests done: +%d coins and +%d casts!")
    val great = L("Puiku!", "Super!", "Great!")
    fun plusCoins(n: Int) = L("+$n ${Plural.lt(n, "moneta", "monetos", "monetų")}", "+$n ${if (n == 1) "Münze" else "Münzen"}", "+$n ${if (n == 1) "coin" else "coins"}")
    val plusXp = L("+%d patirties", "+%d EP", "+%d XP")
    val doubleCoins = L("×2 monetos už reklamą", "×2 Münzen für ein Video", "×2 coins for a video")
    val doubleCoinsNoAds = L("×2 monetos", "×2 Münzen", "×2 coins")
    val doubled = L("Monetos padvigubintos!", "Münzen verdoppelt!", "Coins doubled!")

    // Quests
    val quests = L("Užduotys", "Aufgaben", "Quests")
    val questsTitle = L("Dienos užduotys", "Tagesaufgaben", "Daily quests")
    val questsNote = L("Naujos užduotys kasdien vidurnaktį. Įvykdžius visas tris: +300 monetų ir +3 užmetimai.", "Jeden Tag um Mitternacht neue Aufgaben. Alle drei erledigt: +300 Münzen und +3 Würfe.", "New quests every day at midnight. Finish all three: +300 coins and +3 casts.")
    val streakNow = L("Serija: %d d.", "Serie: %d T.", "Streak: %d d")

    // Shop
    val shop = L("Parduotuvė", "Shop", "Shop")
    val shopNote = L("Viskas perkama tik monetomis, kurias užsidirbi žvejodamas. Jokių atsitiktinių dėžių.", "Alles nur mit Münzen, die du beim Angeln verdienst. Keine Zufallskisten.", "Everything is bought only with coins you earn fishing. No random boxes.")
    val lures = L("Masalai", "Köder", "Baits")
    val rods = L("Meškerių kotai (stiprumas)", "Rutenblanks (Stärke)", "Rod blanks (strength)")
    val reels = L("Ritės", "Rollen", "Reels")
    val lines = L("Valai", "Schnüre", "Lines")
    val floats = L("Plūdės", "Posen", "Floats")
    val upgrades = L("Patobulinimai", "Verbesserungen", "Upgrades")
    val owned = L("Turi", "Gekauft", "Owned")
    val equipped = L("Naudojama", "Ausgerüstet", "Equipped")
    val equip = L("Naudoti", "Ausrüsten", "Equip")
    val needLevel = L("Nuo %d lygio", "Ab Level %d", "From level %d")
    val strengthNote = L("Stiprumas +%d%%", "Stärke +%d%%", "Strength +%d%%")
    val speedNote = L("Vyniojimas +%d%%", "Einholen +%d%%", "Reeling +%d%%")
    val lineNote = L("Stipresnis +%d%%, atsargios žuvys kimba %d%% rečiau", "Stärker +%d%%, scheue Fische beißen %d%% seltener", "Stronger +%d%%, shy fish bite %d%% less")
    val lookNote = L("Tik išvaizda", "Nur die Optik", "Looks only")

    // Map
    val map = L("Žemėlapis", "Karte", "Map")
    val home = L("Namie", "Zuhause", "Home")
    val homeNote = L("Namuose žvejoji pagal savo vietovės orą. Kiti vandenys atsiveria kylant lygiui.", "Zu Hause angelst du mit dem Wetter deines Ortes. Weitere Gewässer öffnen sich mit dem Level.", "At home you fish with your own local weather. More waters open as you level up.")
    val trips = L("Kelionės", "Reisen", "Trips")
    val tripsNote = L("Kiekviena vieta – su savo tikru oru dabar. Sumoki vieną kartą, grįžti nemokamai.", "Jeder Ort hat sein eigenes echtes Wetter. Einmal zahlen, danach kostenlos.", "Each place has its own real weather right now. Pay once, return for free.")
    val go = L("Keliauti", "Hinfahren", "Go")
    val here = L("Tu čia", "Du bist hier", "You are here")
    val locked = L("Užrakinta", "Gesperrt", "Locked")
    val onlyHere = L("Tik čia: %s", "Nur hier: %s", "Only here: %s")
    fun fishHere(n: Int) = L("${Plural.lt(n, "Gyvena %d žuvų rūšis", "Gyvena %d žuvų rūšys", "Gyvena %d žuvų rūšių").format(n)}", "$n Fischarten", "$n fish species")

    // Photos
    val photoCredit = L("Nuotrauka", "Foto", "Photo")

    // Notifications
    val notifyTitle = L("%s kimba dabar!", "%s beißt jetzt!", "%s is biting now!")
    val notifyBody = L("%s. Langas baigsis apie %s.", "%s. Das Zeitfenster endet gegen %s.", "%s. The window closes around %s.")

    // Ice
    val iceOn = L("❄ Ledas %d cm · žvejyba iš eketės", "❄ Eis %d cm · Eisangeln", "❄ Ice %d cm · ice fishing")
    val iceForming = L("❄ Ežeras stingsta: ledas %d cm", "❄ Der See friert zu: Eis %d cm", "❄ The lake is freezing: ice %d cm")
    val holeLead = L("Eketė! Įvesk galvą į eketę", "Eisloch! Kopf ins Loch führen", "The hole! Lead its head into the hole")
    val lower = L("Nuleisti", "Ablassen", "Lower")
    val winterRod = L("Žieminė meškerėlė", "Eisangel", "Ice rod")
    val frozenTitle = L("Ežeras užšalo!", "Der See ist zugefroren!", "The lake has frozen!")
    val frozenBody = L(
        "Pas tave kelias dienas šalo, ir ledas jau %d cm storio. Dabar žvejoji iš eketės su žiemine meškerėle: geriausiai kimba ešeriai, kuojos ir vėgėlės, o karpiai ir lynai miega dumble.\n\nTikrame gyvenime ant ledo eik tik tada, kai jis storesnis nei 10 cm, ir niekada vienas.",
        "Bei dir hat es tagelang gefroren, das Eis ist schon %d cm dick. Jetzt angelst du durchs Eisloch mit der Eisangel: Barsche, Rotaugen und Quappen beißen am besten, Karpfen und Schleien ruhen im Schlamm.\n\nIm echten Leben betritt das Eis erst ab 10 cm Dicke, und nie allein.",
        "It has been freezing where you are for days, and the ice is already %d cm thick. Now you fish through a hole with an ice rod: perch, roach and burbot bite best, while carp and tench rest in the mud.\n\nIn real life only step onto ice thicker than 10 cm, and never alone.",
    )
    val reportTitle = L("Dabar kimba: %s ×2", "Jetzt beißt: %s ×2", "Biting now: %s ×2")
    val reportBody = L(
        "%s, %d°, kibimas %d. Pas tave tikras oras – ir žuvys kimba pagal jį.",
        "%s, %d°, Beißindex %d. Bei dir ist echtes Wetter – und die Fische beißen danach.",
        "%s, %d°, bite index %d. Real weather where you are, and the fish bite by it.",
    )
    val hotNow = L("Dabar kimba: %s ×2", "Beißt jetzt: %s ×2", "Biting now: %s ×2")
    val rarityCommon = L("Įprasta", "Häufig", "Common")
    val rarityUncommon = L("Retesnė", "Seltener", "Uncommon")
    val rarityRare = L("Reta", "Selten", "Rare")
    val rarityVeryRare = L("Labai reta", "Sehr selten", "Very rare")
    val trophy = L("Trofėjus", "Trophäe", "Trophy")
    val caughtIn = L("Pagauta: %s, %d°, %s", "Gefangen: %s, %d°, %s", "Caught: %s, %d°, %s")

    // Sharing a catch and inviting friends
    val shareCatch = L("Pasigirk draugams", "Zeig's deinen Freunden", "Show your friends")
    val shareBonusNote = L("Pirmas pasigyrimas per dieną: +3 užmetimai", "Erstes Teilen am Tag: +3 Würfe", "First share of the day: +3 casts")
    val shareHeader = L("MANO LAIMIKIS", "MEIN FANG", "MY CATCH")
    val shareCodeLabel = L("Draugo kodas", "Freundescode", "Friend code")
    val shareChooser = L("Pasigirk laimikiu", "Fang teilen", "Share your catch")
    /** Name, weight, friend code, link. */
    val shareText = L(
        "Mano laimikis: %s, %s! 🎣 Žvejoju „Kimba!“ – žaidime žuvys kimba pagal tikrą orą. Pabandyk pranokti! Įvesk mano kodą %s ir gausi dovanų.\n%s",
        "Mein Fang: %s, %s! 🎣 Ich angle bei „Wetterbiss“ – hier beißen die Fische nach echtem Wetter. Schaffst du mehr? Mit meinem Code %s gibt's ein Geschenk.\n%s",
        "My catch: %s, %s! 🎣 I fish in “Kimba!”, where fish bite by the real weather. Can you beat it? Use my code %s for a gift.\n%s",
    )
    val friendsTitle = L("Pakviesk draugų", "Freunde einladen", "Invite friends")
    val friendsBody = L(
        "Draugas įveda tavo kodą ir gauna 250 monetų bei 10 užmetimų. Tada jo žaidimas parodo padėkos kodą: įvedęs jį čia, gausi 300 monetų ir 5 užmetimus.",
        "Dein Freund gibt deinen Code ein und bekommt 250 Münzen und 10 Würfe. Sein Spiel zeigt dann einen Dankescode: Gibst du ihn hier ein, bekommst du 300 Münzen und 5 Würfe.",
        "Your friend enters your code and gets 250 coins and 10 casts. Their game then shows a thank-you code: enter it here for 300 coins and 5 casts.",
    )
    val yourCode = L("Tavo kodas", "Dein Code", "Your code")
    val invite = L("Pakviesti", "Einladen", "Invite")
    val inviteChooser = L("Pakviesk draugą", "Freund einladen", "Invite a friend")
    val inviteText = L(
        "Žaisk „Kimba!“ kartu su manim – žvejyba pagal tikrą orą 🎣 Įvesk mano kodą %s ir gausi 250 monetų bei 10 užmetimų.\n%s",
        "Spiel „Wetterbiss“ mit mir – Angeln mit echtem Wetter 🎣 Gib meinen Code %s ein und du bekommst 250 Münzen und 10 Würfe.\n%s",
        "Play “Kimba!” with me – fishing with real weather 🎣 Enter my code %s to get 250 coins and 10 casts.\n%s",
    )
    val enterCode = L("Draugo arba padėkos kodas", "Freundes- oder Dankescode", "Friend or thank-you code")
    val redeem = L("Gauti", "Einlösen", "Redeem")
    val codeBad = L("Kodas netinka arba jau panaudotas.", "Der Code passt nicht oder wurde schon benutzt.", "That code doesn't fit or was used already.")
    val thanksTitle = L("Padėkok draugui", "Bedank dich beim Freund", "Thank your friend")
    val thanksBody = L(
        "Nusiųsk šį kodą draugui, kuris tave pakvietė: jis gaus 300 monetų.",
        "Schick diesen Code dem Freund, der dich eingeladen hat: Er bekommt 300 Münzen.",
        "Send this code to the friend who invited you: they get 300 coins.",
    )
    val thanksText = L(
        "Ačiū, kad pakvietei į „Kimba!“ 🎣 Mano padėkos kodas tau: %s",
        "Danke für die Einladung zu „Wetterbiss“ 🎣 Mein Dankescode für dich: %s",
        "Thanks for inviting me to “Kimba!” 🎣 My thank-you code for you: %s",
    )
    val send = L("Siųsti", "Senden", "Send")
    val giftFriendTitle = L("Draugo dovana!", "Geschenk vom Freund!", "A gift from your friend!")
    val giftThanksTitle = L("Draugas prisijungė!", "Dein Freund ist dabei!", "Your friend joined!")
    val giftShareTitle = L("Ačiū, kad pasigyrei!", "Danke fürs Teilen!", "Thanks for sharing!")
    val giftFriendNote = L(
        "Padėkos kodą draugui rasi skiltyje „Dienos užduotys“.",
        "Den Dankescode für deinen Freund findest du bei den Tagesaufgaben.",
        "You'll find the thank-you code for your friend under Daily quests.",
    )
    fun plusCasts(n: Int) = L("+$n ${Plural.lt(n, "užmetimas", "užmetimai", "užmetimų")}", "+$n ${if (n == 1) "Wurf" else "Würfe"}", "+$n ${if (n == 1) "cast" else "casts"}")

    fun sky(s: Sky) = when (s) {
        Sky.CLEAR -> L("Giedra", "Klar", "Clear")
        Sky.PARTLY -> L("Mažai debesuota", "Wolkig", "Partly cloudy")
        Sky.CLOUDY -> L("Apsiniaukę", "Bedeckt", "Overcast")
        Sky.FOG -> L("Rūkas", "Nebel", "Fog")
        Sky.DRIZZLE -> L("Dulksna", "Nieselregen", "Drizzle")
        Sky.RAIN -> L("Lietus", "Regen", "Rain")
        Sky.HEAVY_RAIN -> L("Liūtis", "Starkregen", "Heavy rain")
        Sky.SLEET -> L("Šlapdriba", "Schneeregen", "Sleet")
        Sky.SNOW -> L("Sniegas", "Schnee", "Snow")
        Sky.THUNDER -> L("Perkūnija", "Gewitter", "Thunderstorm")
    }

    fun phase(p: DayPhase) = when (p) {
        DayPhase.NIGHT -> L("Naktis", "Nacht", "Night")
        DayPhase.DAWN -> L("Aušra", "Morgendämmerung", "Dawn")
        DayPhase.DAY -> L("Diena", "Tag", "Day")
        DayPhase.DUSK -> L("Sutemos", "Abenddämmerung", "Dusk")
    }

    fun moonName(age: Double) = when {
        age < 0.03 || age > 0.97 -> L("Jaunatis", "Neumond", "New moon")
        age < 0.22 -> L("Jaunas mėnulis", "Zunehmende Sichel", "Waxing crescent")
        age < 0.28 -> L("Priešpilnis", "Erstes Viertel", "First quarter")
        age < 0.47 -> L("Didėjantis mėnulis", "Zunehmender Mond", "Waxing gibbous")
        age < 0.53 -> L("Pilnatis", "Vollmond", "Full moon")
        age < 0.72 -> L("Mažėjantis mėnulis", "Abnehmender Mond", "Waning gibbous")
        age < 0.78 -> L("Delčia", "Letztes Viertel", "Last quarter")
        else -> L("Senas mėnulis", "Abnehmende Sichel", "Waning crescent")
    }
}
