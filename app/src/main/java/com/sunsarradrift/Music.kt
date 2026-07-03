package com.sunsarradrift

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * Mission scoring: drop Suno-generated MP3s into assets/music/ named by key
 * (see SUNO_PROMPTS.md): act1_battle.mp3, act1_run.mp3, act2_tension.mp3,
 * act3_inferno.mp3, act5_wonder.mp3, act6_liberation.mp3, act7_finale.mp3,
 * plus title.mp3. Missing files = silence; the game shrugs and plays on.
 */
class Music(private val ctx: Context) {
    private var current: MediaPlayer? = null
    private var currentKey = ""

    fun play(key: String) {
        if (key == currentKey && current?.isPlaying == true) return
        currentKey = key
        runCatching {
            current?.release(); current = null
            val f = cache("music/$key.mp3") ?: return
            val p = MediaPlayer()
            p.setDataSource(f.absolutePath)
            p.isLooping = true
            p.setOnPreparedListener {
                p.setVolume(Game.musicVol, Game.musicVol)
                p.start()
            }
            p.prepareAsync()
            current = p
        }
    }

    fun applyVolume() = runCatching { current?.setVolume(Game.musicVol, Game.musicVol) }
    fun pause() = runCatching { if (current?.isPlaying == true) current?.pause() }
    fun resume() = runCatching { current?.start() }
    fun stop() { runCatching { current?.release() }; current = null; currentKey = "" }

    private fun cache(assetPath: String): File? = try {
        val out = File(ctx.cacheDir, assetPath.replace('/', '_'))
        if (!out.exists() || out.length() == 0L) {
            ctx.assets.open(assetPath).use { i -> out.outputStream().use { i.copyTo(it) } }
        }
        out
    } catch (e: Exception) { null }
}
