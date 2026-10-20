package lt.marcomultiservices.kimba.core

/** Kinds of water, each with its own fish and scene. Home waters open with the player's level. */
enum class Water(val label: L, val homeLevel: Int) {
    LAKE(L("Ežeras", "See", "Lake"), 1),
    POND(L("Tvenkinys", "Teich", "Pond"), 2),
    RIVER(L("Upė", "Fluss", "River"), 4),
    SEA(L("Jūra", "Meer", "Sea"), 6),
}

/** Where the player is fishing: a kind of water, at home or at a trip destination. */
data class Spot(val water: Water, val trip: String? = null) {
    companion object {
        val HOME_LAKE = Spot(Water.LAKE)
    }
}

/** A famous fishing place on the map, fished with its own real weather. */
class Destination(
    val id: String,
    val name: L,
    val country: String,
    val lat: Double,
    val lon: Double,
    val water: Water,
    val level: Int,
    val story: L,
) {
    val price: Int get() = 500 * level
    val spot: Spot get() = Spot(water, id)
    /** Species found only here. */
    val specials: List<Species> get() = Fish.all.filter { it.onlyAt?.contains(id) == true }
}

object Destinations {
    val all = listOf(
        Destination("nemunas", L("Nemunas ties Kaunu", "Memel bei Kaunas", "Neman at Kaunas"), "LT", 54.90, 23.90, Water.RIVER, 3,
            L("Didžiausia Lietuvos upė. Gegužę čia kyla žiobriai.", "Litauens größter Fluss. Im Mai steigen hier die Zährten auf.", "Lithuania's largest river. Vimba run up it in May.")),
        Destination("curonian", L("Kuršių marios, Nida", "Kurisches Haff, Nida", "Curonian Lagoon, Nida"), "LT", 55.30, 21.01, Water.LAKE, 4,
            L("Platus gėlas vanduo už kopų. Sterkai, karšiai ir žiobriai.", "Weites Süßwasser hinter den Dünen. Zander, Brassen und Zährten.", "Wide fresh water behind the dunes. Zander, bream and vimba.")),
        Destination("palanga", L("Palangos tiltas", "Seebrücke Palanga", "Palanga pier"), "LT", 55.92, 21.05, Water.SEA, 5,
            L("Baltijos jūra nuo garsiojo tilto: strimėlės ir plekšnės.", "Ostsee von der berühmten Seebrücke: Hering und Flunder.", "The Baltic from the famous pier: herring and flounder.")),
        Destination("mueritz", L("Müritz ežeras", "Müritz", "Lake Müritz"), "DE", 53.42, 12.70, Water.LAKE, 6,
            L("Didžiausias Vokietijos vidaus ežeras. Čia gyvena sykai.", "Größter Binnensee Deutschlands. Hier leben Maränen und Felchen.", "Germany's largest inland lake, home of whitefish.")),
        Destination("ruegen", L("Riugeno sala", "Rügen", "Rügen island"), "DE", 54.42, 13.43, Water.SEA, 8,
            L("Kreidos uolos ir menkių žvejyba Baltijoje.", "Kreidefelsen und Dorschangeln in der Ostsee.", "Chalk cliffs and cod fishing in the Baltic.")),
        Destination("elbe", L("Elbė ties Drezdenu", "Elbe bei Dresden", "Elbe at Dresden"), "DE", 51.05, 13.74, Water.RIVER, 9,
            L("Plati upė su salačiais ir ūsoriais.", "Breiter Strom mit Rapfen und Barben.", "A broad river with asp and barbel.")),
        Destination("bodensee", L("Bodeno ežeras", "Bodensee", "Lake Constance"), "DE", 47.63, 9.37, Water.LAKE, 10,
            L("Alpių papėdės ežeras, garsus savo sykais.", "Voralpensee, berühmt für seine Felchen.", "An alpine-foothill lake famous for whitefish.")),
        Destination("wachau", L("Dunojus, Vachau", "Donau, Wachau", "Danube, Wachau"), "AT", 48.37, 15.42, Water.RIVER, 12,
            L("Vynuogynų slėnis. Čia slepiasi legendinė Dunojaus lašiša.", "Weinbautal. Hier lauert der legendäre Huchen.", "A wine valley where the legendary huchen lurks.")),
        Destination("woerthersee", L("Vėrterio ežeras", "Wörthersee", "Lake Wörth"), "AT", 46.62, 14.15, Water.LAKE, 14,
            L("Šilčiausias Alpių ežeras: karpiai ir lydekos.", "Der wärmste Alpensee: Karpfen und Hechte.", "The warmest alpine lake: carp and pike.")),
        Destination("loreley", L("Reinas ties Lorelei", "Rhein an der Loreley", "Rhine at the Loreley"), "DE", 50.14, 7.73, Water.RIVER, 15,
            L("Legendų uola virš srauniausios Reino vietos.", "Der Sagenfelsen über der wildesten Stelle des Rheins.", "The legendary rock above the Rhine's wildest stretch.")),
    )

    fun byId(id: String?): Destination? = all.firstOrNull { it.id == id }
}
