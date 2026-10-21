package lt.marcomultiservices.kimba.core

import kotlin.math.max

/**
 * A fish of Central and Northern European waters. Its [activity] says how eagerly it bites
 * under given conditions (0..1); the rules follow what anglers observe, simplified.
 */
class Species(
    val id: String,
    val name: L,
    /** Short angler's hint shown in the fish book. */
    val habit: L,
    val minCm: Int,
    val maxCm: Int,
    /** Weight in grams = k * length³ (length in cm). */
    private val k: Double,
    /** How hard it fights, 0..1. */
    val power: Double,
    /** Where it feeds: 0 = shallow near the reeds, 1 = deep open water. */
    val depth: Double,
    val waters: Set<Water>,
    /** How common it is; common fish bite more often than rare ones at equal activity. */
    val abundance: Double,
    /** Coins per kilogram when sold. */
    val price: Int,
    /** Only found at these trip destinations (local specialities), or anywhere its waters are. */
    val onlyAt: Set<String>? = null,
    /** Notices thick line and bites less on it. */
    val shy: Boolean = false,
    val activity: (Conditions) -> Double,
) {
    fun grams(cm: Double): Int = (k * cm * cm * cm).toInt()

    fun livesAt(spot: Spot): Boolean = spot.water in waters && (onlyAt == null || spot.trip in onlyAt)
}

/** A rare golden form of a species that only bites under one real-world condition. */
class Golden(
    val id: String,
    val base: Species,
    val name: L,
    val condition: L,
    val active: (Conditions) -> Boolean,
)

object Fish {
    private val LAKE = setOf(Water.LAKE)
    private val L_R_P = setOf(Water.LAKE, Water.RIVER, Water.POND)
    private val L_R = setOf(Water.LAKE, Water.RIVER)
    private val L_P = setOf(Water.LAKE, Water.POND)
    private val RIVER = setOf(Water.RIVER)
    private val SEA = setOf(Water.SEA)

    private fun season(c: Conditions, months: IntRange, boost: Double) = if (c.month in months) boost else 1.0

    val roach = Species(
        "roach", L("Kuoja", "Rotauge", "Roach"),
        L("Kimba beveik visada, labiausiai sutemus.", "Beißt fast immer, am liebsten in der Dämmerung.", "Bites almost always, best at dusk."),
        12, 38, 0.0115, 0.18, 0.3, L_R_P, 1.0, 60,
    ) { c -> 0.25 + 0.75 * c.bell(c.waterTempC, 14.0, 9.0) * c.byPhase(0.3, 0.9, 0.8, 1.0) }

    val bleak = Species(
        "bleak", L("Aukšlė", "Ukelei", "Bleak"),
        L("Saulėta diena, šiltas paviršius.", "Sonniger Tag, warmes Oberflächenwasser.", "Sunny day, warm surface water."),
        8, 20, 0.008, 0.1, 0.05, L_R, 0.7, 50,
    ) { c -> c.bell(c.waterTempC, 18.0, 6.0) * c.byPhase(0.05, 0.6, 1.0, 0.8) * (0.4 + 0.6 * (1 - c.weather.cloud)) * (1 - 0.6 * c.rain) }

    val perch = Species(
        "perch", L("Ešerys", "Barsch", "Perch"),
        L("Šviesi diena, giedras dangus.", "Heller Tag, klarer Himmel.", "Bright day, clear sky."),
        12, 45, 0.0125, 0.35, 0.5, L_R_P, 0.8, 90,
    ) { c -> c.bell(c.waterTempC, 15.0, 8.0) * c.byPhase(0.05, 0.9, 1.0, 0.8) * (0.6 + 0.4 * (1 - c.weather.cloud)) }

    val rudd = Species(
        "rudd", L("Raudė", "Rotfeder", "Rudd"),
        L("Karšta vasaros diena prie nendrių.", "Heißer Sommertag am Schilf.", "Hot summer day by the reeds."),
        12, 36, 0.013, 0.22, 0.1, L_P, 0.55, 60,
    ) { c -> c.bell(c.waterTempC, 20.0, 5.0) * c.byPhase(0.05, 0.6, 1.0, 0.7) * (0.5 + 0.5 * (1 - c.weather.cloud)) }

