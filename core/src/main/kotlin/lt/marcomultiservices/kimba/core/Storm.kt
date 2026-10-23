package lt.marcomultiservices.kimba.core

/**
 * Thunderstorms from the real forecast. Before a storm the pressure falls and fish feed hard,
 * which anglers know well; during it a real angler with a carbon rod should leave the water.
 * The game warns ahead, keeps the boat on shore while it thunders and pays double for a fish
 * caught in a real storm.
 */
object Storm {
    /** Coins for a fish landed while it thunders are multiplied by this. */
    const val BONUS = 2

    /** How far ahead the warning looks. */
    const val WARN_HOURS = 4

    /** Start of the first thunder hour within [hours] after [now], or null. */
    fun ahead(f: Forecast, now: Long, hours: Int = WARN_HOURS): Long? = f.hours.firstOrNull {
        it.sky == Sky.THUNDER && it.epochMillis > now && it.epochMillis <= now + hours * 3_600_000L
    }?.epochMillis

    fun on(w: Weather): Boolean = w.sky == Sky.THUNDER
}
