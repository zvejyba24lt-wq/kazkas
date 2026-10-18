package lt.marcomultiservices.skaitaupats

import android.app.Application
import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import lt.marcomultiservices.skaitaupats.audio.Speaker
import lt.marcomultiservices.skaitaupats.billing.BillingManager
import lt.marcomultiservices.skaitaupats.core.Course
import lt.marcomultiservices.skaitaupats.core.ContentParser
import lt.marcomultiservices.skaitaupats.core.CourseBuilder
import lt.marcomultiservices.skaitaupats.data.ProgressRepository

class SkaitauApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val course: Course by lazy {
        CourseBuilder.build(ContentParser.parse(assets.open("kursas.json").bufferedReader().use { it.readText() }))
    }
    val progress by lazy { ProgressRepository(this) }
    val speaker by lazy { Speaker(this) }
    val billing by lazy { BillingManager(this, scope) { owned -> progress.setOwned(owned) } }

    override fun onCreate() {
        super.onCreate()
        @Suppress("DEPRECATION")
        EmojiCompat.init(BundledEmojiCompatConfig(this).setReplaceAll(true))
    }
}

val Context.app: SkaitauApp get() = applicationContext as SkaitauApp
