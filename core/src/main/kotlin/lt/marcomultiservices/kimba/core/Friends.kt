package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

/** One fish in a friend's keep pond. */
@Serializable
data class FishMark(val species: String, val cm: Double, val golden: String? = null)

/**
 * What a friend shares about themself as a short code in a message: their friend code, this
 * week's tournament result, the pieces of their camp and their best fish. Pasted into the game
 * it puts them on the weekly table and lets the player visit their camp. No server needed.
 */
@Serializable
data class FriendCard(
    val code: String,
    val week: Int,
    val grams: Int,
    val camp: List<String> = emptyList(),
    val best: List<FishMark> = emptyList(),
    val level: Int = 1,
)

/**
 * The weekly tournament among friends: every week (Monday to Sunday) one kind of fish, the same
 * for everybody; the heaviest one caught that week counts.
 */
object Tournament {
    private val START: LocalDate = LocalDate.of(2026, 10, 5)
    private val pool = listOf("pike", "perch", "bream", "roach", "carp", "tench", "zander", "crucian", "rudd", "chub")

    fun week(day: Long): Int = Math.floorDiv(day - START.toEpochDay(), 7L).toInt()

    /** Warm-water fish that hardly feed from October to April. */
    private val warmOnly = setOf("tench", "crucian", "rudd")

    /** The week's kind: one that really bites in that week's month, the same for everybody. */
    fun species(week: Int): Species {
        val month = START.plusWeeks(week.toLong()).plusDays(3).monthValue
        val list = if (month in 5..9) pool else pool.filterNot { it in warmOnly }
        return Fish.byId(list[Math.floorMod(week, list.size)]) ?: Fish.all.first()
    }

    /** Days left in the week of [day], counting today. */
    fun daysLeft(day: Long): Int = 7 - Math.floorMod(day - START.toEpochDay(), 7L).toInt()

    /** The player's heaviest fish of the week's kind caught in [week], in grams (0 if none). */
    fun best(catches: List<Catch>, week: Int, zone: ZoneId): Int {
        val id = species(week).id
        return catches.filter { it.species == id && it.golden == null && week(Instant.ofEpochMilli(it.epochMillis).atZone(zone).toLocalDate().toEpochDay()) == week }
            .maxOfOrNull { it.grams } ?: 0
    }
}

object FriendCards {
    const val PREFIX = "KIMBA1:"
    const val MAX_FRIENDS = 30
    private val json = Json { ignoreUnknownKeys = true }
    private val pattern = Regex("KIMBA1:([A-Za-z0-9_-]+)")

    fun make(p: Progress, catches: List<Catch>, day: Long, zone: ZoneId): FriendCard {
        val week = Tournament.week(day)
        val best = catches.groupBy { it.golden ?: it.species }.values.map { l -> l.maxBy { it.grams } }
            .sortedByDescending { it.grams }.take(6).map { FishMark(it.species, it.cm, it.golden) }
        return FriendCard(p.inviteCode, week, Tournament.best(catches, week, zone), CampItem.entries.filter { p.ownsCamp(it) }.map { it.name }, best, p.level)
    }

    fun encode(card: FriendCard): String =
        PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(json.encodeToString(FriendCard.serializer(), card).toByteArray())

    /** The first friend card found anywhere in [text] (a pasted message), or null. */
    fun find(text: String): FriendCard? {
        val m = pattern.find(text) ?: return null
        return runCatching {
            json.decodeFromString(FriendCard.serializer(), String(Base64.getUrlDecoder().decode(m.groupValues[1])))
        }.getOrNull()?.takeIf { it.code.length == 6 }
    }

    /** Adds or refreshes [card] among [p]'s friends; null when it is the player's own. */
    fun add(p: Progress, card: FriendCard): Progress? {
        if (card.code == p.inviteCode) return null
        val others = p.friendCards.filterNot { it.code == card.code }
        return p.copy(friendCards = (others + card).takeLast(MAX_FRIENDS))
    }
}
