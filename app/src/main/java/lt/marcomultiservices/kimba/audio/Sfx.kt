package lt.marcomultiservices.kimba.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * All sounds of the game, synthesised on the device at first start (no audio files to ship):
 * short effects for casting, bites, the reel and the catch, and seamless ambient loops for the
 * lake by day, at night, in the rain, on the river and by the sea.
 */
class Sfx(context: Context) {
    enum class Fx { CAST, PLOP, NIBBLE, BITE, REEL, STRAIN, SNAP, SPLASH, COIN, LEVEL, TAP, ALARM }
    enum class Amb { DAY, CALM, NIGHT, RAIN, RIVER, SEA }

    private val pool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val fxIds = ConcurrentHashMap<Fx, Int>()
    private val ambIds = ConcurrentHashMap<Amb, Int>()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val lastPlayed = ConcurrentHashMap<Fx, Long>()

    @Volatile var enabled = true
        set(value) {
            field = value
            if (!value) stopAmbient() else wantedAmb?.let { startAmbient(it) }
        }

    @Volatile private var wantedAmb: Amb? = null
    private var ambStream = 0
    private var ambPlaying: Amb? = null

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) {
                loaded += id
                val want = wantedAmb
                if (want != null && ambIds[want] == id && enabled) startAmbient(want)
            }
        }
        val dir = File(context.cacheDir, "sfx-v1").apply { mkdirs() }
        thread(name = "sfx-synth", isDaemon = true) {
            // Short effects first so they are ready by the first cast.
            for (fx in Fx.entries) fxIds[fx] = pool.load(file(dir, fx.name) { Synth.fx(fx) }.path, 1)
            for (a in Amb.entries) ambIds[a] = pool.load(file(dir, a.name) { Synth.ambient(a) }.path, 1)
        }
    }

    private fun file(dir: File, name: String, make: () -> FloatArray): File {
        val f = File(dir, "$name.wav")
        if (!f.exists() || f.length() < 64) Wav.write(f, make())
        return f
    }

    /**
     * Plays an effect. [minGapMs] keeps repeated sounds (reel clicks, creaks) from piling up.
     */
    fun play(fx: Fx, volume: Float = 1f, rate: Float = 1f, minGapMs: Long = 0) {
        if (!enabled) return
        val id = fxIds[fx] ?: return
        if (id !in loaded) return
        val now = SystemClock.uptimeMillis()
        if (minGapMs > 0 && now - (lastPlayed[fx] ?: 0) < minGapMs) return
        lastPlayed[fx] = now
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** The background loop for the scene, or null for silence (app in the background, menus). */
    @Synchronized
    fun ambient(a: Amb?) {
        wantedAmb = a
        if (a == null) stopAmbient() else if (enabled) startAmbient(a)
    }

    @Synchronized
    private fun startAmbient(a: Amb) {
        if (ambPlaying == a && ambStream != 0) return
        val id = ambIds[a] ?: return
        if (id !in loaded) return
        stopAmbient()
        ambStream = pool.play(id, AMB_VOLUME, AMB_VOLUME, 0, -1, 1f)
        ambPlaying = a
    }

    @Synchronized
    private fun stopAmbient() {
        if (ambStream != 0) pool.stop(ambStream)
        ambStream = 0
        ambPlaying = null
    }

    companion object {
        const val AMB_VOLUME = 0.32f
    }
}

/** 16-bit mono PCM WAV files. */
private object Wav {
    fun write(f: File, samples: FloatArray, rate: Int = Synth.RATE) {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) data.putShort((s.coerceIn(-1f, 1f) * 32000).toInt().toShort())
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples.size * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(samples.size * 2)
        }
        val tmp = File(f.parentFile, f.name + ".tmp")
        FileOutputStream(tmp).use { it.write(header.array()); it.write(data.array()) }
        tmp.renameTo(f)
    }
}

