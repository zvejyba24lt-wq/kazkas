package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Coins, experience, owned gear and trips. Everything is earned by fishing. */
@Serializable
data class Progress(
    val coins: Int = 150,
    val xp: Int = 0,
    val owned: Set<String> = Shop.starter,
    val lure: Lure = Lure.WORM,
    val tackle: Tackle = Tackle.FLOAT,
    val rod: Rod = Rod.BASIC,
    val reel: Reel = Reel.BASIC,
    val line: Line = Line.THIN,
    val float: FloatStyle = FloatStyle.CLASSIC,
    val trips: Set<String> = emptySet(),
    val water: Water = Water.LAKE,
    /** Trip destination being fished, or null at home. */
    val trip: String? = null,
    val streak: Int = 0,
    val lastDay: Long = 0,
    val quests: Quests? = null,
    val noAds: Boolean = false,
    /** Fish landed in total (the catch log is capped, this is not). */
    val caught: Int = 0,
    /** Keys of the [Achievement]s already earned. */
    val achieved: Set<String> = emptySet(),
    /** Fish put back that may bite again ([Release]). */
    val released: List<Released> = emptyList(),
    /** Good-angler points from released fish. */
    val goodAngler: Int = 0,
    /** How many of each [Surprise] came up. */
    val surprises: Map<String, Int> = emptyMap(),
    /** Old Jonas: the step he is on, when it was given, good-angler points then. */
    val story: Int = 0,
    val storySince: Long = 0,
    val storyAngler: Int = 0,
    /** Waters Jonas has shown before their level. */
    val jonasWaters: Set<Water> = emptySet(),
    /** Jonas told every legend's clues at once. */
    val allClues: Boolean = false,
    /** Friends' shared cards: the weekly table and their camps to visit. */
    val friendCards: List<FriendCard> = emptyList(),
    /** This player's friend code (see [Invites]); made on first start. */
    val inviteCode: String = "",
    /** The friend code this player joined with, once. */
    val invitedBy: String? = null,
    /** Newcomers whose thank-you codes were redeemed. */
    val thanked: Set<String> = emptySet(),
    /** Epoch day of the last share that paid casts. */
    val shareDay: Long = 0,
    /** Ice on the home waters, from the real temperatures (see [Ice]). */
    val ice: IceLog = IceLog(),
    /** Where the player stands at home (see [Stand]). */
    val stand: Stand = Stand.PIER,
    /** Today's best for the daily challenge (see [Challenge]). */
    val challenge: ChallengeLog = ChallengeLog(),
    /** Epoch day the legends' clues started to come, one a day. */
    val cluesFrom: Long = 0,
) {
    /** Clues of each legend known on [day]: one the first day, all three after two more. */
    fun clues(day: Long): Int = if (allClues) 3 else if (cluesFrom == 0L) 1 else (1 + day - cluesFrom).toInt().coerceIn(1, 3)

    /** A home water is open by level, or once Jonas has shown it. */
    fun opened(w: Water): Boolean = level >= w.homeLevel || w in jonasWaters

    val level: Int get() = Levels.levelOf(xp)
    val gear: Gear get() = Gear(bait, tackle, rod, reel, line, boat = standHere == Stand.BOAT, stand = standHere)

    /** The stand in use: trips are fished from the shore, the boat only once bought. */
    val standHere: Stand get() = when {
        trip != null -> Stand.PIER
        stand == Stand.BOAT && Upgrade.BOAT.key !in owned -> Stand.PIER
        !stand.at(water) -> Stand.PIER
        else -> stand
    }

    /** At home on frozen water the player fishes through the ice with a short winter float rod. */
    val onIce: Boolean get() = trip == null && Ice.frozen(water, ice)

    /** The gear actually fished with here: on the ice a float rod with a bait that fits it. */
    /**
     * The gear actually fished. Through the ice it is a short winter rod: a mormyshka jig under a
     * nod tipped with maggots or a worm, or, with a spinning rod and a spoon, a vertical spoon
     * jigged in the hole for perch and pike. Feeders, corn and boilies stay ashore.
     */
    val gearHere: Gear get() = when {
        !onIce -> gear
        tackle == Tackle.SPIN && owns(Tackle.SPIN) && owns(Lure.SPINNER) -> Gear(Lure.SPINNER, Tackle.SPIN, rod, reel, line, boat = false, ice = true)
        else -> Gear(if (lure in ICE_BAITS && owns(lure)) lure else Lure.WORM, Tackle.FLOAT, rod, reel, line, boat = false, ice = true)
    }

    /** The bait actually on the hook: the chosen one if it fits the rod, else the first owned one that does. */
    val bait: Lure get() = if (tackle in lure.tackle) lure else baitsFor(tackle).firstOrNull() ?: Lure.WORM

    /** Owned baits that can be fished on [t]. */
    fun baitsFor(t: Tackle): List<Lure> = Lure.entries.filter { owns(it) && t in it.tackle }

    /** Older saves: spinning lures bought before rods had kinds come with a spinning rod. */
    fun migrated(): Progress {
        if (owns(Tackle.FLOAT) && (owns(Tackle.SPIN) || Lure.entries.none { owns(it) && it.tackle == setOf(Tackle.SPIN) })) return this
        val spin = Lure.entries.any { owns(it) && it.tackle == setOf(Tackle.SPIN) }
        return copy(
            owned = owned + Tackle.FLOAT.key + (if (spin) listOf(Tackle.SPIN.key) else emptyList()),
            tackle = if (spin && lure.tackle == setOf(Tackle.SPIN)) Tackle.SPIN else tackle,
        )
    }
    val boxSize: Int get() = when {
        Upgrade.BOX_10.key in owned -> 20
        Upgrade.BOX_7.key in owned -> 15
        else -> Baits.MAX
    }
    val spot: Spot get() = Destinations.byId(trip)?.spot ?: Spot(water)

    fun owns(item: Item): Boolean = item.key in owned
    fun canBuy(item: Item): Boolean = !owns(item) && level >= item.level && coins >= item.price

    fun buy(item: Item): Progress? {
        if (!canBuy(item)) return null
        // A spinning rod comes with a spinner, so it can be used straight away.
        val extra = if (item == Tackle.SPIN) listOf(Lure.SPINNER.key) else emptyList()
        val p = copy(coins = coins - item.price, owned = owned + item.key + extra)
        return p.equip(item)
    }

    fun equip(item: Item): Progress {
        if (!owns(item)) return this
        return when (item) {
            // A bait for another kind of rod switches to that rod if the player has one.
            is Lure -> when {
                tackle in item.tackle -> copy(lure = item)
                else -> item.tackle.firstOrNull { owns(it) }?.let { copy(tackle = it, lure = item) } ?: this
            }
            is Tackle -> copy(tackle = item, lure = if (item in lure.tackle) lure else baitsFor(item).firstOrNull() ?: lure)
            is Rod -> copy(rod = item)
            is Reel -> copy(reel = item)
            is Line -> copy(line = item)
            is FloatStyle -> copy(float = item)
            else -> this
        }
    }

    fun canTravel(d: Destination): Boolean = d.id in trips || (level >= d.level && coins >= d.price)

    /** Goes to a destination, paying for it the first time; later visits are free. */
    fun travel(d: Destination): Progress? = when {
        d.id in trips -> copy(trip = d.id)
        level >= d.level && coins >= d.price -> copy(coins = coins - d.price, trips = trips + d.id, trip = d.id)
        else -> null
    }

    fun goHome(water: Water): Progress = if (opened(water)) copy(trip = null, water = water) else copy(trip = null)
}