    val crucian = Species(
        "crucian", L("Karosas", "Karausche", "Crucian carp"),
        L("Ramus, šiltas vanduo be vėjo.", "Ruhiges, warmes Wasser ohne Wind.", "Calm, warm water without wind."),
        10, 35, 0.026, 0.3, 0.15, L_P, 0.5, 70,
    ) { c -> c.bell(c.waterTempC, 20.0, 5.0) * c.byPhase(0.4, 1.0, 0.5, 0.9) * (1 - c.weather.windMs / 14).coerceIn(0.2, 1.0) }

    val tench = Species(
        "tench", L("Lynas", "Schleie", "Tench"),
        L("Šilta aušra, lengvas lietus ar rūkas.", "Warme Morgendämmerung, Nieselregen oder Nebel.", "Warm dawn, drizzle or mist."),
        20, 60, 0.015, 0.5, 0.2, L_P, 0.4, 110, shy = true,
    ) { c -> c.bell(c.waterTempC, 19.0, 5.0) * c.byPhase(0.3, 1.0, 0.3, 0.8) * (1 + 0.6 * c.rain + if (c.weather.sky == Sky.FOG) 0.8 else 0.0) }

    val bream = Species(
        "bream", L("Karšis", "Brasse", "Bream"),
        L("Šiltos naktys, debesuota.", "Warme Nächte, bewölkt.", "Warm nights, overcast."),
        25, 70, 0.014, 0.45, 0.65, L_R, 0.6, 50,
    ) { c -> c.bell(c.waterTempC, 17.0, 7.0) * c.byPhase(1.0, 0.8, 0.3, 0.9) * (0.6 + 0.4 * c.weather.cloud) }

    val carp = Species(
        "carp", L("Karpis", "Karpfen", "Carp"),
        L("Vasaros naktis ir aušra, šiltas vanduo.", "Sommernacht und Morgengrauen, warmes Wasser.", "Summer night and dawn, warm water."),
        30, 95, 0.018, 0.8, 0.4, L_P, 0.4, 70, shy = true,
    ) { c -> c.bell(c.waterTempC, 21.0, 5.0) * c.byPhase(0.9, 1.0, 0.4, 0.8) }

    val pike = Species(
        "pike", L("Lydeka", "Hecht", "Pike"),
        L("Apsiniaukę, krentantis slėgis, vėjas raibina vandenį.", "Trüb, fallender Luftdruck, Wind kräuselt das Wasser.", "Overcast, falling pressure, wind rippling the water."),
        40, 120, 0.0075, 0.8, 0.7, L_R_P, 0.5, 80,
    ) { c -> c.bell(c.waterTempC, 10.0, 9.0) * c.byPhase(0.1, 1.0, 0.6, 1.0) * (0.5 + 0.5 * c.weather.cloud) * (1 + 1.2 * c.falling) * (0.8 + 0.3 * (c.weather.windMs / 8).coerceAtMost(1.0)) }

    val zander = Species(
        "zander", L("Sterkas", "Zander", "Zander"),
        L("Sutemos ir naktis, lietus, krentantis slėgis.", "Dämmerung und Nacht, Regen, fallender Luftdruck.", "Dusk and night, rain, falling pressure."),
        35, 95, 0.009, 0.6, 0.85, L_R, 0.4, 130, shy = true,
    ) { c -> c.bell(c.waterTempC, 14.0, 8.0) * c.byPhase(1.0, 0.9, 0.15, 1.0) * (0.6 + 0.4 * c.weather.cloud) * (1 + 0.8 * c.rain + 0.8 * c.falling) }

    val eel = Species(
        "eel", L("Ungurys", "Aal", "Eel"),
        L("Tamsios, šiltos naktys, ypač per jaunatį.", "Dunkle, warme Nächte, besonders bei Neumond.", "Dark, warm nights, especially at new moon."),
        45, 110, 0.0018, 0.6, 0.6, L_R, 0.2, 150,
    ) { c -> c.bell(c.waterTempC, 19.0, 5.0) * c.byPhase(1.0, 0.2, 0.0, 0.5) * (1 + 0.8 * c.rain) * (1.6 - c.moonIllumination) }

