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
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Forecast
import lt.marcomultiservices.kimba.core.Lang
import lt.marcomultiservices.kimba.core.Spot
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.core.Windows
import lt.marcomultiservices.kimba.data.Saved
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.currentLang
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Every two hours: fetches the forecast and, when a golden fish's window is open now or opens
 * within the next two hours, tells the player once; otherwise maybe the daily report. These
 * alerts are true, which is the point.
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
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val lang = currentLang(ctx)
        val intent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // A thunderstorm due within a few hours: fish feed hard before it. Once a day, by day.
        val storm = lt.marcomultiservices.kimba.core.Storm.ahead(forecast, now)
        val stormKey = "stormwarn@${Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()}"
        if (storm != null && stormKey !in saved.notified && Instant.ofEpochMilli(now).atZone(zone).hour in 8..20) {
            val at = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(storm).atZone(zone))
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_float)
                .setContentTitle(S.stormNotifyTitle[lang])
                .setContentText(S.stormNotifyBody[lang].format(at))
                .setStyle(NotificationCompat.BigTextStyle().bigText(S.stormNotifyBody[lang].format(at)))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(ctx).notify(STORM_ID, n) }
            ctx.app.store.update { it.copy(notified = it.notified.filterNot { k -> k.startsWith("stormwarn@") }.toSet() + stormKey) }
            return Result.success()
        }
        val window = Windows.golden(forecast, now, place.lat, place.lon, zone, hours = 3, goldens = goldens)
            .firstOrNull { it.startMillis <= now + 2 * 3_600_000L && "${it.golden.id}@${it.startMillis}" !in saved.notified }
            ?: return dailyReport(saved, forecast, now, zone, lang, intent)

        val until = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(window.endMillis).atZone(zone))
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

    /**
     * At most once a day, for a player who has not fished yet today: which fish bites right now
     * in their real weather (it pays double in the game). Sent when the bite is good, or in the
     * evening at the latest, and never at night.
     */
    private suspend fun dailyReport(saved: Saved, forecast: Forecast, now: Long, zone: ZoneId, lang: Lang, intent: PendingIntent): Result {
        val ctx = applicationContext
        val place = saved.place ?: return Result.success()
        val time = Instant.ofEpochMilli(now).atZone(zone)
        val day = time.toLocalDate().toEpochDay()
        val key = "day@$day"
        if (key in saved.notified || saved.progress.lastDay == day || time.hour !in 9..20) return Result.success()
        val weather = forecast.weatherAt(now) ?: return Result.success()
        val c = Conditions(weather, now, place.lat, place.lon, zone)
        val spot = Spot(saved.progress.water)
        val index = Fish.biteIndex(c, spot)
        if (index < 55 && time.hour < 17) return Result.success()
        val hot = Fish.hotSpecies(c, spot, saved.progress.onIce) ?: return Result.success()
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_float)
            .setContentTitle(S.reportTitle[lang].format(hot.name[lang]))
            .setContentText(S.reportBody[lang].format(S.sky(weather.sky)[lang], weather.tempC.roundToInt(), index))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(REPORT_ID, n) }
        ctx.app.store.update { it.copy(notified = it.notified.filterNot { k -> k.startsWith("day@") }.toSet() + key) }
        return Result.success()
    }

    companion object {
        const val CHANNEL = "bites"
        private const val REPORT_ID = 7101
        private const val STORM_ID = 7102
    }
}
