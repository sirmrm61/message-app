package com.sirmrm.messageapp.tts

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** One entry of assets/tts/voices.json — add a voice = folder + entry. */
data class Voice(
    val id: String,
    val label: String,
    val desc: String,
    val model: String,
    val modelJson: String,
    val tokens: String,
    val sha256: String,
    val source: String,
    val license: String,
)

object VoiceCatalog {

    /** All bundled voices + the shared espeak data dir, from voices.json. */
    fun load(context: Context): Pair<String, List<Voice>> {
        val root = context.assets.open("tts/voices.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(root)
        val dataDir = obj.getString("dataDir")
        val voices = obj.getJSONArray("voices").let { arr ->
            (0 until arr.length()).map { i ->
                val v = arr.getJSONObject(i)
                Voice(
                    id = v.getString("id"),
                    label = v.getString("label"),
                    desc = v.getString("desc"),
                    model = v.getString("model"),
                    modelJson = v.getString("modelJson"),
                    tokens = v.getString("tokens"),
                    sha256 = v.getString("sha256"),
                    source = v.getString("source"),
                    license = v.getString("license"),
                )
            }
        }
        return dataDir to voices
    }

    /** SHA-256 of the bundled model — checked before each load (build-time trust). */
    fun verifyModel(context: Context, voice: Voice): Boolean = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        context.assets.open(voice.model).use { input ->
            val buf = ByteArray(1 shl 20)
            var n: Int
            while (input.read(buf).also { n = it } >= 0) md.update(buf, 0, n)
        }
        md.digest().joinToString("") { "%02x".format(it) } == voice.sha256
    }.getOrDefault(false)
}
