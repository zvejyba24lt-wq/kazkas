package lt.marcomultiservices.skaitaupats.core

/**
 * The free version: the whole course, but only a few new lessons a day and a short ad after a
 * finished lesson. Lessons already finished can always be repeated. The one-time purchase removes both.
 */
object FreePlan {
    const val NEW_LESSONS_PER_DAY = 2

    /** Lessons finished for the first time on [today], given the day and count stored last. */
    fun newToday(storedDay: Long, storedCount: Int, today: Long): Int = if (storedDay == today) storedCount else 0

    /** The day and count to store after a lesson was finished for the first time on [today]. */
    fun afterNewLesson(storedDay: Long, storedCount: Int, today: Long): Pair<Long, Int> =
        today to newToday(storedDay, storedCount, today) + 1

    fun mayOpen(alreadyFinished: Boolean, newToday: Int): Boolean =
        alreadyFinished || newToday < NEW_LESSONS_PER_DAY
}