/** Baits for the mormyshka through the ice. */
val ICE_BAITS = setOf(Lure.MAGGOT, Lure.WORM)

object Levels {
    /** Total XP needed to reach [level]. */
    fun xpFor(level: Int): Int = 50 * (level - 1) * level

    fun levelOf(xp: Int): Int {
        var l = 1
        while (xpFor(l + 1) <= xp) l++
        return l
    }
}

/** What a landed fish paid; [challenge] is set when it is the best of the day for the daily challenge. */
class Reward(val coins: Int, val xp: Int, val levelUp: Int?, val quests: List<Quest>, val allQuests: Boolean, val challenge: Boolean = false, val storm: Boolean = false)

object Economy {
    const val ALL_QUESTS_COINS = 300
    const val ALL_QUESTS_CASTS = 3

    fun coins(species: Species, golden: Golden?, grams: Int): Int {
        val base = 5 + species.price * grams / 1000.0
        return (base * (if (golden != null) 5 else 1)).toInt()
    }

    fun xp(species: Species, golden: Golden?, isNew: Boolean): Int {
        val rarity = ((1.0 - species.abundance).coerceAtLeast(0.0) * 20).toInt()
        return 8 + rarity + (if (golden != null) 100 else 0) + (if (isNew) 50 else 0)
    }

    /**
     * What the fish itself pays: its price, double for the kind biting best right now ([hot]),
     * and double again in a real thunderstorm. This is the amount on the catch card, the one a
     * rewarded ad pays once more and the one given back when the fish is released.
     */
    fun fishCoins(species: Species, golden: Golden?, grams: Int, hot: Boolean, sky: Sky): Int =
        coins(species, golden, grams) * (if (hot) 2 else 1) * (if (sky == Sky.THUNDER) Storm.BONUS else 1)

