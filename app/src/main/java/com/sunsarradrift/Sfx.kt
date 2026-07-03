package com.sunsarradrift

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File

/** SoundPool wrapper for the procedural SFX in assets/sfx. */
class Sfx(private val ctx: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(10)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = HashMap<String, Int>()

    fun preload(vararg names: String) {
        for (n in names) {
            val f = cache("sfx/$n.ogg") ?: continue
            ids[n] = pool.load(f.absolutePath, 1)
        }
    }

    fun play(name: String, vol: Float = 1f, rate: Float = 1f) {
        val v = vol * Game.sfxVol
        val id = ids[name] ?: return
        if (v > 0.01f) pool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    private fun cache(assetPath: String): File? = try {
        val out = File(ctx.cacheDir, assetPath.replace('/', '_'))
        if (!out.exists() || out.length() == 0L) {
            ctx.assets.open(assetPath).use { i -> out.outputStream().use { i.copyTo(it) } }
        }
        out
    } catch (e: Exception) { null }

    fun release() = pool.release()
}
