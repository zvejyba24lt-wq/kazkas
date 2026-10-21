package lt.marcomultiservices.kimba.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import lt.marcomultiservices.kimba.MainActivity
import lt.marcomultiservices.kimba.R
import lt.marcomultiservices.kimba.app
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Spot
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.core.Windows
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.currentLang
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Every two hours: fetches the forecast and, when a golden fish's window is open now or opens
 * within the next two hours, tells the player once. These alerts are true, which is the point.
 */
class BiteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val saved = ctx.app.store.current()
        val place = saved.place ?: return Result.success()
        if (!saved.notify) return Result.success()
        val forecast = ctx.app.weather.forecast(place) ?: return Result.retry()
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        // Home waters the player has opened (trips have their own weather and are not watched).
        val level = saved.progress.level
        val goldens = Fish.goldens.filter { g -> Water.entries.any { it.homeLevel <= level && g.base.livesAt(Spot(it)) } }
        val window = Windows.golden(forecast, now, place.lat, place.lon, zone, hours = 3, goldens = goldens)
            .firstOrNull { it.startMillis <= now + 2 * 3_600_000L && "${it.golden.id}@${it.startMillis}" !in saved.notified }
            ?: return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val lang = currentLang(ctx)
        val until = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(window.endMillis).atZone(zone))
        val intent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_float)
            .setContentTitle(S.notifyTitle[lang].format(window.golden.name[lang]))
            .setContentText(S.notifyBody[lang].format(window.golden.condition[lang], until))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(window.golden.id.hashCode(), n) }
        ctx.app.store.update { it.copy(notified = it.notified + "${window.golden.id}@${window.startMillis}") }
        return Result.success()
    }

    companion object {
        const val CHANNEL = "bites"
    }
}
