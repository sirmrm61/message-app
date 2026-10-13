package com.sirmrm.messageapp.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.sirmrm.messageapp.classifier.tts.TtsTextPreprocessor

/**
 * Holds the single TTS engine instance. Uses sherpa-onnx Piper voices when their
 * model files are present on disk, otherwise falls back to the system TTS if it
 * has a Persian voice. Fully offline either way.
 */
object TtsEngineHolder {

    @Volatile private var systemTts: TextToSpeech? = null
    @Volatile private var sherpa: SherpaFaTts? = null
    @Volatile private var loadedVoiceId: String? = null

    /** Lazily creates the sherpa engine (loads models on first use only);
     *  recreates it when a different bundled voice is requested. */
    private fun sherpa(context: Context, voiceId: String): SherpaFaTts? {
        if (sherpa != null && loadedVoiceId == voiceId) return sherpa
        synchronized(this) {
            if (sherpa != null && loadedVoiceId == voiceId) return sherpa
            sherpa?.release()
            sherpa = SherpaFaTts.create(context, voiceId)
            loadedVoiceId = if (sherpa != null) voiceId else null
            return sherpa
        }
    }

    /** Drop the cached engine when the user picks a different voice. */
    fun switchVoice() {
        synchronized(this) {
            sherpa?.release()
            sherpa = null
        }
    }

    fun speak(context: Context, text: String, voiceId: String = "voice1",
              speed: Float = 1f, volume: Float = 1f, onDone: (() -> Unit)? = null) {
        val engine = sherpa(context.applicationContext, voiceId)
        if (engine != null) {
            engine.lengthScale = speed
            engine.volume = volume
            engine.speak(text, onDone)
            return
        }
        // Fallback: system TTS with a Persian locale, if any is installed.
        val ctx = context.applicationContext
        val tts = systemTts ?: synchronized(this) {
            systemTts ?: TextToSpeech(ctx) { }.also { systemTts = it }
        }
        runCatching {
            val fa = Locale("fa", "IR")
            if (tts.isLanguageAvailable(fa) >= TextToSpeech.LANG_AVAILABLE) {
                tts.language = fa
                tts.speak(TtsTextPreprocessor.preprocess(text).joinToString(" "), TextToSpeech.QUEUE_FLUSH, null, "sms")
            }
        }
        onDone?.invoke()
    }

    fun speakAll(context: Context, texts: List<String>, voiceId: String = "voice1",
                 speed: Float = 1f, volume: Float = 1f, maxItems: Int = 3,
                 onDone: (() -> Unit)? = null) {
        val engine = sherpa(context.applicationContext, voiceId)
        if (engine != null) {
            engine.lengthScale = speed
            engine.volume = volume
            engine.speakAll(texts, maxItems, onDone)
            return
        }
        onDone?.invoke()
    }

    fun stop() {
        sherpa?.stop()
        runCatching { systemTts?.stop() }
    }

    /** Release the sherpa models (called after idle minutes or master off). */
    fun release() {
        synchronized(this) {
            sherpa?.release()
            sherpa = null
        }
    }
}
