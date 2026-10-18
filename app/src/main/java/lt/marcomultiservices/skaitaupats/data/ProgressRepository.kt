package lt.marcomultiservices.skaitaupats.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("progress")

data class Progress(
    /** Best stars (1..3) per finished lesson id. */
    val stars: Map<Int, Int> = emptyMap(),
    /** How often the child picked a wrong answer for each letter. */
    val letterMistakes: Map<String, Int> = emptyMap(),
    val owned: Boolean = false,
) {
    fun finished(id: Int) = (stars[id] ?: 0) > 0
}

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
        Progress(stars, mistakes, prefs[OWNED] ?: false)
    }

    suspend fun finishLesson(id: Int, stars: Int, mistakesByLetter: Map<String, Int>) {
        context.store.edit { prefs ->
            val key = intPreferencesKey("$STARS$id")
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

    suspend fun reset() {
        context.store.edit { prefs ->
            val owned = prefs[OWNED] ?: false
            prefs.clear()
            prefs[OWNED] = owned
        }
    }

    private companion object {
        const val STARS = "stars_"
        const val MISTAKES = "mistakes_"
        val OWNED = booleanPreferencesKey("owned")
    }
}
