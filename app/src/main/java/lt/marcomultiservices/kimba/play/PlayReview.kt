package lt.marcomultiservices.kimba.play

import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play's in-app rating card. Play decides whether it really shows (it limits how often),
 * and it only works for a copy installed from Play; elsewhere nothing happens.
 */
object PlayReview {
    fun ask() {
        val a = PlayGames.current() ?: return
        runCatching {
            val manager = ReviewManagerFactory.create(a)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) manager.launchReviewFlow(a, task.result)
            }
        }
    }
}
