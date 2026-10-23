package lt.marcomultiservices.kimba.core

import java.time.Instant
import java.time.ZoneId

/**
 * Old Jonas, the angler of the home lake, teaches the player step by step. Each step is a task
 * counted from the moment he gives it; finishing it pays coins and some steps open a water
 * before its level, or tell the legends' secrets all at once.
 */
enum class StoryStep(val talk: L, val task: L, val target: Int, val coins: Int, val opens: Water? = null, val revealsClues: Boolean = false) {
    ROACH(
        L("Sveikas, jaunasis žvejy! Aš Jonas, prie šito ežero žvejoju penkiasdešimt metų. Parodyk, ką moki.", "Servus, junger Angler! Ich bin Jonas und angle seit fünfzig Jahren an diesem See. Zeig mal, was du kannst.", "Hello, young angler! I'm Jonas, I've fished this lake for fifty years. Show me what you can do."),
        L("Sugauk 3 kuojas", "Fange 3 Rotaugen", "Catch 3 roach"), 3, 150,
    ),
    PERCH(
        L("Ešerys – dryžuotas plėšrūnas. Sliekas jam patinka labiau už viską.", "Der Barsch ist ein gestreifter Räuber. Einen Wurm mag er lieber als alles andere.", "The perch is a striped hunter. It likes a worm more than anything."),
        L("Sugauk bent 200 g ešerį", "Fange einen Barsch ab 200 g", "Catch a perch of 200 g or more"), 200, 200,
    ),
    WEATHER(
        L("Kai lyja ar rūkas, žuvys drąsesnės. Tikras žvejys oro nebijo.", "Bei Regen oder Nebel werden Fische mutiger. Ein echter Angler fürchtet kein Wetter.", "In rain or fog fish grow bolder. A real angler isn't afraid of weather."),
        L("Sugauk žuvį per tikrą lietų arba rūką", "Fange einen Fisch bei echtem Regen oder Nebel", "Catch a fish in real rain or fog"), 1, 250,
    ),
    BUSY_DAY(
        L("Geras žvejys žino, kada žuvis kimba. Pasirink gerą dieną ir neskubėk namo.", "Ein guter Angler weiß, wann die Fische beißen. Such dir einen guten Tag aus und eil nicht nach Hause.", "A good angler knows when the fish bite. Pick a good day and don't hurry home."),
        L("Sugauk 5 žuvis per vieną dieną", "Fange 5 Fische an einem Tag", "Catch 5 fish in one day"), 5, 250, opens = Water.RIVER,
    ),
    RIVER(
        L("Parodysiu tau upę už miško. Srovė ten stipri, bet ir žuvys kitokios.", "Ich zeig dir den Fluss hinter dem Wald. Die Strömung ist stark, aber die Fische sind anders.", "I'll show you the river beyond the woods. The current is strong, but the fish are different."),
        L("Sugauk 3 žuvis upėje", "Fange 3 Fische im Fluss", "Catch 3 fish in the river"), 3, 300, revealsClues = true,
    ),
    PIKE(
        L("Dabar žinai legendų paslaptis. Bet pirma sugauk tikrą lydeką, kad ranka priprastų. Sliekas jai mažai rūpi – imk spiningą su blizge.", "Jetzt kennst du die Geheimnisse der Legenden. Aber fang erst einen richtigen Hecht, damit die Hand sich gewöhnt. Ein Wurm reizt ihn kaum, nimm die Spinnrute mit dem Blinker.", "Now you know the legends' secrets. But first catch a real pike, so your hand gets used to it. A worm hardly tempts it: take the spinning rod and a spinner."),
        L("Sugauk bent 2 kg lydeką", "Fange einen Hecht ab 2 kg", "Catch a pike of 2 kg or more"), 2000, 400,
    ),
    RELEASE(
        L("Mažylius paleisk – užaugs. Taip mano tėvas mokė, taip ir aš tave moku.", "Lass die Kleinen frei, sie wachsen nach. So hat es mein Vater mir beigebracht, so bringe ich es dir bei.", "Let the little ones go, they'll grow. My father taught me so, and so I teach you."),
        L("Surink 3 gero žvejo taškus paleisdamas žuvis", "Sammle 3 Fairness-Punkte durch Zurücksetzen", "Earn 3 good-angler points by releasing fish"), 3, 400, opens = Water.SEA,
    ),
    SEA(
        L("Jūra – visai kitas pasaulis. Menkės ir strimėlės, vėjas ir bangos.", "Das Meer ist eine ganz andere Welt. Dorsch und Hering, Wind und Wellen.", "The sea is another world. Cod and herring, wind and waves."),
        L("Sugauk 3 žuvis jūroje", "Fange 3 Fische im Meer", "Catch 3 fish in the sea"), 3, 500,
    ),
    SPECIES(
        L("Kuo daugiau rūšių pažinsi, tuo geriau suprasi vandenį.", "Je mehr Arten du kennst, desto besser verstehst du das Wasser.", "The more kinds you know, the better you understand the water."),
        L("Sugauk 5 skirtingas rūšis", "Fange 5 verschiedene Arten", "Catch 5 different species"), 5, 600,
    ),
    LEGEND(
        L("Paskutinis mano prašymas. Pagauk bent vieną legendą, ir būsi tikras šio krašto žvejys.", "Meine letzte Bitte. Fang wenigstens eine Legende, dann bist du ein echter Angler dieser Gegend.", "My last wish. Catch at least one legend, and you'll be a true angler of these parts."),
        L("Sugauk legendinę žuvį", "Fange einen legendären Fisch", "Catch a legendary fish"), 1, 3000,
    );

    /** Progress towards the task, from the catches since it was given and the player's state. */
    fun progress(p: Progress, since: List<Catch>, zone: ZoneId): Int = when (this) {
        ROACH -> since.count { it.species == "roach" }
        PERCH -> since.filter { it.species == "perch" }.maxOfOrNull { it.grams } ?: 0
        WEATHER -> since.count { it.sky in setOf(Sky.DRIZZLE, Sky.RAIN, Sky.HEAVY_RAIN, Sky.THUNDER, Sky.FOG) }
        BUSY_DAY -> since.groupBy { Instant.ofEpochMilli(it.epochMillis).atZone(zone).toLocalDate() }.values.maxOfOrNull { it.size } ?: 0
        RIVER -> since.count { it.water == Water.RIVER }
        PIKE -> since.filter { it.species == "pike" && it.golden == null }.maxOfOrNull { it.grams } ?: 0
        RELEASE -> p.goodAngler - p.storyAngler
        SEA -> since.count { it.water == Water.SEA }
        SPECIES -> since.map { it.species }.toSet().size
        LEGEND -> since.count { it.golden?.startsWith("legend_") == true }
    }
}

object Story {
    /** The step Jonas is on, or null when he has taught everything. */
    fun current(p: Progress): StoryStep? = StoryStep.entries.getOrNull(p.story)

    fun since(p: Progress, catches: List<Catch>): List<Catch> = catches.filter { it.epochMillis >= p.storySince }

    /** Finishes the current step if its task is done: pays, opens, moves on. Null if not done. */
    fun advance(p: Progress, catches: List<Catch>, now: Long, zone: ZoneId): Pair<Progress, StoryStep>? {
        val step = current(p) ?: return null
        if (step.progress(p, since(p, catches), zone) < step.target) return null
        return p.copy(
            coins = p.coins + step.coins,
            story = p.story + 1,
            storySince = now,
            storyAngler = p.goodAngler,
            jonasWaters = p.jonasWaters + listOfNotNull(step.opens),
            allClues = p.allClues || step.revealsClues,
        ) to step
    }
}
