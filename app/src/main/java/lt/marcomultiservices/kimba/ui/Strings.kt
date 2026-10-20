package lt.marcomultiservices.kimba.ui

import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.L
import lt.marcomultiservices.kimba.core.Sky

/** All player-facing text of the app. */
object S {
    val appName = L("Kimba!", "Wetterbiss", "Kimba!")
    val tagline = L("Žvejyba pagal tikrą orą", "Angeln mit echtem Wetter", "Fishing with real weather")

    // Welcome
    val welcomeTitle = L("Žuvys kimba pagal tikrą orą", "Die Fische beißen nach echtem Wetter", "Fish bite by the real weather")
    val welcomeBody = L(
        "Lyja pas tave – lyja ir ant ežero. Sterkai mėgsta lietų, lydekos – krentantį slėgį, o auksinės žuvys pasirodo tik per pilnatį, perkūniją ar šalną. Tikrą.",
        "Regnet es bei dir, regnet es auch am See. Zander lieben Regen, Hechte fallenden Luftdruck, und goldene Fische zeigen sich nur bei Vollmond, Gewitter oder Frost. Echtem.",
        "If it rains where you are, it rains on the lake. Zander love rain, pike love falling pressure, and golden fish only show up at full moon, in thunder or frost. Real ones.",
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
    val noCasts = L("Užmetimai baigėsi", "Keine Würfe mehr", "Out of casts")
    val noCastsBody = L("Naujas užmetimas atsiras po %s.", "Ein neuer Wurf kommt in %s.", "A new cast arrives in %s.")
    val bonusCasts = L("+3 užmetimai", "+3 Würfe", "+3 casts")
    val bonusNote = L("Vėliau – už trumpą reklamą", "Später – für ein kurzes Werbevideo", "Later – for a short ad")
    val lure = L("Masalas", "Köder", "Bait")

    // Results
    val caught = L("Pagavai!", "Gefangen!", "Caught!")
    val newSpecies = L("Nauja rūšis!", "Neue Art!", "New species!")
    val newRecord = L("Naujas rekordas!", "Neuer Rekord!", "New record!")
    val goldenCaught = L("AUKSINĖ ŽUVIS!", "GOLDENER FISCH!", "GOLDEN FISH!")
    val snapped = L("Valas nutrūko!", "Die Schnur ist gerissen!", "The line snapped!")
    val snappedHint = L("Kai žuvis tempia, paleisk ritę.", "Wenn der Fisch zieht, lass die Rolle los.", "When the fish runs, let go of the reel.")
    val escaped = L("Žuvis nusikabino", "Der Fisch ist ausgeschlitzt", "The fish got off")
    val escapedHint = L("Neleisk valui per ilgai kabėti laisvai.", "Lass die Schnur nicht zu lange locker.", "Don't leave the line slack for too long.")
    val missed = L("Nespėjai kirsti", "Anhieb verpasst", "Missed the strike")
    val missedHint = L("Kai plūdė panyra – kirsk iškart.", "Taucht die Pose ab – sofort anschlagen.", "When the float goes under, strike at once.")
    val again = L("Užmesti dar", "Nochmal", "Cast again")
    val length = L("Ilgis", "Länge", "Length")
    val weight = L("Svoris", "Gewicht", "Weight")

    // Book
    val book = L("Žuvų knyga", "Fangbuch", "Fish book")
    val bookProgress = L("Pagauta %d iš %d", "%d von %d gefangen", "%d of %d caught")
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
        "Per artimiausias 2 dienas auksinėms žuvims sąlygų nebus. Kitos pilnaties liko %d d., jaunaties – %d d.",
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
    val notifications = L("Pranešimai apie auksinių žuvų langus", "Hinweise auf Zeitfenster für goldene Fische", "Alerts for golden fish windows")
    val prototype = L(
        "Prototipas. Reklamos, parduotuvės ir kitų vandenų dar nėra.",
        "Prototyp. Werbung, Shop und weitere Gewässer kommen später.",
        "Prototype. Ads, shop and more waters come later.",
    )
    val back = L("Atgal", "Zurück", "Back")

    // Notifications
    val notifyTitle = L("%s kimba dabar!", "%s beißt jetzt!", "%s is biting now!")
    val notifyBody = L("%s. Langas baigsis apie %s.", "%s. Das Zeitfenster endet gegen %s.", "%s. The window closes around %s.")

    fun sky(s: Sky) = when (s) {
        Sky.CLEAR -> L("Giedra", "Klar", "Clear")
        Sky.PARTLY -> L("Debesuota su pragiedruliais", "Wolkig", "Partly cloudy")
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
        age < 0.47 -> L("Didėjantis", "Zunehmend", "Waxing gibbous")
        age < 0.53 -> L("Pilnatis", "Vollmond", "Full moon")
        age < 0.72 -> L("Delčia", "Abnehmend", "Waning gibbous")
        age < 0.78 -> L("Paskutinis ketvirtis", "Letztes Viertel", "Last quarter")
        else -> L("Senas mėnulis", "Abnehmende Sichel", "Waning crescent")
    }
}
