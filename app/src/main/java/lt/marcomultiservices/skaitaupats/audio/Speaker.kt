package lt.marcomultiservices.skaitaupats.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import lt.marcomultiservices.skaitaupats.core.Audio
import java.io.IOException
import kotlin.coroutines.resume

/** Plays the bundled recordings, one text after another. A new request interrupts the current one. */
class Speaker(private val context: Context) {
    private val scope = MainScope()
    private var job: Job? = null
    private var player: MediaPlayer? = null

    private val _speaking = MutableStateFlow<String?>(null)
    /** The text being said right now, or null when quiet. */
    val speaking: StateFlow<String?> = _speaking

    fun say(vararg texts: String, onDone: () -> Unit = {}) = say(texts.toList(), onDone)

    fun say(texts: List<String>, onDone: () -> Unit = {}) {
        job?.cancel()
        job = scope.launch {
            for (t in texts) play(t)
            _speaking.value = null
            onDone()
        }
    }

    fun stop() {
        job?.cancel()
        release()
        _speaking.value = null
    }

    /** How far the current recording has played, 0..1, for highlighting words as they are read. */
    fun fraction(): Float {
        val p = player ?: return 0f
        return runCatching { if (p.duration > 0) p.currentPosition.toFloat() / p.duration else 0f }.getOrDefault(0f)
    }

    private suspend fun play(text: String) {
        release()
        val afd = try {
            context.assets.openFd(Audio.path(text))
        } catch (e: IOException) {
            Log.w(TAG, "No recording for \"$text\"")
            _speaking.value = text
            delay(300)
            return
        }
        suspendCancellableCoroutine { cont ->
            val mp = MediaPlayer()
            player = mp
            _speaking.value = text
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                )
                afd.use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                mp.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.w(TAG, "Playback failed for \"$text\"", e)
                if (cont.isActive) cont.resume(Unit)
            }
            cont.invokeOnCancellation { scope.launch { release() } }
        }
    }

    private fun release() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    private companion object {
        const val TAG = "Speaker"
    }
}
