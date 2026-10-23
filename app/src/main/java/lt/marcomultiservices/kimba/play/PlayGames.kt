package lt.marcomultiservices.kimba.play

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import java.lang.ref.WeakReference

/**
 * Google Play Games: leaderboards and achievements. Every ID lives in res/values/games-ids.xml;
 * while the project ID there is empty (before the game is set up in Play Console) nothing here
 * touches Play Games at all and the game works exactly as without it.
 *
 * Players are signed in automatically by Play Games when it is set up on the phone; scores and
 * achievements reached before that wait here and are sent on the next resume.
 */
object PlayGames {
    enum class Board(val res: String) {
        BIGGEST_FISH("leaderboard_biggest_fish"),
        DAILY_CHALLENGE("leaderboard_daily_challenge"),
        FISH_CAUGHT("leaderboard_fish_caught"),
        EXPERIENCE("leaderboard_experience"),
    }

    private const val RC_UI = 9004

    var enabled = false
        private set
    private lateinit var app: Application
    private var activity: WeakReference<Activity>? = null
    private val scores = mutableMapOf<Board, Long>()
    private val unlocks = mutableSetOf<String>()
    @Volatile private var signedIn = false

    fun init(app: Application) {
        this.app = app
        enabled = id(app, "game_services_project_id").isNotBlank()
        if (enabled) runCatching { PlayGamesSdk.initialize(app) }.onFailure { enabled = false }
    }

    private fun id(context: Context, name: String): String {
        val res = context.resources.getIdentifier(name, "string", context.packageName)
        return if (res == 0) "" else context.getString(res).trim()
    }

    /** Called from the activity's onResume: checks the sign-in and sends what is waiting. */
    fun attach(a: Activity) {
        activity = WeakReference(a)
        if (!enabled) return
        runCatching {
            PlayGames.getGamesSignInClient(a).isAuthenticated.addOnCompleteListener { task ->
                signedIn = task.isSuccessful && task.result?.isAuthenticated == true
                if (signedIn) flush()
            }
        }
    }

    /** The activity in front, for other Play features such as the review prompt. */
    fun current(): Activity? = activity?.get()

    fun detach(a: Activity) {
        if (activity?.get() === a) activity = null
    }

    /** Sends [value] to [board] (Play keeps each player's best). */
    @Synchronized
    fun score(board: Board, value: Long) {
        if (!enabled || value <= 0) return
        scores[board] = maxOf(value, scores[board] ?: 0L)
        flush()
    }

    /** Unlocks the Play achievement for the game's own achievement [key] (e.g. "first_fish"). */
    @Synchronized
    fun unlock(key: String) {
        if (!enabled) return
        unlocks += key
        flush()
    }

    @Synchronized
    private fun flush() {
        val a = activity?.get() ?: return
        if (!signedIn) return
        runCatching {
            val boards = PlayGames.getLeaderboardsClient(a)
            for ((b, v) in scores) id(a, b.res).takeIf { it.isNotBlank() }?.let { boards.submitScore(it, v) }
            scores.clear()
            val achievements = PlayGames.getAchievementsClient(a)
            for (k in unlocks) id(a, "achievement_$k").takeIf { it.isNotBlank() }?.let { achievements.unlock(it) }
            unlocks.clear()
        }
    }

    /** Opens Play's list of all leaderboards, signing in first if needed. */
    fun showLeaderboards(a: Activity) = withSignIn(a) {
        PlayGames.getLeaderboardsClient(a).allLeaderboardsIntent.addOnSuccessListener { a.startActivityForResult(it, RC_UI) }
    }

    /** Opens Play's achievements screen. */
    fun showAchievements(a: Activity) = withSignIn(a) {
        PlayGames.getAchievementsClient(a).achievementsIntent.addOnSuccessListener { a.startActivityForResult(it, RC_UI) }
    }

    private fun withSignIn(a: Activity, then: () -> Unit) {
        if (!enabled) return
        runCatching {
            if (signedIn) { then(); return }
            PlayGames.getGamesSignInClient(a).signIn().addOnCompleteListener { task ->
                signedIn = task.isSuccessful && task.result?.isAuthenticated == true
                if (signedIn) { flush(); then() }
            }
        }
    }
}
