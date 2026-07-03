package com.sunsarradrift

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * SUNSARRA DRIFT — host activity. Controls, as decreed: head movement,
 * TAP, SWIPE forward/back (right temple pad; screen mirrors for testing).
 * Double-tap = settings (tap=next · double=select · in-menu swipe also moves).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var glView: GLSurfaceView
    private lateinit var gaze: GazeCamera
    private lateinit var sfx: Sfx
    private lateinit var music: Music
    private lateinit var voice: VoiceBank
    private lateinit var renderer: GameRenderer
    private lateinit var gestures: GameGestures
    private val menuItemCount = 8

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()

        Game.missionsUnlocked = getSharedPreferences("drift", MODE_PRIVATE)
            .getInt("unlocked", 1).coerceIn(1, Campaign.missions.size)
        Game.missionIdx = Game.missionsUnlocked - 1

        gaze = GazeCamera(this)
        sfx = Sfx(this)
        sfx.preload("pulse", "impact", "explosion", "boltby", "shieldhit", "lockon",
            "missile", "emp", "deny", "uiswitch", "gate", "gatemiss", "boost",
            "alarm", "weld", "fixed", "fuelflow", "winsting", "failsting", "menu")
        music = Music(this)
        voice = VoiceBank(this)
        renderer = GameRenderer(this, gaze, sfx, music, voice)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        setContentView(glView)

        gestures = GameGestures(
            onTap = { onTap() },
            onDoubleTap = { onDoubleTap() },
            onSwipeForward = { onSwipe(true) },
            onSwipeBack = { onSwipe(false) }
        )
    }

    private fun onTap() {
        if (Game.menuOpen) { selectMenuItem(); return }
        Game.actions.add(1)
    }

    private fun onSwipe(forward: Boolean) {
        if (Game.menuOpen) {
            Game.menuIndex = (Game.menuIndex + (if (forward) 1 else menuItemCount - 1)) % menuItemCount
            sfx.play("menu", 0.6f)
            return
        }
        Game.actions.add(if (forward) 2 else 3)
    }

    private fun onDoubleTap() {
        if (!Game.menuOpen) {
            Game.menuIndex = 0
            Game.menuOpen = true
            if (Game.phase == Game.PLAY) Game.paused = true
            music.pause()
            sfx.play("menu", 0.9f)
            return
        }
        closeMenu()
    }

    private fun selectMenuItem() {
        when (Game.menuIndex) {
            0 -> closeMenu()
            1 -> {                                            // restart mission
                Game.menuOpen = false; Game.paused = false
                renderer.engine.start(renderer.engine.mission)
                Game.phase = Game.BRIEFING
            }
            2 -> Game.subtitlesOn = !Game.subtitlesOn
            3 -> { Game.musicVol = step(Game.musicVol); music.applyVolume() }
            4 -> { Game.sfxVol = step(Game.sfxVol); sfx.play("gate") }
            5 -> Game.invertSteer = !Game.invertSteer
            6 -> { gaze.recenter(); Game.say("VIEW RECENTERED", 2000) }
            7 -> {                                            // abandon to title
                Game.menuOpen = false; Game.paused = false
                voice.stop()
                Game.phase = Game.TITLE
                music.play("title")
            }
        }
        sfx.play("menu", 0.8f)
    }

    private fun closeMenu() {
        Game.menuOpen = false
        if (Game.phase == Game.PLAY) { Game.paused = false }
        music.resume()
        sfx.play("menu", 0.7f)
    }

    private fun step(v: Float): Float = when {
        v < 0.2f -> 0.4f; v < 0.5f -> 0.8f; v < 0.9f -> 1.0f; else -> 0.0f
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val name = ev.device?.name ?: ""
        if (name.contains("cyttsp5") || name.contains("cyttsp6")) return gestures.onTouchEvent(ev)
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val name = ev.device?.name ?: ""
        if (name.contains("cyttsp5") || name.contains("cyttsp6")) return gestures.onTouchEvent(ev)
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
        gaze.start()
        if (!Game.menuOpen) music.resume()
        hideSystemUi()
    }

    override fun onPause() {
        glView.onPause()
        gaze.stop()
        if (Game.phase == Game.PLAY) Game.paused = true
        music.pause()
        super.onPause()
    }

    override fun onDestroy() {
        music.stop()
        voice.stop()
        sfx.release()
        super.onDestroy()
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
}
