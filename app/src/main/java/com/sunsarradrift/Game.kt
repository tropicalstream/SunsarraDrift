package com.sunsarradrift

/**
 * SUNSARRA DRIFT — shared blackboard between input, mission logic and the
 * renderer. One young pilot, a stolen mother, and an empire that thinks
 * people can be sorted. All @Volatile primitives; no locks on the hot path.
 */
object Game {
    // phases
    const val TITLE = 0
    const val CUTSCENE = 1      // story cards (letterboxed, voiced)
    const val BRIEFING = 2      // mission card: role, objective, homage note
    const val PLAY = 3
    const val DEBRIEF = 4       // win: outro cutscene queued; loss: retry card

    const val ROLE_GUNNER = 0
    const val ROLE_PILOT = 1
    const val ROLE_ENGINEER = 2

    @Volatile var phase = TITLE
    @Volatile var paused = false
    @Volatile var missionIdx = 0
    @Volatile var missionsUnlocked = 1
    @Volatile var won = false

    // player ship vitals
    @Volatile var hull = 100f
    @Volatile var shield = 100f
    @Volatile var lastHitAt = 0L

    // objective HUD (set by MissionEngine)
    @Volatile var objectiveText = ""
    @Volatile var progress = 0f            // 0..1 toward mission goal
    @Volatile var weaponName = ""
    @Volatile var lockFrac = 0f            // missile lock 0..1
    @Volatile var pursuerDist = 1f         // pilot: 0 = caught
    @Volatile var boostCd = 0f

    // cutscene runtime
    @Volatile var sceneLines: List<String> = emptyList()   // line ids into script.json
    @Volatile var sceneIdx = 0
    @Volatile var sceneAfter = TITLE       // phase to enter when the scene ends

    // HUD text
    @Volatile var caption = ""             // subtitle of the current voice line
    @Volatile var captionSpeaker = ""
    @Volatile var message = ""
    @Volatile var messageUntil = 0L
    @Volatile var flash = 0f
    @Volatile var letterbox = 0f           // eased by renderer

    // settings
    @Volatile var menuOpen = false
    @Volatile var menuIndex = 0
    @Volatile var subtitlesOn = true
    @Volatile var musicVol = 0.8f
    @Volatile var sfxVol = 1.0f
    @Volatile var invertSteer = false

    // input actions (main thread -> game thread)
    // 1 tap, 2 swipe-forward, 3 swipe-back
    val actions = java.util.concurrent.ConcurrentLinkedQueue<Int>()

    fun say(text: String, ms: Long = 2400) {
        message = text
        messageUntil = System.currentTimeMillis() + ms
    }

    fun damage(amount: Float) {
        lastHitAt = System.currentTimeMillis()
        if (shield > 0f) {
            shield = (shield - amount).coerceAtLeast(0f)
        } else {
            hull = (hull - amount).coerceAtLeast(0f)
        }
        flash = (flash + 0.25f).coerceAtMost(0.8f)
    }
}
