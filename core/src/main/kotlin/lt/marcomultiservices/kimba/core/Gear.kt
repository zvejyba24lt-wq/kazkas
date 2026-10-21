package lt.marcomultiservices.kimba.core

/** Something sold in the shop. Prices are in coins earned by fishing; nothing costs real money. */
interface Item {
    val key: String
    val label: L
    val price: Int
    val level: Int
}

/**
 * The kind of rod. It decides which baits can be used and how a bite shows: the float on a
 * float rod, the rod tip on a feeder (bottom) rod, a knock on the line while a spinning lure
 * is wound back.
 */
enum class Tackle(override val label: L, override val price: Int, override val level: Int, val note: L) : Item {
    FLOAT(L("Plūdinė meškerė", "Posenrute", "Float rod"), 0, 1,
        L("Kibimą rodo plūdė. Sliekas, lervos, kukurūzas.", "Die Pose zeigt den Biss. Wurm, Maden, Mais.", "The float shows the bite. Worm, maggots, sweetcorn.")),
    FEEDER(L("Fiderinė (dugninė) meškerė", "Feederrute (Grundrute)", "Feeder (bottom) rod"), 700, 2,
        L("Masalas guli ant dugno, kibimą rodo meškerės galūnė. Stambesnės dugno žuvys.", "Der Köder liegt am Grund, die Rutenspitze zeigt den Biss. Größere Grundfische.", "The bait lies on the bottom; the rod tip shows the bite. Bigger bottom fish.")),
    SPIN(L("Spiningas", "Spinnrute", "Spinning rod"), 900, 2,
        L("Blizgės ir vobleriai plėšrūnams. Blizgė komplekte.", "Blinker und Wobbler für Räuber. Blinker inklusive.", "Spinners and wobblers for predators. Spinner included."));

    override val key: String get() = "tackle:$name"

    /** Fish that feed on the bottom bite better on a feeder; surface fish hardly ever. */
    fun affinity(s: Species): Double = when (this) {
        FEEDER -> when (s.id) {
            "bream", "carp", "tench", "crucian", "eel", "catfish", "barbel", "burbot", "flounder", "turbot", "grass_carp", "vimba", "cod" -> 1.7
            "bleak", "rudd", "garfish", "herring" -> 0.3
            else -> 1.0
        }
        else -> 1.0
    }
}

