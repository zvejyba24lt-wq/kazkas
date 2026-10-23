package lt.marcomultiservices.kimba.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lt.marcomultiservices.kimba.MainActivity
import lt.marcomultiservices.kimba.R
import lt.marcomultiservices.kimba.app
import lt.marcomultiservices.kimba.core.Forecast
import lt.marcomultiservices.kimba.core.Spot
import lt.marcomultiservices.kimba.core.Today
import lt.marcomultiservices.kimba.data.Saved
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.currentLang
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The home-screen widget: today's bite at the player's place from the real forecast, the kind
 * that bites best now and the best hour still ahead. Refreshed by the bite worker every two
 * hours and whenever the game fetches the weather. Picture slot `widget_bg` (see docs/GRAFIKA.md)
 * replaces the plain dusk background.
 */
class BiteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                val saved = context.app.store.current()
                val forecast = saved.place?.let { context.app.weather.forecast(it) }
                render(context, saved, forecast)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Whether the player has put the widget on a home screen. */
        fun placed(context: Context): Boolean = runCatching {
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, BiteWidget::class.java)).isNotEmpty()
        }.getOrDefault(false)

        /** Draws every placed widget from [saved] and [forecast] (null shows how to start). */
        fun render(context: Context, saved: Saved, forecast: Forecast?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = runCatching { manager.getAppWidgetIds(ComponentName(context, BiteWidget::class.java)) }.getOrNull() ?: return
            if (ids.isEmpty()) return
            runCatching { manager.updateAppWidget(ids, views(context, saved, forecast)) }
        }

        /** The widget's face for [saved] and [forecast]. */
        fun views(context: Context, saved: Saved, forecast: Forecast?): RemoteViews {
            val lang = currentLang(context)
            val views = RemoteViews(context.packageName, R.layout.widget_bite)
            val open = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            art(context)?.let {
                views.setImageViewBitmap(R.id.widget_art, it)
                views.setViewVisibility(R.id.widget_art, View.VISIBLE)
                views.setViewVisibility(R.id.widget_shade, View.VISIBLE)
            }
            val place = saved.place
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = if (place != null && forecast != null) {
                Today.of(forecast, now, place.lat, place.lon, zone, Spot(saved.progress.water), saved.progress.onIce)
            } else null
            if (place == null || today == null) {
                views.setTextViewText(R.id.widget_index, "🎣")
                views.setTextViewText(R.id.widget_label, "")
                views.setProgressBar(R.id.widget_bar, 100, 0, false)
                views.setTextViewText(R.id.widget_hot, S.widgetStart[lang])
                views.setTextViewText(R.id.widget_best, "")
                views.setTextViewText(R.id.widget_weather, "")
            } else {
                views.setTextViewText(R.id.widget_title, place.name)
                views.setTextViewText(R.id.widget_index, "${today.index}")
                views.setTextViewText(R.id.widget_label, S.widgetBite[lang])
                views.setProgressBar(R.id.widget_bar, 100, today.index, false)
                views.setTextViewText(R.id.widget_hot, today.hot?.let { S.widgetHot[lang].format(it.name[lang]) } ?: "")
                views.setTextViewText(R.id.widget_weather, "${S.skyIcon(today.weather.sky)} ${today.weather.tempC.roundToInt()}°")
                val best = if (today.bestMillis <= now + 30 * 60_000L) S.widgetNow[lang]
                else S.widgetBest[lang].format(DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(today.bestMillis).atZone(zone)), today.bestIndex)
                views.setTextViewText(R.id.widget_best, best)
            }
            return views
        }

        private fun art(context: Context) = runCatching {
            val names = context.assets.list("art")?.toSet() ?: emptySet()
            val file = listOf("webp", "png", "jpg").map { "widget_bg.$it" }.firstOrNull { it in names } ?: return@runCatching null
            context.assets.open("art/$file").use { stream ->
                // Small enough for the widget host's size limit.
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 2 })
            }
        }.getOrNull()
    }
}
