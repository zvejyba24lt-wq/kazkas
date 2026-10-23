package lt.marcomultiservices.kimba.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import lt.marcomultiservices.kimba.core.Baits
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Lure
import lt.marcomultiservices.kimba.core.Progress

private val Context.dataStore by preferencesDataStore("kimba")

@Serializable
data class Place(val name: String, val lat: Double, val lon: Double, val fromDevice: Boolean = false)

data class Saved(
    val onboarded: Boolean = false,
    val place: Place? = null,
    val baits: Baits = Baits(Baits.MAX, 0),
    val catches: List<Catch> = emptyList(),
    val progress: Progress = Progress(),
    val notify: Boolean = true,
    /** Sound effects and the ambient sounds of the lake. */
    val sound: Boolean = true,
    /** Golden windows already announced, as "id@startMillis", so each is notified once. */
    val notified: Set<String> = emptySet(),
    /** First-play tips the player has already learned (see `game.Tip`). */
    val tips: Set<String> = emptySet(),
    /** Google Play's install referrer was read (it may carry a friend code). */
    val referrerChecked: Boolean = false,
)

/** Everything the game remembers, in one DataStore. */
class Store(private val context: Context) {
    // An unknown value (from a newer or older version) falls back to its default instead of
    // failing the whole save.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    private val kOnboarded = booleanPreferencesKey("onboarded")
    private val kPlace = stringPreferencesKey("place")
    private val kBaits = stringPreferencesKey("baits")
    private val kCatches = stringPreferencesKey("catches")
    private val kLure = stringPreferencesKey("lure")
    private val kProgress = stringPreferencesKey("progress")
    private val kNotify = booleanPreferencesKey("notify")
    private val kSound = booleanPreferencesKey("sound")
    private val kNotified = stringPreferencesKey("notified")
    private val kTips = stringPreferencesKey("tips")
    private val kReferrer = booleanPreferencesKey("referrer_checked")

    val flow: Flow<Saved> = context.dataStore.data.map(::read)

    suspend fun current(): Saved = flow.first()

    suspend fun update(transform: (Saved) -> Saved) {
        context.dataStore.edit { p ->
            // A save that can't be read is kept aside before anything is written over it, so a
            // bad update can never silently wipe the player's progress or fish book.
            keepUnreadable(p, kProgress, Progress.serializer())
            keepUnreadable(p, kCatches, ListSerializer(Catch.serializer()))
            val s = transform(read(p))
            p[kOnboarded] = s.onboarded
            if (s.place != null) p[kPlace] = json.encodeToString(Place.serializer(), s.place) else p.remove(kPlace)
            p[kBaits] = json.encodeToString(Baits.serializer(), s.baits)
            p[kCatches] = json.encodeToString(ListSerializer(Catch.serializer()), s.catches)
            p[kProgress] = json.encodeToString(Progress.serializer(), s.progress)
            p[kNotify] = s.notify
            p[kSound] = s.sound
            p[kNotified] = s.notified.takeLast(40).joinToString("|")
            p[kTips] = s.tips.joinToString("|")
            p[kReferrer] = s.referrerChecked
        }
    }

    private fun <T> Set<T>.takeLast(n: Int): List<T> = toList().takeLast(n)

    private fun <T> keepUnreadable(p: androidx.datastore.preferences.core.MutablePreferences, key: Preferences.Key<String>, serializer: kotlinx.serialization.KSerializer<T>) {
        val raw = p[key] ?: return
        if (runCatching { json.decodeFromString(serializer, raw) }.isSuccess) return
        val backup = stringPreferencesKey(key.name + "_unreadable")
        if (p[backup] == null) p[backup] = raw
    }

    private fun read(p: Preferences): Saved = Saved(
        onboarded = p[kOnboarded] ?: false,
        place = p[kPlace]?.let { runCatching { json.decodeFromString(Place.serializer(), it) }.getOrNull() },
        baits = p[kBaits]?.let { runCatching { json.decodeFromString(Baits.serializer(), it) }.getOrNull() } ?: Baits(Baits.MAX, 0),
        catches = p[kCatches]?.let { runCatching { json.decodeFromString(ListSerializer(Catch.serializer()), it) }.getOrNull() } ?: emptyList(),
        progress = (p[kProgress]?.let { runCatching { json.decodeFromString(Progress.serializer(), it) }.getOrNull() } ?: Progress()).migrated(),
        notify = p[kNotify] ?: true,
        sound = p[kSound] ?: true,
        notified = p[kNotified]?.split("|")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet(),
        tips = p[kTips]?.split("|")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet(),
        referrerChecked = p[kReferrer] ?: false,
    )
}

object Places {
    val cities = listOf(
        Place("Vilnius", 54.687, 25.280),
        Place("Kaunas", 54.899, 23.904),
        Place("Klaipėda", 55.703, 21.144),
        Place("Šiauliai", 55.935, 23.314),
        Place("Panevėžys", 55.735, 24.358),
        Place("Berlin", 52.520, 13.405),
        Place("Hamburg", 53.551, 9.994),
        Place("München", 48.135, 11.582),
        Place("Köln", 50.938, 6.960),
        Place("Frankfurt", 50.111, 8.682),
        Place("Leipzig", 51.340, 12.373),
        Place("Wien", 48.208, 16.374),
        Place("Graz", 47.071, 15.440),
        Place("Salzburg", 47.810, 13.055),
        Place("Zürich", 47.377, 8.542),
        Place("London", 51.507, -0.128),
    )
}