/** Tiny sound synthesiser: noise, filters, bubbles and tones. */
internal object Synth {
    const val RATE = 22050
    private val TWO_PI = (2 * PI).toFloat()

    fun fx(fx: Sfx.Fx): FloatArray = when (fx) {
        Sfx.Fx.CAST -> cast()
        Sfx.Fx.PLOP -> plop()
        Sfx.Fx.NIBBLE -> buf(0.14f).also { bubble(it, 0f, 650f, 1100f, 0.09f, 0.35f) }
        Sfx.Fx.BITE -> bite()
        Sfx.Fx.REEL -> reel()
        Sfx.Fx.STRAIN -> strain()
        Sfx.Fx.SNAP -> snap()
        Sfx.Fx.SPLASH -> splash()
        Sfx.Fx.COIN -> coin()
        Sfx.Fx.LEVEL -> level()
        Sfx.Fx.TAP -> buf(0.05f).also { tone(it, 0f, 0.05f, 1250f, 0.25f, decay = 110f) }
        // The electronic bite alarm: a short square-ish beep.
        Sfx.Fx.ALARM -> buf(0.16f).also { tone(it, 0f, 0.15f, 2350f, 0.5f, decay = 6f, harmonics = 0.6f) }
    }.also { normalize(it) }

    fun ambient(a: Sfx.Amb): FloatArray = when (a) {
        Sfx.Amb.DAY -> loop(8f) { n, r -> lapping(n, r, 0.5f); birds(n, r, 7) }
        Sfx.Amb.CALM -> loop(8f) { n, r -> lapping(n, r, 0.55f) }
        Sfx.Amb.NIGHT -> loop(8f) { n, r -> lapping(n, r, 0.3f); crickets(n, r) }
        Sfx.Amb.RAIN -> loop(8f) { n, r -> rain(n, r) }
        Sfx.Amb.RIVER -> loop(8f) { n, r -> river(n, r); birds(n, r, 4) }
        Sfx.Amb.SEA -> loop(8f) { n, r -> surf(n, r); gulls(n, r) }
    }

    private fun buf(sec: Float) = FloatArray((sec * RATE).toInt())

    private fun normalize(b: FloatArray, peak: Float = 0.9f) {
        val m = b.maxOf { abs(it) }
        if (m > 0) for (i in b.indices) b[i] *= peak / m
    }

    /** Builds [sec] seconds plus a tail that is cross-faded into the start, so the loop has no seam. */
    private fun loop(sec: Float, fill: (FloatArray, Random) -> Unit): FloatArray {
        val n = (sec * RATE).toInt()
        val fade = RATE / 2
        val b = FloatArray(n + fade)
        fill(b, Random(n))
        for (i in 0 until fade) {
            val k = i / fade.toFloat()
            b[i] = b[i] * k + b[n + i] * (1 - k)
        }
        val out = b.copyOf(n)
        normalize(out, 0.8f)
        return out
    }

    /** A water bubble: a sine whose pitch rises quickly, the classic "bloop". */
    private fun bubble(b: FloatArray, at: Float, f0: Float, f1: Float, len: Float, amp: Float) {
        val s = (at * RATE).toInt()
        val n = (len * RATE).toInt()
        var ph = 0f
        for (i in 0 until n) {
            val idx = s + i
            if (idx !in b.indices) break
            val t = i / n.toFloat()
            val f = f0 + (f1 - f0) * t.pow(0.6f)
            ph += TWO_PI * f / RATE
            val env = (1 - t).pow(2) * (if (i < 40) i / 40f else 1f)
            b[idx] += sin(ph) * env * amp
        }
    }

    private fun tone(b: FloatArray, at: Float, len: Float, f: Float, amp: Float, decay: Float, harmonics: Float = 0f) {
        val s = (at * RATE).toInt()
        val n = (len * RATE).toInt()
        for (i in 0 until n) {
            val idx = s + i
            if (idx !in b.indices) break
            val t = i / RATE.toFloat()
            val env = exp(-t * decay) * (if (i < 60) i / 60f else 1f)
            val x = TWO_PI * f * t
            b[idx] += (sin(x) + harmonics * sin(2 * x) * 0.5f + harmonics * sin(3 * x) * 0.25f) * env * amp
        }
    }

