package lt.marcomultiservices.kimba.core

/** Long-term goals. Each is earned once and pays coins; the weather ones need the real weather. */
enum class Achievement(val title: L, val text: L, val target: Int, val coins: Int) {
    FIRST_FISH(L("Pirmoji žuvis", "Der erste Fisch", "First fish"), L("Sugauk pirmą žuvį", "Fange deinen ersten Fisch", "Catch your first fish"), 1, 50),
    FISH_25(L("Žvejys", "Angler", "Angler"), L("Sugauk 25 žuvis", "Fange 25 Fische", "Catch 25 fish"), 25, 200),
    FISH_100(L("Patyręs žvejys", "Erfahrener Angler", "Seasoned angler"), L("Sugauk 100 žuvų", "Fange 100 Fische", "Catch 100 fish"), 100, 600),
    FISH_500(L("Ežerų legenda", "Legende der Seen", "Legend of the lakes"), L("Sugauk 500 žuvų", "Fange 500 Fische", "Catch 500 fish"), 500, 2500),
    SPECIES_5(L("Smalsuolis", "Neugierig", "Curious"), L("Sugauk 5 skirtingas rūšis", "Fange 5 verschiedene Arten", "Catch 5 different species"), 5, 250),
    SPECIES_12(L("Gamtininkas", "Naturkundler", "Naturalist"), L("Sugauk 12 skirtingų rūšių", "Fange 12 verschiedene Arten", "Catch 12 different species"), 12, 800),
    SPECIES_ALL(L("Visa knyga", "Das ganze Buch", "The whole book"), L("Sugauk visas rūšis", "Fange alle Arten", "Catch every species"), Fish.all.size, 5000),
    KILO(L("Kilogramas", "Ein Kilo", "One kilo"), L("Sugauk žuvį, sveriančią bent 1 kg", "Fange einen Fisch ab 1 kg", "Catch a fish of 1 kg or more"), 1000, 150),
    FIVE_KILO(L("Stambi žuvis", "Großfisch", "Big one"), L("Sugauk žuvį, sveriančią bent 5 kg", "Fange einen Fisch ab 5 kg", "Catch a fish of 5 kg or more"), 5000, 700),
    TEN_KILO(L("Pabaisa", "Monster", "Monster"), L("Sugauk žuvį, sveriančią bent 10 kg", "Fange einen Fisch ab 10 kg", "Catch a fish of 10 kg or more"), 10000, 2000),
    GOLDEN(L("Aukso blizgesys", "Goldglanz", "Golden glint"), L("Sugauk auksinę žuvį", "Fange einen goldenen Fisch", "Catch a golden fish"), 1, 1000),
    ALL_WATERS(L("Visi vandenys", "Alle Gewässer", "Every water"), L("Pažvejok ežere, tvenkinyje, upėje ir jūroje", "Fange im See, Teich, Fluss und Meer", "Catch fish in a lake, pond, river and the sea"), Water.entries.size, 800),
    RAIN(L("Lietaus žvejys", "Regenangler", "Rain angler"), L("Sugauk žuvį, kai pas tave tikrai lyja", "Fange einen Fisch, während es bei dir wirklich regnet", "Catch a fish while it really rains where you are"), 1, 200),
    STORM(L("Perkūno draugas", "Gewitterfreund", "Storm friend"), L("Sugauk žuvį per tikrą perkūniją", "Fange einen Fisch bei echtem Gewitter", "Catch a fish during a real thunderstorm"), 1, 600),
    FOG(L("Rūko žvejys", "Nebelangler", "Fog angler"), L("Sugauk žuvį per tikrą rūką", "Fange einen Fisch bei echtem Nebel", "Catch a fish in real fog"), 1, 400),
    SNOW(L("Žiemos žvejys", "Winterangler", "Winter angler"), L("Sugauk žuvį, kai tikrai sninga", "Fange einen Fisch, während es wirklich schneit", "Catch a fish while it really snows"), 1, 400),
    TRAVELER(L("Keliautojas", "Reisender", "Traveller"), L("Aplankyk 3 žvejybos vietas žemėlapyje", "Besuche 3 Angelorte auf der Karte", "Visit 3 places on the map"), 3, 500),
    STREAK_7(L("Ištikimas", "Treu", "Faithful"), L("Žvejok 7 dienas iš eilės", "Angle 7 Tage am Stück", "Fish 7 days in a row"), 7, 500),
    LEVEL_10(L("Meistras", "Meister", "Master"), L("Pasiek 10 lygį", "Erreiche Level 10", "Reach level 10"), 10, 1500);

    val key: String get() = name.lowercase()

    /** How far the player is, 0..[target]. */
    fun progress(p: Progress, catches: List<Catch>): Int {
        val v = when (this) {
            FIRST_FISH, FISH_25, FISH_100, FISH_500 -> maxOf(p.caught, catches.size)
            SPECIES_5, SPECIES_12, SPECIES_ALL -> catches.map { it.species }.distinct().size
            KILO, FIVE_KILO, TEN_KILO -> if ((catches.maxOfOrNull { it.grams } ?: 0) >= target) target else 0
            GOLDEN -> if (catches.any { it.golden != null }) 1 else 0
            ALL_WATERS -> catches.map { it.water }.distinct().size
            RAIN -> catches.count { it.sky in Achievements.rainy }
            STORM -> catches.count { it.sky == Sky.THUNDER }
            FOG -> catches.count { it.sky == Sky.FOG }
            SNOW -> catches.count { it.sky == Sky.SNOW || it.sky == Sky.SLEET }
            TRAVELER -> p.trips.size
            STREAK_7 -> p.streak
            LEVEL_10 -> p.level
        }
        return v.coerceIn(0, target)
    }
}

object Achievements {
    val rainy = setOf(Sky.DRIZZLE, Sky.RAIN, Sky.HEAVY_RAIN, Sky.THUNDER)

    /** Marks the goals [p] has just reached and pays their coins. */
    fun check(p: Progress, catches: List<Catch>): Pair<Progress, List<Achievement>> {
        val new = Achievement.entries.filter { it.key !in p.achieved && it.progress(p, catches) >= it.target }
        if (new.isEmpty()) return p to emptyList()
        return p.copy(coins = p.coins + new.sumOf { it.coins }, achieved = p.achieved + new.map { it.key }) to new
    }
}

/** The next thing the player's level opens: home waters, trips and shop items. */
object Unlocks {
    class Next(val level: Int, val names: List<L>)

    fun next(level: Int): Next? {
        val waters = Water.entries.map { it.homeLevel to it.label }
        val trips = Destinations.all.map { it.level to it.name }
        val items = Shop.items.map { it.level to it.label }
        val all = waters + trips + items
        val l = all.map { it.first }.filter { it > level }.minOrNull() ?: return null
        // Waters and trips first: they matter most to the player.
        return Next(l, all.filter { it.first == l }.map { it.second })
    }
}
