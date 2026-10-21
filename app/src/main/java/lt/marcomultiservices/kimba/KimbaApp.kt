package lt.marcomultiservices.kimba

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.MainScope
import lt.marcomultiservices.kimba.ads.AdManager
import lt.marcomultiservices.kimba.audio.Sfx
import lt.marcomultiservices.kimba.ads.BillingManager
import lt.marcomultiservices.kimba.data.Store
import lt.marcomultiservices.kimba.data.WeatherRepo
import lt.marcomultiservices.kimba.notify.BiteWorker
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import java.util.concurrent.TimeUnit

class KimbaApp : Application() {
    val store by lazy { Store(this) }
    val weather by lazy { WeatherRepo(this) }
    val ads by lazy { AdManager(this) }
    val sfx by lazy { Sfx(this) }
    private val scope = MainScope()
    val billing by lazy {
        BillingManager(this, scope) { owned ->
            store.update { s -> if (s.progress.noAds == owned) s else s.copy(progress = s.progress.copy(noAds = owned)) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ArtPack.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(BiteWorker.CHANNEL, getString(R.string.channel_bites), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val request = PeriodicWorkRequestBuilder<BiteWorker>(2, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("bites", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

val Context.app: KimbaApp get() = applicationContext as KimbaApp

/** The activity behind a Compose context (for ads and billing). */
tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
