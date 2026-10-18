package lt.marcomultiservices.skaitaupats.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreePlanTest {
    @Test
    fun countStartsOverEachDay() {
        var day = 0L
        var count = 0
        repeat(FreePlan.NEW_LESSONS_PER_DAY) { FreePlan.afterNewLesson(day, count, 100).let { day = it.first; count = it.second } }
        assertFalse(FreePlan.mayOpen(alreadyFinished = false, newToday = FreePlan.newToday(day, count, 100)))
        assertTrue(FreePlan.mayOpen(alreadyFinished = true, newToday = FreePlan.newToday(day, count, 100)))
        assertEquals(0, FreePlan.newToday(day, count, 101))
        assertTrue(FreePlan.mayOpen(alreadyFinished = false, newToday = FreePlan.newToday(day, count, 101)))
        assertEquals(101L to 1, FreePlan.afterNewLesson(day, count, 101))
    }
}