    val catfish = Species(
        "catfish", L("Šamas", "Wels", "Wels catfish"),
        L("Karštos naktys prieš perkūniją.", "Heiße Nächte vor dem Gewitter.", "Hot nights before a thunderstorm."),
        60, 200, 0.0075, 1.0, 0.95, L_R, 0.12, 40,
    ) { c -> c.bell(c.waterTempC, 22.0, 5.0) * c.byPhase(1.0, 0.4, 0.05, 0.7) * (1 + c.falling) * (if (c.weather.sky == Sky.THUNDER) 4.0 else 1.0) }

    val burbot = Species(
        "burbot", L("Vėgėlė", "Quappe", "Burbot"),
        L("Šaltos žiemos naktys.", "Kalte Winternächte.", "Cold winter nights."),
        30, 80, 0.0075, 0.5, 0.9, L_R, 0.25, 120,
    ) { c -> c.bell(c.waterTempC, 3.0, 4.0) * c.byPhase(1.0, 0.5, 0.05, 0.8) }

    // River
    val chub = Species(
        "chub", L("Šapalas", "Döbel", "Chub"),
        L("Vasarą ties srove, kai krenta vabzdžiai.", "Im Sommer an der Strömung, wenn Insekten fallen.", "In summer by the current when insects fall."),
        20, 60, 0.0135, 0.45, 0.35, RIVER, 0.7, 60,
    ) { c -> c.bell(c.waterTempC, 16.0, 8.0) * c.byPhase(0.4, 0.9, 0.8, 1.0) }

    val asp = Species(
        "asp", L("Salatis", "Rapfen", "Asp"),
        L("Saulėtą dieną medžioja paviršiuje.", "Jagt an sonnigen Tagen an der Oberfläche.", "Hunts at the surface on sunny days."),
        40, 85, 0.0105, 0.7, 0.45, RIVER, 0.35, 70,
    ) { c -> c.bell(c.waterTempC, 17.0, 6.0) * c.byPhase(0.05, 1.0, 0.7, 0.9) * (1 - 0.3 * c.weather.cloud) }

    val barbel = Species(
        "barbel", L("Ūsorius", "Barbe", "Barbel"),
        L("Naktį ir po lietaus, kai vanduo pakyla.", "Nachts und nach Regen, wenn das Wasser steigt.", "At night and after rain when the water rises."),
        30, 90, 0.011, 0.8, 0.75, RIVER, 0.3, 80,
    ) { c -> c.bell(c.waterTempC, 18.0, 6.0) * c.byPhase(1.0, 0.8, 0.3, 0.9) * (1 + 0.6 * c.rain) }

    val trout = Species(
        "trout", L("Upėtakis", "Bachforelle", "Brown trout"),
        L("Vėsus vanduo, apsiniaukę, aušra.", "Kühles Wasser, bedeckt, Morgengrauen.", "Cool water, overcast, dawn."),
        20, 60, 0.0115, 0.6, 0.4, RIVER, 0.35, 160, shy = true,
    ) { c -> c.bell(c.waterTempC, 10.0, 5.0) * c.byPhase(0.3, 1.0, 0.5, 1.0) * (0.6 + 0.4 * c.weather.cloud) * (1 + 0.5 * c.rain) }

    val grayling = Species(
        "grayling", L("Kiršlys", "Äsche", "Grayling"),
        L("Šviesi, vėsi diena skaidriame vandenyje.", "Heller, kühler Tag in klarem Wasser.", "Bright, cool day in clear water."),
        20, 50, 0.01, 0.4, 0.3, RIVER, 0.3, 150, shy = true,
    ) { c -> c.bell(c.waterTempC, 9.0, 5.0) * c.byPhase(0.05, 0.8, 1.0, 0.8) * (0.6 + 0.4 * (1 - c.weather.cloud)) }

    // Pond
    val grassCarp = Species(
        "grass_carp", L("Baltasis amūras", "Graskarpfen", "Grass carp"),
        L("Karšta, saulėta diena.", "Heißer, sonniger Tag.", "Hot, sunny day."),
        40, 110, 0.012, 0.85, 0.3, setOf(Water.POND), 0.3, 60, shy = true,
    ) { c -> c.bell(c.waterTempC, 23.0, 4.0) * c.byPhase(0.3, 0.9, 1.0, 0.8) * (0.5 + 0.5 * (1 - c.weather.cloud)) }