/** What hangs on the hook. Each lure draws different fish. */
enum class Lure(
    override val label: L,
    override val price: Int,
    override val level: Int,
    val note: L,
    private val default: Double,
    private val likes: Map<String, Double>,
) : Item {
    WORM(
        L("Sliekas", "Wurm", "Worm"), 0, 1,
        L("Viskam po truputį", "Ein bisschen für alles", "A bit of everything"), 1.0,
        mapOf("eel" to 1.6, "perch" to 1.3, "tench" to 1.3, "burbot" to 1.4, "catfish" to 1.2, "flounder" to 2.0, "barbel" to 1.4,
            "garfish" to 0.3, "herring" to 0.5, "asp" to 0.1, "huchen" to 0.1, "grass_carp" to 0.3, "turbot" to 0.6),
    ),
    MAGGOT(
        L("Musių lervos", "Maden", "Maggots"), 120, 1,
        L("Kuojos, aukšlės, kiršliai", "Rotaugen, Ukelei, Äschen", "Roach, bleak, grayling"), 0.8,
        mapOf("roach" to 2.0, "bleak" to 2.5, "rudd" to 1.8, "bream" to 1.4, "chub" to 1.5, "grayling" to 2.2, "vimba" to 1.8, "herring" to 1.4,
            "whitefish" to 1.8, "pike" to 0.03, "zander" to 0.05, "catfish" to 0.05, "cod" to 0.2, "turbot" to 0.2, "asp" to 0.1, "huchen" to 0.02, "carp" to 0.4, "garfish" to 0.3),
    ),
    CORN(
        L("Kukurūzas", "Mais", "Sweetcorn"), 250, 2,
        L("Karpiai, karosai, lynai", "Karpfen, Karauschen, Schleien", "Carp, crucian, tench"), 0.05,
        mapOf("carp" to 3.0, "crucian" to 2.0, "tench" to 1.8, "bream" to 1.8, "roach" to 1.3, "rudd" to 1.3, "grass_carp" to 2.5, "chub" to 1.2, "barbel" to 1.3, "vimba" to 1.0),
    ),
    SPINNER(
        L("Blizgė", "Blinker", "Spinner"), 400, 2,
        L("Lydekos, ešeriai, upėtakiai", "Hechte, Barsche, Forellen", "Pike, perch, trout"), 0.05,
        mapOf("pike" to 3.0, "perch" to 2.2, "zander" to 2.2, "catfish" to 1.2, "asp" to 2.5, "trout" to 2.5, "chub" to 1.2, "sea_trout" to 2.5,
            "garfish" to 2.2, "herring" to 0.8, "huchen" to 1.5, "cod" to 0.6, "eel" to 0.1, "burbot" to 0.2),
    ),
    BOILIE(
        L("Boiliai", "Boilies", "Boilies"), 900, 5,
        L("Tik dideli karpiai ir amūrai", "Nur große Karpfen und Graskarpfen", "Only big carp and grass carp"), 0.03,
        mapOf("carp" to 5.0, "grass_carp" to 3.0, "tench" to 1.5, "bream" to 1.2, "barbel" to 2.0, "catfish" to 0.6, "crucian" to 0.3),
    ),
    PILK(
        L("Pilkeris", "Pilker", "Pilk jig"), 1200, 6,
        L("Menkės jūros gilumoje", "Dorsche in der Tiefe des Meeres", "Cod in the deep sea"), 0.04,
        mapOf("cod" to 4.0, "turbot" to 1.5, "sea_trout" to 1.0, "garfish" to 0.5, "herring" to 0.5, "zander" to 0.8, "pike" to 0.8, "perch" to 0.8, "huchen" to 0.6),
    ),
    WOBBLER(
        L("Vobleris", "Wobbler", "Wobbler"), 1500, 7,
        L("Stambūs plėšrūnai", "Große Räuber", "Big predators"), 0.04,
        mapOf("pike" to 3.5, "zander" to 3.0, "asp" to 3.0, "trout" to 2.5, "sea_trout" to 3.0, "huchen" to 3.0, "catfish" to 1.5, "perch" to 1.5, "cod" to 1.0, "chub" to 1.5),
    ),
    LIVEBAIT(
        L("Gyva žuvelė", "Köderfisch", "Live bait"), 2500, 9,
        L("Šamai, vėgėlės, trofėjai", "Welse, Quappen, Trophäen", "Catfish, burbot, trophies"), 0.03,
        mapOf("catfish" to 4.0, "pike" to 3.0, "zander" to 3.0, "burbot" to 2.5, "eel" to 2.0, "huchen" to 3.5, "cod" to 1.5, "turbot" to 2.0),
    );

    override val key: String get() = "lure:$name"

    /** Rods this bait is fished on: spinning lures only on a spinning rod, boilies on the bottom. */
    val tackle: Set<Tackle> get() = when (this) {
        SPINNER, PILK, WOBBLER -> setOf(Tackle.SPIN)
        BOILIE -> setOf(Tackle.FEEDER)
        else -> setOf(Tackle.FLOAT, Tackle.FEEDER)
    }

    fun affinity(s: Species): Double = likes[s.id] ?: default

    /** Predator lures make bigger fish more likely. */
    val sizeBias: Double get() = when (this) {
        BOILIE, WOBBLER, LIVEBAIT -> 0.75
        SPINNER, PILK -> 0.9
        else -> 1.0
    }
}

/** A stronger rod survives harder pulls. */
enum class Rod(override val label: L, override val price: Int, override val level: Int, val strength: Double) : Item {
    BASIC(L("Bambukinė meškerė", "Bambusrute", "Bamboo rod"), 0, 1, 1.0),
    CARBON(L("Anglies pluošto meškerė", "Karbonrute", "Carbon rod"), 1500, 3, 1.1),
    PRO(L("Profesionalo meškerė", "Profi-Rute", "Pro rod"), 6000, 7, 1.2),
    LEGEND(L("Legendinė meškerė", "Legendäre Rute", "Legendary rod"), 20000, 12, 1.32);

