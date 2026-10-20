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
) {
    val level: Int get() = Levels.levelOf(xp)
    val gear: Gear get() = Gear(lure, rod, reel, line, Upgrade.BOAT.key in owned)
    val boxSize: Int get() = when {
        Upgrade.BOX_10.key in owned -> 10
        Upgrade.BOX_7.key in owned -> 7
        else -> Baits.MAX
    }
    val spot: Spot get() = Destinations.byId(trip)?.spot ?: Spot(water)

    fun owns(item: Item): Boolean = item.key in owned
    fun canBuy(item: Item): Boolean = !owns(item) && level >= item.level && coins >= item.price

    fun buy(item: Item): Progress? {
        if (!canBuy(item)) return null
        val p = copy(coins = coins - item.price, owned = owned + item.key)
        return p.equip(item)
    }

    fun equip(item: Item): Progress {
        if (!owns(item)) return this
        return when (item) {
            is Lure -> copy(lure = item)
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

    fun goHome(water: Water): Progress = if (level >= water.homeLevel) copy(trip = null, water = water) else copy(trip = null)
}

object Levels {
    /** Total XP needed to reach [level]. */
    fun xpFor(level: Int): Int = 50 * (level - 1) * level

    fun levelOf(xp: Int): Int {
        var l = 1
        while (xpFor(l + 1) <= xp) l++
        return l
    }
}

/** What a landed fish earns. */
class Reward(val coins: Int, val xp: Int, val levelUp: Int?, val quests: List<Quest>, val allQuests: Boolean)

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

    /** Applies a landed fish to [p]: coins, XP and quest progress. */
    fun land(p: Progress, catch: Catch, isNew: Boolean, epochDay: Long): Pair<Progress, Reward> {
        val species = Fish.byId(catch.species) ?: return p to Reward(0, 0, null, emptyList(), false)
        val golden = catch.golden?.let { Fish.goldenById(it) }
        var coins = coins(species, golden, catch.grams)
        val xp = xp(species, golden, isNew)
        val quests = Quests.today(p.quests, epochDay)
        val (updated, done) = quests.record(catch)
        coins += done.sumOf { it.coins }
        val all = done.isNotEmpty() && updated.allDone
        if (all) coins += ALL_QUESTS_COINS
        val before = p.level
        val next = p.copy(coins = p.coins + coins, xp = p.xp + xp, quests = updated)
        return next to Reward(coins, xp, next.level.takeIf { it > before }, done, all)
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
            QuestKind.COUNT -> L("Sugauk $target žuvų", "Fange $target Fische", "Catch $target fish")[lang]
            QuestKind.SPECIES -> L("Sugauk: $name ×$target", "Fange: $name ×$target", "Catch: $name ×$target")[lang]
            QuestKind.BIG -> L("Sugauk žuvį, sveriančią ${target / 1000.0} kg ar daugiau", "Fange einen Fisch ab ${target / 1000.0} kg", "Catch a fish of ${target / 1000.0} kg or more")[lang]
            QuestKind.TOTAL_KG -> L("Sugauk iš viso ${target / 1000} kg žuvies", "Fange insgesamt ${target / 1000} kg Fisch", "Catch ${target / 1000} kg of fish in total")[lang]
            QuestKind.GOLDEN_OR_RARE -> L("Sugauk retą žuvį (plėšrūną ar auksinę)", "Fange einen seltenen Fisch (Räuber oder golden)", "Catch a rare fish (predator or golden)")[lang]
            QuestKind.WATER -> L("Sugauk $target žuvis: ${water?.label?.get(Lang.LT)?.lowercase()}", "Fange $target Fische: ${water?.label?.get(Lang.DE)}", "Catch $target fish: ${water?.label?.get(Lang.EN)?.lowercase()}")[lang]
        }
    }

    /** Progress shown to the player, e.g. "3/5" or "1.2/3 kg". */
    fun progressText(): String = when (kind) {
        QuestKind.BIG -> if (done) "✓" else "0/1"
        QuestKind.TOTAL_KG -> "%.1f/%d kg".format(progress.coerceAtMost(target) / 1000.0, target / 1000)
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
        private val common = listOf("roach", "perch", "bream", "rudd", "pike", "crucian", "tench", "bleak")

        fun today(current: Quests?, epochDay: Long): Quests =
            if (current != null && current.day == epochDay) current else generate(epochDay)

        fun generate(epochDay: Long): Quests {
            val rng = Random(epochDay * 7919 + 17)
            val kinds = listOf(QuestKind.SPECIES, QuestKind.BIG, QuestKind.TOTAL_KG, QuestKind.GOLDEN_OR_RARE, QuestKind.WATER).shuffled(rng).take(2)
            val list = listOf(Quest(QuestKind.COUNT, 5 + rng.nextInt(4), coins = 100)) + kinds.map { k ->
                when (k) {
                    QuestKind.SPECIES -> Quest(k, 2 + rng.nextInt(2), species = common[rng.nextInt(common.size)], coins = 150)
                    QuestKind.BIG -> Quest(k, listOf(800, 1000, 1500)[rng.nextInt(3)], coins = 200)
                    QuestKind.TOTAL_KG -> Quest(k, 3000 + 1000 * rng.nextInt(3), coins = 150)
                    QuestKind.GOLDEN_OR_RARE -> Quest(k, 1, coins = 200)
                    else -> Quest(QuestKind.WATER, 3, water = Water.LAKE, coins = 120)
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