    /** White noise through a one-pole low-pass whose cutoff follows [cut] (0..1) over time. */
    private fun noise(b: FloatArray, at: Float, len: Float, rnd: Random, amp: (Float) -> Float, cut: (Float) -> Float) {
        val s = (at * RATE).toInt()
        val n = (len * RATE).toInt()
        var y = 0f
        for (i in 0 until n) {
            val idx = s + i
            if (idx !in b.indices) break
            val t = i / n.toFloat()
            y += (rnd.nextFloat() * 2 - 1 - y) * cut(t)
            b[idx] += y * amp(t)
        }
    }

    private fun cast(): FloatArray {
        val b = buf(0.5f)
        val r = Random(1)
        // Rod and line swishing through the air.
        noise(b, 0f, 0.5f, r, { t -> sin(PI.toFloat() * t).pow(2) }, { t -> 0.04f + 0.35f * sin(PI.toFloat() * t) })
        // The reel's bail singing as the line pays out.
        for (i in b.indices) {
            val t = i / b.size.toFloat()
            b[i] += sin(TWO_PI * (1800f + 900f * t) * i / RATE) * 0.04f * sin(PI.toFloat() * t)
        }
        return b
    }

    private fun plop(): FloatArray {
        val b = buf(0.35f)
        val r = Random(2)
        noise(b, 0f, 0.08f, r, { t -> (1 - t).pow(3) * 0.5f }, { 0.25f })
        bubble(b, 0.01f, 380f, 820f, 0.16f, 0.8f)
        bubble(b, 0.09f, 900f, 1500f, 0.06f, 0.2f)
        return b
    }

    private fun bite(): FloatArray {
        val b = buf(0.5f)
        val r = Random(3)
        noise(b, 0f, 0.15f, r, { t -> (1 - t).pow(2) * 0.6f }, { 0.18f })
        bubble(b, 0f, 170f, 460f, 0.26f, 1f)
        bubble(b, 0.12f, 520f, 900f, 0.1f, 0.35f)
        bubble(b, 0.2f, 700f, 1200f, 0.07f, 0.2f)
        return b
    }

    /** A burst of ratchet clicks from the reel. */
    private fun reel(): FloatArray {
        val b = buf(0.11f)
        val r = Random(4)
        for (k in 0 until 4) {
            val at = k * 0.026f
            noise(b, at, 0.006f, r, { t -> (1 - t) }, { 0.9f })
            tone(b, at, 0.012f, 2300f + k * 40f, 0.35f, decay = 400f)
        }
        return b
    }

    /** The line creaking under strain. */
    private fun strain(): FloatArray {
        val b = buf(0.45f)
        val r = Random(5)
        var ph = 0f
        for (i in b.indices) {
            val t = i / b.size.toFloat()
            ph += TWO_PI * (95f + 30f * sin(t * 9f)) / RATE
            val saw = (ph / TWO_PI) % 1f * 2 - 1
            val grain = if (r.nextFloat() < 0.3f) 1f else 0.4f
            b[i] = saw * grain * sin(PI.toFloat() * t) * 0.6f
        }
        var y = 0f
        for (i in b.indices) { y += (b[i] - y) * 0.2f; b[i] = y }
        return b
    }

    private fun snap(): FloatArray {
        val b = buf(0.4f)
        val r = Random(6)
        noise(b, 0f, 0.03f, r, { t -> 1 - t }, { 0.95f })
        // The loose line twanging.
        var ph = 0f
        for (i in 0 until (0.35f * RATE).toInt()) {
            val t = i / RATE.toFloat()
            ph += TWO_PI * (1400f * exp(-t * 9f) + 180f) / RATE
            b[i] += sin(ph) * exp(-t * 11f) * 0.5f
        }
        return b
    }