    override val key: String get() = "rod:$name"
}

/** A faster reel brings the fish in sooner. */
enum class Reel(override val label: L, override val price: Int, override val level: Int, val speed: Double) : Item {
    BASIC(L("Paprasta ritė", "Einfache Rolle", "Basic reel"), 0, 1, 1.0),
    SMOOTH(L("Tyli ritė", "Laufruhige Rolle", "Smooth reel"), 1200, 3, 1.2),
    FAST(L("Greita ritė", "Schnelle Rolle", "Fast reel"), 5000, 6, 1.45),
    MASTER(L("Meistro ritė", "Meisterrolle", "Master reel"), 15000, 11, 1.7);

    override val key: String get() = "reel:$name"
}

/** Thicker line holds more, but shy fish notice it. */
enum class Line(override val label: L, override val price: Int, override val level: Int, val strength: Double, val shyFactor: Double) : Item {
    THIN(L("Plonas valas", "Dünne Schnur", "Thin line"), 0, 1, 0.0, 1.0),
    MEDIUM(L("Vidutinis valas", "Mittlere Schnur", "Medium line"), 600, 2, 0.06, 0.85),
    THICK(L("Storas valas", "Dicke Schnur", "Thick line"), 2000, 5, 0.13, 0.65);

    override val key: String get() = "line:$name"
}

/** Floats only change the look. */
enum class FloatStyle(override val label: L, override val price: Int, override val level: Int) : Item {
    CLASSIC(L("Raudona plūdė", "Rote Pose", "Red float"), 0, 1),
    FEATHER(L("Plunksnos plūdė", "Federkielpose", "Quill float"), 500, 2),
    GLOW(L("Švytinti plūdė", "Leuchtpose", "Glow float"), 1500, 4),
    GOLD(L("Auksinė plūdė", "Goldene Pose", "Golden float"), 8000, 10);

    override val key: String get() = "float:$name"
}

/** One-off upgrades. */
enum class Upgrade(override val label: L, override val price: Int, override val level: Int, val note: L) : Item {
    BOX_7(L("Didesnė masalų dėžė", "Größere Köderdose", "Bigger bait box"), 2000, 3,
        L("7 užmetimai vietoj 5", "7 Würfe statt 5", "7 casts instead of 5")),
    BOX_10(L("Didžiausia masalų dėžė", "Größte Köderdose", "Largest bait box"), 8000, 8,
        L("10 užmetimų", "10 Würfe", "10 casts")),
    BOAT(L("Valtis", "Boot", "Boat"), 10000, 8,
        L("Pasieki gilumą, kimba greičiau", "Erreicht die Tiefe, Bisse kommen schneller", "Reach deep water, bites come faster")),
    ALARM(L("Elektroninis signalizatorius", "Elektronischer Bissanzeiger", "Electronic bite alarm"), 3500, 7,
        L("Ant dugninės meškerės pypsi ir mirksi, kai žuvis liečia masalą", "Piept und blinkt an der Grundrute, wenn ein Fisch den Köder berührt", "Beeps and flashes on the bottom rod when a fish touches the bait"));

    override val key: String get() = "up:$name"
}

/** Everything the player fishes with right now. */
data class Gear(
    val lure: Lure = Lure.WORM,
    val tackle: Tackle = Tackle.FLOAT,
    val rod: Rod = Rod.BASIC,
    val reel: Reel = Reel.BASIC,
    val line: Line = Line.THIN,
    val boat: Boolean = false,
) {
    val strength: Double get() = rod.strength + line.strength
}

object Shop {
    val items: List<Item> = Tackle.entries + Lure.entries + Rod.entries + Reel.entries + Line.entries + FloatStyle.entries + Upgrade.entries
    val starter: Set<String> = setOf(Tackle.FLOAT.key, Lure.WORM.key, Rod.BASIC.key, Reel.BASIC.key, Line.THIN.key, FloatStyle.CLASSIC.key)
    fun byKey(key: String): Item? = items.firstOrNull { it.key == key }
}
