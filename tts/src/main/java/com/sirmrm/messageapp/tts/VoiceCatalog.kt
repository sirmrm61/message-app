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
        val realDataDir = ensureDataDir(context, dataDir)
        return realDataDir to voices
    }

    /**
     * espeak-ng reads its data files with fopen(), so the bundled assets must be
     * copied to the filesystem once. Returns the absolute dir on disk.
     */
    private fun ensureDataDir(context: Context, assetDir: String): String {
        val out = java.io.File(context.filesDir, "espeak-ng-data")
        if (out.isDirectory && java.io.File(out, "phondata").isFile) return out.absolutePath
        copyAssetDir(context, assetDir, out)
        return out.absolutePath
    }

    private fun copyAssetDir(context: Context, assetPath: String, outDir: java.io.File) {
        val entries = context.assets.list(assetPath) ?: return
        if (entries.isEmpty()) {
            outDir.parentFile?.mkdirs()
            context.assets.open(assetPath).use { inp ->
                outDir.outputStream().use { inp.copyTo(it) }
            }
            return
        }
        outDir.mkdirs()
        for (name in entries) {
            copyAssetDir(context, "$assetPath/$name", java.io.File(outDir, name))
        }
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
