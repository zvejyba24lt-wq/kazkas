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
import lt.marcomultiservices.kimba.data.Store
import lt.marcomultiservices.kimba.data.WeatherRepo
import lt.marcomultiservices.kimba.notify.BiteWorker
import java.util.concurrent.TimeUnit

class KimbaApp : Application() {
    val store by lazy { Store(this) }
    val weather by lazy { WeatherRepo(this) }

    override fun onCreate() {
        super.onCreate()
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
