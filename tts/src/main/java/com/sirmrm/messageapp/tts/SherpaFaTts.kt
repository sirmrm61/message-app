package com.sirmrm.messageapp.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.sirmrm.messageapp.classifier.tts.TtsTextPreprocessor
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * sherpa-onnx (Apache-2.0) wrapper: lazily loads one int8 Piper fa voice from
 * assets, synthesizes sentence chunks on a worker thread and streams PCM into
 * an AudioTrack. Fully offline. Falls back to null when the voice cannot load.
 */
class SherpaFaTts private constructor(
    private val context: Context,
    private val voice: Voice,
    private val dataDir: String,
) {

    @Volatile private var tts: OfflineTts? = null
    @Volatile private var sampleRate: Int = 22050
    private val playing = AtomicBoolean(false)
    @Volatile var lengthScale: Float = 1.0f
    @Volatile var volume: Float = 1.0f
    /** Max queued chunks waiting to speak. */
    private val queue = ConcurrentLinkedQueue<String>()
    @Volatile private var worker: Thread? = null

    private fun load(): Boolean {
        tts?.let { return true }
        synchronized(this) {
            if (tts != null) return true
            if (!VoiceCatalog.verifyModel(context, voice)) return false
            return runCatching {
                val cfg = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = voice.model,
                            lexicon = "",
                            tokens = voice.tokens,
                            dataDir = dataDir,
                        ),
                        numThreads = 2,
                        debug = false,
                        provider = "cpu",
                    ),
                )
                val engine = OfflineTts(context.assets, cfg)
                tts = engine
                sampleRate = engine.sampleRate()
                true
            }.getOrDefault(false)
        }
    }

    /** Synthesize + play text on a background thread; calls onDone at the end. */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!load()) { onDone?.invoke(); return }
        queue.clear()
        queue.addAll(TtsTextPreprocessor.preprocess(text))
        if (queue.isEmpty()) { onDone?.invoke(); return }
        startWorker(onDone)
    }

    /** Queue several texts back-to-back (auto-read), capped at [maxItems]. */
    fun speakAll(texts: List<String>, maxItems: Int = 3, onDone: (() -> Unit)? = null) {
        if (!load()) { onDone?.invoke(); return }
        queue.clear()
        texts.take(maxItems).forEach { queue.addAll(TtsTextPreprocessor.preprocess(it)) }
        if (queue.isEmpty()) { onDone?.invoke(); return }
        startWorker(onDone)
    }

    private fun startWorker(onDone: (() -> Unit)?) {
        worker?.interrupt()
        worker = Thread {
            playing.set(true)
            val track = newTrack()
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val focus = requestFocus(am)
            try {
                track?.play()
                while (playing.get() && !Thread.currentThread().isInterrupted) {
                    val sentence = queue.poll() ?: break
                    val engine = tts ?: break
                    val audio: GeneratedAudio = runCatching {
                        engine.generate(sentence, 0, 1f / lengthScale.coerceIn(0.5f, 2f))
                    }.getOrNull() ?: continue
                    writeSamples(track, audio.samples, volume)
                    // ~200ms trailing silence — Piper fa clips the last syllable.
                    writeSilence(track, audio.sampleRate / 5)
                }
            } finally {
                runCatching { track?.stop(); track?.release() }
                abandonFocus(am, focus)
                playing.set(false)
                onDone?.invoke()
            }
        }.also { it.start() }
    }

    private fun newTrack(): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(sampleRate * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }.getOrNull()

    private fun writeSamples(track: AudioTrack?, samples: FloatArray, vol: Float) {
        track ?: return
        val pcm = ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1f, 1f) * vol.coerceIn(0f, 1f) * Short.MAX_VALUE).toInt()
                .toShort()
        }
        var off = 0
        while (off < pcm.size && playing.get() && !Thread.currentThread().isInterrupted) {
            val n = track.write(pcm, off, pcm.size - off)
            if (n <= 0) break
            off += n
        }
    }

    private fun writeSilence(track: AudioTrack?, frames: Int) {
        track ?: return
        val pcm = ShortArray(frames.coerceAtMost(sampleRate))
        var off = 0
        while (off < pcm.size && playing.get()) {
            val n = track.write(pcm, off, pcm.size - off)
            if (n <= 0) break
            off += n
        }
    }

    private fun requestFocus(am: AudioManager): AudioFocusRequest? {
        return if (Build.VERSION.SDK_INT >= 26) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                ).build().also { am.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            null
        }
    }

    private fun abandonFocus(am: AudioManager, focus: AudioFocusRequest?) {
        if (Build.VERSION.SDK_INT >= 26 && focus != null) {
            am.abandonAudioFocusRequest(focus)
        } else {
            @Suppress("DEPRECATION") am.abandonAudioFocus(null)
        }
    }

    fun stop() {
        queue.clear()
        playing.set(false)
        worker?.interrupt()
    }

    fun release() {
        stop()
        runCatching { tts?.release() }
        tts = null
    }

    companion object {
        fun create(context: Context, voiceId: String = "voice1"): SherpaFaTts? {
            val (dataDir, voices) = runCatching { VoiceCatalog.load(context) }
                .getOrNull() ?: return null
            val voice = voices.firstOrNull { it.id == voiceId } ?: voices.firstOrNull()
                ?: return null
            return SherpaFaTts(context.applicationContext, voice, dataDir)
        }
    }
}