    // Baltic Sea
    val cod = Species(
        "cod", L("Menkė", "Dorsch", "Cod"),
        L("Šaltas vanduo, gylis, vėjuotas oras.", "Kaltes Wasser, Tiefe, windiges Wetter.", "Cold water, depth, windy weather."),
        30, 100, 0.0105, 0.65, 0.9, SEA, 0.6, 90,
    ) { c -> c.bell(c.waterTempC, 6.0, 6.0) * c.byPhase(0.6, 0.9, 0.7, 0.9) * (1 + 0.3 * (c.weather.windMs / 10).coerceAtMost(1.0)) }

    val flounder = Species(
        "flounder", L("Plekšnė", "Flunder", "Flounder"),
        L("Smėlėtas dugnas, sutemos, sliekas.", "Sandiger Grund, Dämmerung, Wurm.", "Sandy bottom, dusk, worm."),
        20, 50, 0.012, 0.35, 0.55, SEA, 0.7, 70,
    ) { c -> c.bell(c.waterTempC, 10.0, 7.0) * c.byPhase(0.8, 0.9, 0.6, 1.0) }

    val herring = Species(
        "herring", L("Strimėlė", "Hering", "Herring"),
        L("Pavasarį ir rudenį plaukia būriais prie kranto.", "Zieht im Frühjahr und Herbst in Schwärmen an die Küste.", "Runs in shoals to the shore in spring and autumn."),
        15, 35, 0.008, 0.25, 0.4, SEA, 0.9, 40,
    ) { c -> c.bell(c.waterTempC, 8.0, 6.0) * c.byPhase(0.2, 1.0, 0.6, 1.0) * season(c, 3..5, 1.6) * season(c, 9..11, 1.6) }

    val seaTrout = Species(
        "sea_trout", L("Šlakys", "Meerforelle", "Sea trout"),
        L("Vėsus, vėjuotas, apsiniaukęs oras prie kranto.", "Kühles, windiges, trübes Wetter an der Küste.", "Cool, windy, overcast weather by the shore."),
        40, 90, 0.011, 0.75, 0.6, SEA, 0.25, 170, shy = true,
    ) { c -> c.bell(c.waterTempC, 8.0, 6.0) * c.byPhase(0.4, 1.0, 0.4, 1.0) * (0.6 + 0.4 * c.weather.cloud) * (1 + 0.3 * (c.weather.windMs / 8).coerceAtMost(1.0)) }

    val garfish = Species(
        "garfish", L("Vėjažuvė", "Hornhecht", "Garfish"),
        L("Gegužę, šviesią dieną paviršiuje.", "Im Mai, an hellen Tagen an der Oberfläche.", "In May, on bright days at the surface."),
        45, 90, 0.0018, 0.45, 0.2, SEA, 0.4, 90,
    ) { c -> c.bell(c.waterTempC, 14.0, 4.0) * c.byPhase(0.05, 0.8, 1.0, 0.8) * season(c, 4..6, 2.0) }

    val turbot = Species(
        "turbot", L("Otas", "Steinbutt", "Turbot"),
        L("Retas jūros lobis, sutemose ant smėlio.", "Seltener Meeresschatz, abends auf Sand.", "Rare sea treasure, on sand at dusk."),
        30, 70, 0.02, 0.6, 0.7, SEA, 0.12, 200,
    ) { c -> c.bell(c.waterTempC, 14.0, 5.0) * c.byPhase(1.0, 0.7, 0.3, 0.8) }

    // Local specialities
    val whitefish = Species(
        "whitefish", L("Sykas", "Felchen", "Whitefish"),
        L("Gilus, vėsus ežeras, ramus rytas.", "Tiefer, kühler See, ruhiger Morgen.", "Deep, cool lake, calm morning."),
        25, 55, 0.009, 0.4, 0.8, LAKE, 0.6, 140, onlyAt = setOf("bodensee", "mueritz"),
    ) { c -> c.bell(c.waterTempC, 10.0, 6.0) * c.byPhase(0.3, 1.0, 0.8, 0.9) }

