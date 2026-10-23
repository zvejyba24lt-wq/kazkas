package lt.marcomultiservices.kimba.core

/**
 * The angler's own camp by the home water, built up piece by piece with coins. Each piece is
 * drawn in the camp scene and makes the camp cosier: every morning the camp pays a few coins
 * for each piece, so the coins spent come back slowly.
 */
enum class CampItem(val label: L, val price: Int, val level: Int, val note: L) {
    FIRE(L("Laužavietė", "Feuerstelle", "Campfire"), 400, 1, L("Šildo ir šviečia vakarais", "Wärmt und leuchtet am Abend", "Warms and glows in the evening")),
    CHAIR(L("Žvejo kėdė", "Anglerstuhl", "Angler's chair"), 600, 2, L("Patogiai laukti kibimo", "Bequem auf den Biss warten", "Wait for the bite in comfort")),
    TENT(L("Palapinė", "Zelt", "Tent"), 1500, 3, L("Nakvynė prie vandens", "Übernachten am Wasser", "A night by the water")),
    LANTERN(L("Žibintas", "Laterne", "Lantern"), 900, 4, L("Šviesa naktinei žvejybai", "Licht fürs Nachtangeln", "Light for night fishing")),
    DOG(L("Šuo", "Hund", "Dog"), 3000, 5, L("Ištikimas žvejybos draugas", "Treuer Angelfreund", "A faithful fishing friend")),
    SMOKER(L("Rūkykla", "Räucherofen", "Smoker"), 4000, 6, L("Rūkyta žuvis kvepia visam ežerui", "Räucherfisch duftet über den See", "Smoked fish scents the whole lake")),
    FLAG(L("Vėliava", "Fahne", "Flag"), 1200, 7, L("Rodo vėjo kryptį", "Zeigt den Wind", "Shows the wind")),
    CABIN(L("Žvejo namelis", "Anglerhütte", "Angler's cabin"), 15000, 10, L("Tikra žvejo svajonė", "Der Traum jedes Anglers", "Every angler's dream"));

    val key: String get() = "camp:$name"

    companion object {
        /** Coins the camp pays each morning for every piece built. */
        const val DAILY_PER_ITEM = 20
    }
}

fun Progress.ownsCamp(item: CampItem): Boolean = item.key in owned

fun Progress.campCount(): Int = CampItem.entries.count { ownsCamp(it) }

/** Buys a camp piece; null if owned already, locked or too dear. */
fun Progress.buyCamp(item: CampItem): Progress? {
    if (ownsCamp(item) || level < item.level || coins < item.price) return null
    return copy(coins = coins - item.price, owned = owned + item.key)
}
