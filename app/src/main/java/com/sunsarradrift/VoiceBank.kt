package com.sunsarradrift

import android.content.Context
import android.media.MediaPlayer
import org.json.JSONObject
import java.io.File

/**
 * Voiced dialog: script.json is the single source of truth (id -> speaker+text);
 * clips live in assets/voice/<id>.ogg (rendered once by tools/generate_dialogue.py).
 * Captions always work, even before the clips exist — a missing clip simply
 * shows its subtitle for a reading-length beat.
 */
class VoiceBank(private val ctx: Context) {
    class Line(val speaker: String, val text: String)

    val lines = HashMap<String, Line>()
    private var mp: MediaPlayer? = null
    @Volatile var speaking = false; private set
    @Volatile var lineEndsAt = 0L; private set
    @Volatile private var lastRadioAt = 0L

    val speakerNames = mapOf(
        "SUN" to "SUNSARRA", "YU" to "YU", "ADA" to "DR. OBI",
        "TERN" to "TERN", "CASQ" to "CASQUE", "MOM" to "DR. VEX")

    init {
        runCatching {
            val root = JSONObject(ctx.assets.open("script.json")
                .bufferedReader().use { it.readText() })
            val ls = root.getJSONObject("lines")
            for (k in ls.keys()) {
                val o = ls.getJSONObject(k)
                lines[k] = Line(o.getString("s"), o.getString("t"))
            }
        }
    }

    private fun strip(t: String) =
        t.replace(Regex("\\[[^\\]]*\\]"), "").replace(Regex("\\s+"), " ").trim()

    /** Plays a story line (cutscenes): captions + audio; returns expected end time. */
    fun play(id: String): Long {
        val line = lines[id] ?: return System.currentTimeMillis()
        Game.captionSpeaker = speakerNames[line.speaker] ?: line.speaker
        Game.caption = strip(line.text)
        stop()
        val f = cache("voice/$id.ogg")
        val now = System.currentTimeMillis()
        if (f == null) {                                 // clip not rendered yet: timed caption
            lineEndsAt = now + (1800 + Game.caption.length * 45L)
            speaking = false
            return lineEndsAt
        }
        return runCatching {
            val p = MediaPlayer()
            mp = p
            speaking = true
            lineEndsAt = now + 8000
            p.setDataSource(f.absolutePath)
            p.setOnPreparedListener {
                val v = Game.sfxVol
                p.setVolume(v, v)
                lineEndsAt = System.currentTimeMillis() + p.duration + 250
                p.start()
            }
            p.setOnCompletionListener { speaking = false }
            p.setOnErrorListener { _, _, _ -> speaking = false; true }
            p.prepareAsync()
            lineEndsAt
        }.getOrElse { speaking = false; now + 2500 }
    }

    /** Combat radio: short lines, global 9s cooldown, never over a story line. */
    fun radio(pool: List<String>, cooldownMs: Long = 9000): Boolean {
        val now = System.currentTimeMillis()
        if (speaking || now < lineEndsAt || now - lastRadioAt < cooldownMs) return false
        lastRadioAt = now
        play(pool.random())
        return true
    }

    fun stop() {
        runCatching { mp?.release() }
        mp = null
        speaking = false
    }

    private fun cache(assetPath: String): File? = try {
        val out = File(ctx.cacheDir, assetPath.replace('/', '_'))
        if (!out.exists() || out.length() == 0L) {
            ctx.assets.open(assetPath).use { i -> out.outputStream().use { i.copyTo(it) } }
        }
        out
    } catch (e: Exception) { null }
}