    val huchen = Species(
        "huchen", L("Dunojaus lašiša", "Huchen", "Danube salmon"),
        L("Šaltas metų laikas, apsiniaukę, sutemos.", "Kalte Jahreszeit, bedeckt, Dämmerung.", "Cold season, overcast, dusk."),
        60, 130, 0.0095, 0.95, 0.85, RIVER, 0.15, 200, onlyAt = setOf("wachau"),
    ) { c -> c.bell(c.waterTempC, 6.0, 5.0) * c.byPhase(0.3, 1.0, 0.3, 1.0) * (0.6 + 0.4 * c.weather.cloud) }

    val vimba = Species(
        "vimba", L("Žiobris", "Zährte", "Vimba"),
        L("Gegužę kyla upe neršti.", "Steigt im Mai zum Laichen die Flüsse hinauf.", "Swims upriver to spawn in May."),
        20, 45, 0.012, 0.4, 0.5, setOf(Water.RIVER, Water.LAKE), 0.6, 100, onlyAt = setOf("nemunas", "curonian"),
    ) { c -> c.bell(c.waterTempC, 15.0, 6.0) * c.byPhase(0.3, 1.0, 0.6, 1.0) * season(c, 4..6, 2.0) }

    val all = listOf(
        roach, bleak, perch, rudd, crucian, tench, bream, carp, pike, zander, eel, catfish, burbot,
        chub, asp, barbel, trout, grayling, grassCarp,
        cod, flounder, herring, seaTrout, garfish, turbot,
        whitefish, huchen, vimba,
    )

    val goldens = listOf(
        Golden("moon_pike", pike, L("Mėnesienos lydeka", "Mondhecht", "Moonlight Pike"), L("Pilnaties naktį", "In einer Vollmondnacht", "On a full-moon night")) { c ->
            c.fullMoon && (c.phase == DayPhase.NIGHT || c.phase == DayPhase.DUSK)
        },
        Golden("storm_catfish", catfish, L("Perkūno šamas", "Gewitterwels", "Thunder Catfish"), L("Kai griaudžia perkūnija", "Wenn ein Gewitter grollt", "While thunder rolls")) { c ->
            c.weather.sky == Sky.THUNDER
        },
        Golden("frost_perch", perch, L("Šerkšno ešerys", "Frostbarsch", "Frost Perch"), L("Kai pas tave šąla (0 °C ir mažiau)", "Wenn es bei dir friert (0 °C und kälter)", "When it freezes where you are (0 °C or colder)")) { c ->
            c.frost
        },
        Golden("mist_tench", tench, L("Rūko lynas", "Nebelschleie", "Mist Tench"), L("Rūke, auštant arba naktį", "Im Nebel, bei Tagesanbruch oder nachts", "In fog, at dawn or at night")) { c ->
            c.weather.sky == Sky.FOG && c.phase != DayPhase.DAY
        },
        Golden("rain_zander", zander, L("Liūties sterkas", "Regenzander", "Downpour Zander"), L("Per stiprų lietų", "Bei starkem Regen", "In heavy rain")) { c ->
            c.weather.sky == Sky.HEAVY_RAIN || (c.rain >= 0.6 && c.falling > 0.3)
        },
        Golden("dark_eel", eel, L("Jaunaties ungurys", "Neumond-Aal", "New-Moon Eel"), L("Jaunaties naktį", "In einer Neumondnacht", "On a new-moon night")) { c ->
            c.newMoon && c.phase == DayPhase.NIGHT
        },
        Golden("dawn_trout", trout, L("Aušros upėtakis", "Morgenrot-Forelle", "Sunrise Trout"), L("Giedrą aušrą prie upės", "Bei klarem Morgenrot am Fluss", "At a clear sunrise by the river")) { c ->
            c.phase == DayPhase.DAWN && c.weather.cloud < 0.4
        },
        Golden("storm_cod", cod, L("Štormo menkė", "Sturmdorsch", "Storm Cod"), L("Kai pučia stiprus vėjas (10 m/s ir daugiau)", "Bei starkem Wind (10 m/s und mehr)", "In strong wind (10 m/s or more)")) { c ->
            c.weather.windMs >= 10
        },
        Golden("heat_carp", carp, L("Karščio karpis", "Hitzekarpfen", "Heatwave Carp"), L("Kai pas tave karšta (26 °C ir daugiau)", "Wenn es bei dir heiß ist (26 °C und mehr)", "When it is hot where you are (26 °C or more)")) { c ->
            c.weather.tempC >= 26
        },
        Golden("snow_burbot", burbot, L("Sniego vėgėlė", "Schneequappe", "Snow Burbot"), L("Kai sninga", "Wenn es schneit", "While it snows")) { c ->
            c.weather.sky == Sky.SNOW
        },
    )

