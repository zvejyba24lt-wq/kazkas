package lt.marcomultiservices.skaitaupats.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lt.marcomultiservices.skaitaupats.core.FreePlan
import java.time.LocalDate

private val Context.store by preferencesDataStore("progress")

data class Progress(
    /** Best stars (1..3) per finished lesson id. */
    val stars: Map<Int, Int> = emptyMap(),
    /** How often the child picked a wrong answer for each letter. */
    val letterMistakes: Map<String, Int> = emptyMap(),
    val owned: Boolean = false,
    /** Test builds only: pretend the purchase was made, to see the app without ads and limit. */
    val testOwned: Boolean = false,
    /** The day (epoch day) and number of lessons finished for the first time that day. */
    val newDay: Long = 0,
    val newCount: Int = 0,
) {
    fun finished(id: Int) = (stars[id] ?: 0) > 0

    /** Bought: no ads and no daily limit. */
    val adFree: Boolean get() = owned || testOwned

    fun newToday(day: Long = today()): Int = FreePlan.newToday(newDay, newCount, day)
}

fun today(): Long = LocalDate.now().toEpochDay()

class ProgressRepository(private val context: Context) {
    val progress: Flow<Progress> = context.store.data.map { prefs ->
        val stars = mutableMapOf<Int, Int>()
        val mistakes = mutableMapOf<String, Int>()
        for ((key, value) in prefs.asMap()) {
            when {
                key.name.startsWith(STARS) -> key.name.removePrefix(STARS).toIntOrNull()?.let { stars[it] = value as Int }
                key.name.startsWith(MISTAKES) -> mistakes[key.name.removePrefix(MISTAKES)] = value as Int
            }
        }
        Progress(stars, mistakes, prefs[OWNED] ?: false, prefs[TEST_OWNED] ?: false, prefs[NEW_DAY] ?: 0L, prefs[NEW_COUNT] ?: 0)
    }

    suspend fun finishLesson(id: Int, stars: Int, mistakesByLetter: Map<String, Int>) {
        context.store.edit { prefs ->
            val key = intPreferencesKey("$STARS$id")
            if ((prefs[key] ?: 0) == 0) {
                val (day, count) = FreePlan.afterNewLesson(prefs[NEW_DAY] ?: 0L, prefs[NEW_COUNT] ?: 0, today())
                prefs[NEW_DAY] = day
                prefs[NEW_COUNT] = count
            }
            prefs[key] = maxOf(prefs[key] ?: 0, stars)
            for ((letter, n) in mistakesByLetter) {
                val mk = intPreferencesKey("$MISTAKES$letter")
                prefs[mk] = (prefs[mk] ?: 0) + n
            }
        }
    }

    suspend fun setOwned(owned: Boolean) {
        context.store.edit { it[OWNED] = owned }
    }

    suspend fun setTestOwned(owned: Boolean) {
        context.store.edit { it[TEST_OWNED] = owned }
    }

    /** Clears progress but keeps the purchase and today's count, so a reset does not lift the daily limit. */
    suspend fun reset() {
        context.store.edit { prefs ->
            val kept = listOf(OWNED, TEST_OWNED).associateWith { prefs[it] ?: false }
            val day = prefs[NEW_DAY]
            val count = prefs[NEW_COUNT]
            prefs.clear()
            kept.forEach { (k, v) -> prefs[k] = v }
            if (day != null && count != null) { prefs[NEW_DAY] = day; prefs[NEW_COUNT] = count }
        }
    }

    private companion object {
        const val STARS = "stars_"
        const val MISTAKES = "mistakes_"
        val OWNED = booleanPreferencesKey("owned")
        val TEST_OWNED = booleanPreferencesKey("test_owned")
        val NEW_DAY = longPreferencesKey("new_day")
        val NEW_COUNT = intPreferencesKey("new_count")
    }
}