    private fun splash(): FloatArray {
        val b = buf(0.9f)
        val r = Random(7)
        noise(b, 0f, 0.9f, r, { t -> (if (t < 0.03f) t / 0.03f else 1f) * exp(-t * 5f) }, { t -> 0.5f - 0.35f * t })
        repeat(14) {
            val at = r.nextFloat() * 0.55f
            val f0 = 300f + r.nextFloat() * 700f
            bubble(b, at, f0, f0 * (1.6f + r.nextFloat()), 0.04f + r.nextFloat() * 0.08f, 0.15f + r.nextFloat() * 0.25f)
        }
        return b
    }

    private fun coin(): FloatArray {
        val b = buf(0.6f)
        tone(b, 0f, 0.5f, 1318.5f, 0.5f, decay = 9f, harmonics = 0.4f)
        tone(b, 0.08f, 0.5f, 1760f, 0.6f, decay = 7f, harmonics = 0.4f)
        return b
    }

    private fun level(): FloatArray {
        val b = buf(1.3f)
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f)
        notes.forEachIndexed { i, f -> tone(b, i * 0.11f, 0.9f, f, 0.5f, decay = 4f, harmonics = 0.6f) }
        tone(b, 0.44f, 0.85f, 1318.5f, 0.25f, decay = 3.5f, harmonics = 0.3f)
        // A short echo makes it sound like it rings out over the water.
        val d = (0.13f * RATE).toInt()
        for (i in b.size - 1 downTo d) b[i] += b[i - d] * 0.3f
        return b
    }

    /** Small waves lapping against the jetty: slow swells of soft, low noise and the odd bloop. */
    private fun lapping(b: FloatArray, r: Random, amp: Float) {
        var y = 0f
        var y2 = 0f
        for (i in b.indices) {
            val t = i / RATE.toFloat()
            y += (r.nextFloat() * 2 - 1 - y) * 0.06f
            y2 += (y - y2) * 0.12f
            val swell = 0.35f + 0.65f * (0.5f + 0.5f * sin(TWO_PI * 0.375f * t)).pow(3) * (0.6f + 0.4f * sin(TWO_PI * 0.125f * t + 1f))
            b[i] += y2 * swell * amp * 2.2f
        }
        repeat(10) {
            val at = r.nextFloat() * (b.size / RATE.toFloat() - 0.2f)
            val f0 = 250f + r.nextFloat() * 300f
            bubble(b, at, f0, f0 * 1.8f, 0.07f + r.nextFloat() * 0.06f, 0.06f * amp * 2)
        }
    }

    /** Songbirds: quick whistled phrases of rising and falling notes. */
    private fun birds(b: FloatArray, r: Random, phrases: Int) {
        val sec = b.size / RATE.toFloat()
        repeat(phrases) {
            var at = r.nextFloat() * (sec - 1.2f)
            val base = 2600f + r.nextFloat() * 1800f
            val notes = 3 + r.nextInt(5)
            val amp = 0.04f + r.nextFloat() * 0.05f
            repeat(notes) {
                val len = 0.05f + r.nextFloat() * 0.09f
                val f0 = base * (0.8f + r.nextFloat() * 0.5f)
                val f1 = f0 * (0.7f + r.nextFloat() * 0.7f)
                val s = (at * RATE).toInt()
                val n = (len * RATE).toInt()
                var ph = 0f
                for (i in 0 until n) {
                    val idx = s + i
                    if (idx !in b.indices) break
                    val t = i / n.toFloat()
                    ph += TWO_PI * (f0 + (f1 - f0) * t + 120f * sin(TWO_PI * 30f * t)) / RATE
                    b[idx] += sin(ph) * sin(PI.toFloat() * t) * amp
                }
                at += len + 0.02f + r.nextFloat() * 0.06f
            }
        }
    }

    /** Two crickets chirping at their own pace. */
    private fun crickets(b: FloatArray, r: Random) {
        for ((freq, period, amp) in listOf(Triple(4400f, 0.75f, 0.05f), Triple(3900f, 1.0f, 0.035f))) {
            var at = r.nextFloat() * period
            val sec = b.size / RATE.toFloat()
            while (at < sec) {
                repeat(3) { p -> tone(b, at + p * 0.033f, 0.022f, freq, amp, decay = 60f) }
                at += period * (0.9f + r.nextFloat() * 0.2f)
            }
        }
    }

    /** Steady rain: dense hiss with drops pattering on the water and leaves. */
    private fun rain(b: FloatArray, r: Random) {
        var y = 0f
        var lp = 0f
        for (i in b.indices) {
            val w = r.nextFloat() * 2 - 1
            y += (w - y) * 0.5f
            lp += (y - lp) * 0.08f
            b[i] += (y - lp) * 0.35f + lp * 0.25f
        }
        repeat(160) {
            val at = r.nextFloat() * (b.size / RATE.toFloat())
            val f0 = 900f + r.nextFloat() * 2200f
            bubble(b, at, f0, f0 * 1.4f, 0.012f + r.nextFloat() * 0.02f, 0.05f + r.nextFloat() * 0.08f)
        }
    }

    /** A river: constant murmur and many small bubbles in the current. */
    private fun river(b: FloatArray, r: Random) {
        var y = 0f
        var y2 = 0f
        for (i in b.indices) {
            y += (r.nextFloat() * 2 - 1 - y) * 0.15f
            y2 += (y - y2) * 0.3f
            val t = i / RATE.toFloat()
            b[i] += y2 * (0.75f + 0.25f * sin(TWO_PI * 0.25f * t)) * 0.6f
        }
        repeat(70) {
            val at = r.nextFloat() * (b.size / RATE.toFloat())
            val f0 = 400f + r.nextFloat() * 900f
            bubble(b, at, f0, f0 * (1.4f + r.nextFloat() * 0.6f), 0.02f + r.nextFloat() * 0.05f, 0.04f + r.nextFloat() * 0.07f)
        }
    }

    /** Surf: long swells breaking and drawing back over sand. */
    private fun surf(b: FloatArray, r: Random) {
        var y = 0f
        var y2 = 0f
        val sec = b.size / RATE.toFloat()
        for (i in b.indices) {
            val t = i / RATE.toFloat()
            y += (r.nextFloat() * 2 - 1 - y) * 0.35f
            y2 += (y - y2) * 0.1f
            // Two breakers per loop.
            val ph = (t / sec * 2f) % 1f
            val wave = if (ph < 0.15f) ph / 0.15f else exp(-(ph - 0.15f) * 4.5f)
            b[i] += (y * 0.25f + y2) * (0.2f + 0.8f * wave) * 0.9f
        }
    }

    private fun gulls(b: FloatArray, r: Random) {
        val sec = b.size / RATE.toFloat()
        repeat(2) {
            var at = 0.5f + r.nextFloat() * (sec - 2f)
            repeat(2 + r.nextInt(2)) {
                val len = 0.28f
                val s = (at * RATE).toInt()
                val n = (len * RATE).toInt()
                var ph = 0f
                for (i in 0 until n) {
                    val idx = s + i
                    if (idx !in b.indices) break
                    val t = i / n.toFloat()
                    val f = 1300f + 500f * sin(PI.toFloat() * t) - 400f * t
                    ph += TWO_PI * f / RATE
                    // A rough, nasal call: the tone and its overtones, slightly clipped.
                    val v = sin(ph) + 0.6f * sin(2 * ph) + 0.3f * sin(3 * ph)
                    b[idx] += (v / 1.9f).coerceIn(-0.7f, 0.7f) * sin(PI.toFloat() * t) * 0.07f
                }
                at += len + 0.1f
            }
        }
    }
}