    /** Applies a landed fish to [p]: coins, XP and quest progress. */
    fun land(p: Progress, catch: Catch, isNew: Boolean, epochDay: Long): Pair<Progress, Reward> {
        val species = Fish.byId(catch.species) ?: return p to Reward(0, 0, null, emptyList(), false)
        val golden = catch.golden?.let { Fish.goldenById(it) }
        // A fish from a real thunderstorm pays double.
        val storm = catch.sky == Sky.THUNDER
        // The catch carries its own price (with the bonuses it earned); older callers leave it 0.
        var coins = if (catch.coins > 0) catch.coins else fishCoins(species, golden, catch.grams, false, catch.sky)
        val xp = xp(species, golden, isNew)
        val quests = Quests.today(p.quests, epochDay, p.level)
        val (updated, done) = quests.record(catch)
        coins += done.sumOf { it.coins }
        val all = done.isNotEmpty() && updated.allDone
        if (all) coins += ALL_QUESTS_COINS
        // The daily challenge: a better fish of the day's kind is kept, the first one pays.
        val month = java.time.LocalDate.ofEpochDay(epochDay).monthValue
        val challenge = Challenge.record(p.challenge, epochDay, month, catch)
        if (challenge != null && p.challenge.day != epochDay) coins += Challenge.COINS
        val before = p.level
        val next = p.copy(coins = p.coins + coins, xp = p.xp + xp, quests = updated, caught = p.caught + 1, challenge = challenge ?: p.challenge)
        return next to Reward(coins, xp, next.level.takeIf { it > before }, done, all, challenge = challenge != null, storm = storm)
    }
}

/** Kinds of daily quest. */
enum class QuestKind { COUNT, SPECIES, BIG, TOTAL_KG, GOLDEN_OR_RARE, WATER }

@Serializable
data class Quest(val kind: QuestKind, val target: Int, val species: String? = null, val water: Water? = null, val coins: Int, val progress: Int = 0) {
    val done: Boolean get() = progress >= target

    fun text(lang: Lang): String {
        val name = species?.let { Fish.byId(it)?.name?.get(lang) } ?: ""
        return when (kind) {
            QuestKind.COUNT -> L("Sugauk ${Plural.fishLt(target)}", "Fange $target Fische", "Catch $target fish")[lang]
            QuestKind.SPECIES -> {
                val many = species?.let { Fish.plural(it)?.get(lang) } ?: name
                L("Sugauk $target $many", "Fange $target $many", "Catch $target $many")[lang]
            }
            QuestKind.BIG -> {
                val kg = Plural.kg(target, lang)
                L("Sugauk žuvį, sveriančią bent $kg", "Fange einen Fisch ab $kg", "Catch a fish of $kg or more")[lang]
            }
            QuestKind.TOTAL_KG -> L("Sugauk iš viso ${target / 1000} kg žuvies", "Fange insgesamt ${target / 1000} kg Fisch", "Catch ${target / 1000} kg of fish in total")[lang]
            QuestKind.GOLDEN_OR_RARE -> L("Sugauk retą žuvį (pvz. lyną, sterką ar auksinę)", "Fange einen seltenen Fisch (z. B. Schleie, Zander oder golden)", "Catch a rare fish (e.g. tench, zander or golden)")[lang]
            QuestKind.WATER -> L("Sugauk ${Plural.fishLt(target)} ${Plural.inWaterLt(water)}", "Fange $target Fische ${Plural.inWaterDe(water)}", "Catch $target fish in ${Plural.inWaterEn(water)}")[lang]
        }
    }

    /** Progress shown to the player, e.g. "3/5" or "1.2/3 kg". */
    fun progressText(): String = when (kind) {
        QuestKind.BIG -> if (done) "✓" else "0/1"
        QuestKind.TOTAL_KG -> "%.1f/%d kg".format(java.util.Locale.GERMANY, progress.coerceAtMost(target) / 1000.0, target / 1000)
        else -> "${progress.coerceAtMost(target)}/$target"
    }

    fun record(c: Catch): Quest {
        if (done) return this
        val s = Fish.byId(c.species)
        val add = when (kind) {
            QuestKind.COUNT -> 1
            QuestKind.SPECIES -> if (c.species == species) 1 else 0
            QuestKind.BIG -> if (c.grams >= target) target else 0
            QuestKind.TOTAL_KG -> c.grams
            QuestKind.GOLDEN_OR_RARE -> if (c.golden != null || (s != null && s.abundance <= 0.4)) 1 else 0
            QuestKind.WATER -> if (c.water == water) 1 else 0
        }
        return copy(progress = progress + add)
    }
}

/** Three quests a day, the same for everyone on that date. */
@Serializable
data class Quests(val day: Long, val list: List<Quest>) {
    val allDone: Boolean get() = list.all { it.done }

    /** Records a catch; returns the new state and the quests it just completed. */
    fun record(c: Catch): Pair<Quests, List<Quest>> {
        val next = list.map { it.record(c) }
        val done = next.filterIndexed { i, q -> q.done && !list[i].done }
        return Quests(day, next) to done
    }

