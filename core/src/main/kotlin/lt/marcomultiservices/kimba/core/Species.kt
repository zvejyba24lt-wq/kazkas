package lt.marcomultiservices.kimba.core

import kotlin.math.max

/**
 * A freshwater fish of Central and Northern Europe. Its [activity] says how eagerly it bites
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
    val activity: (Conditions) -> Double,
) {
    fun grams(cm: Double): Int = (k * cm * cm * cm).toInt()
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
    val roach = Species(
        "roach", L("Kuoja", "Rotauge", "Roach"),
        L("Kimba beveik visada, labiausiai sutemus.", "Beißt fast immer, am liebsten in der Dämmerung.", "Bites almost always, best at dusk."),
        12, 38, 0.0115, 0.18, 0.3,
    ) { c -> 0.25 + 0.75 * c.bell(c.waterTempC, 14.0, 9.0) * c.byPhase(0.3, 0.9, 0.8, 1.0) }

    val bleak = Species(
        "bleak", L("Aukšlė", "Ukelei", "Bleak"),
        L("Saulėta diena, šiltas paviršius.", "Sonniger Tag, warmes Oberflächenwasser.", "Sunny day, warm surface water."),
        8, 20, 0.008, 0.1, 0.05,
    ) { c -> c.bell(c.waterTempC, 18.0, 6.0) * c.byPhase(0.05, 0.6, 1.0, 0.8) * (0.4 + 0.6 * (1 - c.weather.cloud)) * (1 - 0.6 * c.rain) }

    val perch = Species(
        "perch", L("Ešerys", "Barsch", "Perch"),
        L("Šviesi diena, giedras dangus.", "Heller Tag, klarer Himmel.", "Bright day, clear sky."),
        12, 48, 0.0125, 0.35, 0.5,
    ) { c -> c.bell(c.waterTempC, 15.0, 8.0) * c.byPhase(0.05, 0.9, 1.0, 0.8) * (0.6 + 0.4 * (1 - c.weather.cloud)) }

    val rudd = Species(
        "rudd", L("Raudė", "Rotfeder", "Rudd"),
        L("Karšta vasaros diena prie nendrių.", "Heißer Sommertag am Schilf.", "Hot summer day by the reeds."),
        12, 36, 0.013, 0.22, 0.1,
    ) { c -> c.bell(c.waterTempC, 20.0, 5.0) * c.byPhase(0.05, 0.6, 1.0, 0.7) * (0.5 + 0.5 * (1 - c.weather.cloud)) }

    val crucian = Species(
        "crucian", L("Karosas", "Karausche", "Crucian carp"),
        L("Ramus, šiltas vanduo be vėjo.", "Ruhiges, warmes Wasser ohne Wind.", "Calm, warm water without wind."),
        10, 35, 0.02, 0.3, 0.15,
    ) { c -> c.bell(c.waterTempC, 20.0, 5.0) * c.byPhase(0.4, 1.0, 0.5, 0.9) * (1 - c.weather.windMs / 14).coerceIn(0.2, 1.0) }

    val tench = Species(
        "tench", L("Lynas", "Schleie", "Tench"),
        L("Šilta aušra, lengvas lietus ar rūkas.", "Warme Morgendämmerung, Nieselregen oder Nebel.", "Warm dawn, drizzle or mist."),
        20, 60, 0.015, 0.5, 0.2,
    ) { c -> c.bell(c.waterTempC, 19.0, 5.0) * c.byPhase(0.3, 1.0, 0.3, 0.8) * (1 + 0.6 * c.rain + if (c.weather.sky == Sky.FOG) 0.8 else 0.0) }

    val bream = Species(
        "bream", L("Karšis", "Brasse", "Bream"),
        L("Šiltos naktys, debesuota.", "Warme Nächte, bewölkt.", "Warm nights, overcast."),
        25, 70, 0.014, 0.45, 0.65,
    ) { c -> c.bell(c.waterTempC, 17.0, 7.0) * c.byPhase(1.0, 0.8, 0.3, 0.9) * (0.6 + 0.4 * c.weather.cloud) }

    val carp = Species(
        "carp", L("Karpis", "Karpfen", "Carp"),
        L("Vasaros naktis ir aušra, šiltas vanduo.", "Sommernacht und Morgengrauen, warmes Wasser.", "Summer night and dawn, warm water."),
        30, 95, 0.016, 0.8, 0.4,
    ) { c -> c.bell(c.waterTempC, 21.0, 5.0) * c.byPhase(0.9, 1.0, 0.4, 0.8) }

    val pike = Species(
        "pike", L("Lydeka", "Hecht", "Pike"),
        L("Apsiniaukę, krentantis slėgis, vėjas raibina vandenį.", "Trüb, fallender Luftdruck, Wind kräuselt das Wasser.", "Overcast, falling pressure, wind rippling the water."),
        40, 120, 0.0065, 0.8, 0.7,
    ) { c -> c.bell(c.waterTempC, 10.0, 9.0) * c.byPhase(0.1, 1.0, 0.6, 1.0) * (0.5 + 0.5 * c.weather.cloud) * (1 + 1.2 * c.falling) * (0.8 + 0.3 * (c.weather.windMs / 8).coerceAtMost(1.0)) }

    val zander = Species(
        "zander", L("Sterkas", "Zander", "Zander"),
        L("Sutemos ir naktis, lietus, krentantis slėgis.", "Dämmerung und Nacht, Regen, fallender Luftdruck.", "Dusk and night, rain, falling pressure."),
        35, 95, 0.0085, 0.6, 0.85,
    ) { c -> c.bell(c.waterTempC, 14.0, 8.0) * c.byPhase(1.0, 0.9, 0.15, 1.0) * (0.6 + 0.4 * c.weather.cloud) * (1 + 0.8 * c.rain + 0.8 * c.falling) }

    val eel = Species(
        "eel", L("Ungurys", "Aal", "Eel"),
        L("Tamsios, šiltos naktys, ypač per jaunatį.", "Dunkle, warme Nächte, besonders bei Neumond.", "Dark, warm nights, especially at new moon."),
        45, 110, 0.0016, 0.6, 0.6,
    ) { c -> c.bell(c.waterTempC, 19.0, 5.0) * c.byPhase(1.0, 0.2, 0.0, 0.5) * (1 + 0.8 * c.rain) * (1.6 - c.moonIllumination) }

    val catfish = Species(
        "catfish", L("Šamas", "Wels", "Wels catfish"),
        L("Karštos naktys prieš perkūniją.", "Heiße Nächte vor dem Gewitter.", "Hot nights before a thunderstorm."),
        60, 200, 0.006, 1.0, 0.95,
    ) { c -> c.bell(c.waterTempC, 22.0, 5.0) * c.byPhase(1.0, 0.4, 0.05, 0.7) * (1 + c.falling) * (if (c.weather.sky == Sky.THUNDER) 4.0 else 1.0) }

    val burbot = Species(
        "burbot", L("Vėgėlė", "Quappe", "Burbot"),
        L("Šaltos žiemos naktys.", "Kalte Winternächte.", "Cold winter nights."),
        30, 80, 0.0065, 0.5, 0.9,
    ) { c -> c.bell(c.waterTempC, 3.0, 4.0) * c.byPhase(1.0, 0.5, 0.05, 0.8) }

    val all = listOf(roach, bleak, perch, rudd, crucian, tench, bream, carp, pike, zander, eel, catfish, burbot)

    /** Relative abundance: common fish bite more often than rare ones at equal activity. */
    private val abundance = mapOf(
        "roach" to 1.0, "bleak" to 0.7, "perch" to 0.8, "rudd" to 0.55, "crucian" to 0.5, "tench" to 0.4,
        "bream" to 0.6, "carp" to 0.4, "pike" to 0.5, "zander" to 0.4, "eel" to 0.2, "catfish" to 0.12, "burbot" to 0.25,
    )

    val goldens = listOf(
        Golden(
            "moon_pike", pike, L("Mėnesienos lydeka", "Mondhecht", "Moonlight Pike"),
            L("Pilnaties naktį", "In einer Vollmondnacht", "On a full-moon night"),
        ) { c -> c.fullMoon && (c.phase == DayPhase.NIGHT || c.phase == DayPhase.DUSK) },
        Golden(
            "storm_catfish", catfish, L("Perkūno šamas", "Gewitterwels", "Thunder Catfish"),
            L("Kai griaudžia perkūnija", "Wenn ein Gewitter grollt", "While thunder rolls"),
        ) { c -> c.weather.sky == Sky.THUNDER },
        Golden(
            "frost_perch", perch, L("Šerkšno ešerys", "Frostbarsch", "Frost Perch"),
            L("Kai pas tave šąla (0 °C ir mažiau)", "Wenn es bei dir friert (0 °C und kälter)", "When it freezes where you are (0 °C or colder)"),
        ) { c -> c.frost },
        Golden(
            "mist_tench", tench, L("Rūko lynas", "Nebelschleie", "Mist Tench"),
            L("Rūke, auštant arba naktį", "Im Nebel, bei Tagesanbruch oder nachts", "In fog, at dawn or at night"),
        ) { c -> c.weather.sky == Sky.FOG && c.phase != DayPhase.DAY },
        Golden(
            "rain_zander", zander, L("Liūties sterkas", "Regenzander", "Downpour Zander"),
            L("Per stiprų lietų", "Bei starkem Regen", "In heavy rain"),
        ) { c -> c.weather.sky == Sky.HEAVY_RAIN || (c.rain >= 0.6 && c.falling > 0.3) },
        Golden(
            "dark_eel", eel, L("Jaunaties ungurys", "Neumond-Aal", "New-Moon Eel"),
            L("Jaunaties naktį", "In einer Neumondnacht", "On a new-moon night"),
        ) { c -> c.newMoon && c.phase == DayPhase.NIGHT },
    )

    fun byId(id: String): Species? = all.firstOrNull { it.id == id }
    fun goldenById(id: String): Golden? = goldens.firstOrNull { it.id == id }

    /**
     * How likely each species is to bite right now, already weighted by abundance.
     * A species whose golden form is active is lifted so that the golden one is catchable.
     */
    fun activities(c: Conditions): Map<Species, Double> {
        val boosted = goldens.filter { it.active(c) }.map { it.base }.toSet()
        return all.associateWith { s ->
            val a = s.activity(c).coerceIn(0.0, 1.5) * abundance.getValue(s.id)
            if (s in boosted) max(a, 0.35) else max(a, 0.01)
        }
    }

    /** Bite index 0..100 shown to the player: how good fishing is right now. */
    fun biteIndex(c: Conditions): Int {
        val sum = activities(c).values.sum()
        return (sum / 3.0 * 100).toInt().coerceIn(1, 100)
    }
}