    /** Plural for "catch 3 …": Lithuanian accusative, German and English plural. */
    private val plurals = mapOf(
        "roach" to L("kuojas", "Rotaugen", "roach"), "bleak" to L("aukšles", "Ukeleis", "bleak"),
        "perch" to L("ešerius", "Barsche", "perch"), "rudd" to L("raudes", "Rotfedern", "rudd"),
        "crucian" to L("karosus", "Karauschen", "crucian carp"), "tench" to L("lynus", "Schleien", "tench"),
        "bream" to L("karšius", "Brassen", "bream"), "carp" to L("karpius", "Karpfen", "carp"),
        "pike" to L("lydekas", "Hechte", "pike"), "zander" to L("sterkus", "Zander", "zander"),
        "eel" to L("ungurius", "Aale", "eels"), "catfish" to L("šamus", "Welse", "wels catfish"),
        "burbot" to L("vėgėles", "Quappen", "burbot"), "chub" to L("šapalus", "Döbel", "chub"),
        "asp" to L("salačius", "Rapfen", "asp"), "barbel" to L("ūsorius", "Barben", "barbel"),
        "trout" to L("upėtakius", "Bachforellen", "brown trout"), "grayling" to L("kiršlius", "Äschen", "grayling"),
        "grass_carp" to L("baltuosius amūrus", "Graskarpfen", "grass carp"), "cod" to L("menkes", "Dorsche", "cod"),
        "flounder" to L("plekšnes", "Flundern", "flounder"), "herring" to L("strimėles", "Heringe", "herring"),
        "sea_trout" to L("šlakius", "Meerforellen", "sea trout"), "garfish" to L("vėjažuves", "Hornhechte", "garfish"),
        "turbot" to L("otus", "Steinbutte", "turbot"), "whitefish" to L("sykus", "Felchen", "whitefish"),
        "huchen" to L("Dunojaus lašišas", "Huchen", "Danube salmon"), "vimba" to L("žiobrius", "Zährten", "vimba"),
    )

    fun plural(id: String): L? = plurals[id]

    fun byId(id: String): Species? = all.firstOrNull { it.id == id }
    fun goldenById(id: String): Golden? = goldens.firstOrNull { it.id == id }

    /**
     * How likely each species living at [spot] is to bite right now, weighted by abundance.
     * A species whose golden form is active is lifted so that the golden one is catchable.
     */
    fun activities(c: Conditions, spot: Spot = Spot.HOME_LAKE): Map<Species, Double> {
        val boosted = goldens.filter { it.active(c) }.map { it.base }.toSet()
        return all.filter { it.livesAt(spot) }.associateWith { s ->
            val a = s.activity(c).coerceIn(0.0, 1.5) * s.abundance
            if (s in boosted) max(a, 0.35) else max(a, 0.01)
        }
    }

    /** Golden fish that can be caught at [spot] (their base species lives there). */
    fun goldensAt(spot: Spot): List<Golden> = goldens.filter { it.base.livesAt(spot) }

    /** Bite index 0..100 shown to the player: how good fishing is right now. */
    fun biteIndex(c: Conditions, spot: Spot = Spot.HOME_LAKE): Int {
        val sum = activities(c, spot).values.sum()
        val full = all.filter { it.livesAt(spot) }.sumOf { it.abundance }
        return (sum / (0.5 * full) * 100).toInt().coerceIn(1, 100)
    }
}