    companion object {
        /** Waters open at home for a player of [level]. */
        fun watersFor(level: Int): List<Water> = Water.entries.filter { it.homeLevel <= level }

        /**
         * Common, widespread fish of the waters open to the player that bite often enough in
         * [month] to catch two or three: bream only in the warm half of the year, bleak (small
         * and choosy) only in high summer.
         */
        fun questSpecies(level: Int, month: Int = 7): List<Species> = Fish.all.filter { s ->
            s.onlyAt == null && s.abundance >= 0.6 && s.waters.any { it in watersFor(level) } && when (s.id) {
                "bleak" -> month in 6..8
                "bream" -> month in 5..9
                else -> true
            }
        }.ifEmpty { listOf(Fish.roach, Fish.perch) }

        fun today(current: Quests?, epochDay: Long, level: Int = 1): Quests =
            if (current != null && current.day == epochDay) current else generate(epochDay, level)

        /** The day decides the quests; the player's level only limits them to fish and waters already open. */
        fun generate(epochDay: Long, level: Int = 1): Quests {
            val rng = Random(epochDay * 7919 + 17)
            val kinds = listOf(QuestKind.SPECIES, QuestKind.BIG, QuestKind.TOTAL_KG, QuestKind.GOLDEN_OR_RARE, QuestKind.WATER).shuffled(rng).take(2)
            val species = questSpecies(level, java.time.LocalDate.ofEpochDay(epochDay).monthValue)
            val waters = watersFor(level)
            val list = listOf(Quest(QuestKind.COUNT, 5 + rng.nextInt(4), coins = 100)) + kinds.map { k ->
                when (k) {
                    QuestKind.SPECIES -> Quest(k, 2 + rng.nextInt(2), species = species[rng.nextInt(species.size)].id, coins = 150)
                    QuestKind.BIG -> Quest(k, listOf(800, 1000, 1500)[rng.nextInt(3)], coins = 200)
                    QuestKind.TOTAL_KG -> Quest(k, 2000 + 1000 * rng.nextInt(2), coins = 150)
                    QuestKind.GOLDEN_OR_RARE -> Quest(k, 1, coins = 200)
                    else -> Quest(QuestKind.WATER, 3, water = waters[rng.nextInt(waters.size)], coins = 120)
                }
            }
            return Quests(epochDay, list)
        }
    }
}

/** Daily return bonus. */
object Streak {
    class Bonus(val day: Int, val coins: Int, val casts: Int)

    /** Updates the streak on opening the game; returns the bonus if this is the first visit today. */
    fun visit(p: Progress, epochDay: Long): Pair<Progress, Bonus?> {
        if (p.lastDay == epochDay) return p to null
        val streak = if (p.lastDay == epochDay - 1) p.streak + 1 else 1
        val bonus = Bonus(streak, 50 * minOf(streak, 7), if (streak >= 7) 3 else 2)
        return p.copy(streak = streak, lastDay = epochDay, coins = p.coins + bonus.coins) to bonus
    }
}

/** Number words: Lithuanian nouns change with the count. */
object Plural {
    /** Lithuanian noun form for [n]: "1 moneta", "2 monetos", "10 monetų", "21 moneta". */
    fun lt(n: Int, one: String, few: String, many: String) = when {
        n % 10 == 1 && n % 100 != 11 -> one
        n % 10 == 0 || n % 100 in 11..19 -> many
        else -> few
    }

    /** "1 žuvį", "2 žuvis", "10 žuvų", "21 žuvį". */
    fun fishLt(n: Int): String {
        val word = when {
            n % 10 == 1 && n % 100 != 11 -> "žuvį"
            n % 10 == 0 || n % 100 in 11..19 -> "žuvų"
            else -> "žuvis"
        }
        return "$n $word"
    }

    fun inWaterLt(w: Water?) = when (w) { Water.POND -> "tvenkinyje"; Water.RIVER -> "upėje"; Water.SEA -> "jūroje"; else -> "ežere" }
    fun inWaterDe(w: Water?) = when (w) { Water.POND -> "im Teich"; Water.RIVER -> "im Fluss"; Water.SEA -> "im Meer"; else -> "im See" }
    fun inWaterEn(w: Water?) = when (w) { Water.POND -> "a pond"; Water.RIVER -> "a river"; Water.SEA -> "the sea"; else -> "a lake" }

    /** "0,8 kg" in Lithuanian and German, "0.8 kg" in English. */
    fun kg(grams: Int, lang: Lang): String {
        val v = "%.1f".format(java.util.Locale.US, grams / 1000.0).removeSuffix(".0")
        return (if (lang == Lang.EN) v else v.replace('.', ',')) + " kg"
    }
}
