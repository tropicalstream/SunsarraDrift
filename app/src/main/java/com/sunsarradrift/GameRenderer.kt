package com.sunsarradrift

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.sunsarradrift.gl.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sunsarra Drift — stereo renderer + game director. Pale Blue's galaxy
 * (planets, HYG stars, dust, rocks, nebula) is the battlefield; the mission
 * engine plays out on top of it. Head = view/aim/steer; the renderer routes
 * taps/swipes to the current seat and turns engine events into light & sound.
 */
class GameRenderer(
    private val ctx: Context,
    private val gaze: GazeCamera,
    private val sfx: Sfx,
    private val music: Music,
    private val voice: VoiceBank
) : GLSurfaceView.Renderer {

    companion object { const val EYE_OFFSET = 0.032f }

    private val world = SolarSystem(ctx)
    val engine = MissionEngine(world)
    private val planetShader = PlanetShader()
    private val starShader = StarShader()
    private val skybox = Skybox()
    private val nebula = Nebula()
    private val dust = DustField()
    private val rocks = RockField()
    private val hull3d = ShipHull()
    private val craft = Craft()
    private val fx = LineBatch(6000)
    private val hudFx = LineBatch(1500)
    private val letterbox = Letterbox()
    private val captionText = HudText(1400, 512, 46f, maxChars = 42, maxLines = 5)
    private val speakerText = HudText(512, 128, 40f, maxChars = 18, maxLines = 1)
    private val objText = HudText(1024, 192, 44f, maxChars = 34, maxLines = 2)
    private val msgText = HudText(1024, 256, 60f, maxChars = 26, maxLines = 3)
    private val cardText = HudText(1400, 768, 50f, maxChars = 40, maxLines = 10)
    private val bigText = HudText(1200, 384, 92f, maxChars = 18, maxLines = 3)
    private lateinit var sphere: Sphere
    private var hyg: HygStars? = null

    private var width = 0; private var height = 0
    private var timeSec = 0f
    private var lastNs = 0L
    private var letterboxAmt = 0f
    private val prevCam = FloatArray(3)
    private var havePrev = false
    private val shipVel = FloatArray(3)
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val viewNT = FloatArray(16)
    private val vpNT = FloatArray(16)
    private val hudVp = FloatArray(16)
    private val rgb = FloatArray(3)
    private val portraits = HashMap<String, Int>()
    private val cinematic = TitleCinematic()
    private var titleStartMs = System.currentTimeMillis()
    private var lastPhase = -1
    private var lineAdvanceAt = 0L

    // ---------------- lifecycle ----------------
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        sphere = Sphere(24, 48)
        planetShader.init(); starShader.init(); skybox.init(); nebula.init()
        dust.init(); rocks.init(world); hull3d.init(); craft.init()
        fx.init(); hudFx.init(); letterbox.init()
        captionText.init(); speakerText.init(); objText.init(); msgText.init()
        cardText.init(); bigText.init()
        world.initGl()
        hyg = HygStars(ctx)
        for (sp in arrayOf("SUN", "YU", "ADA", "TERN", "CASQ", "MOM")) {
            portraits[sp] = GlAssets.loadTexture(ctx, "cutscenes/$sp.png")
        }
        Matrix.orthoM(hudVp, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        lastNs = System.nanoTime()
        music.play("title")
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) { width = w; height = h }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1e9f).coerceIn(0f, 0.08f)
        lastNs = now
        timeSec += dt
        Game.flash = (Game.flash - dt * 2f).coerceAtLeast(0f)
        if (Game.phase != lastPhase) {
            if (Game.phase == Game.TITLE) titleStartMs = System.currentTimeMillis()
            lastPhase = Game.phase
        }
        val lbTarget = if (Game.phase == Game.CUTSCENE || Game.phase == Game.TITLE) 1f else 0f
        letterboxAmt += (lbTarget - letterboxAmt) * (1f - exp(-dt * 4f))
        Game.letterbox = letterboxAmt

        // ship frame — during TITLE the cinematic director drives the camera,
        // shot-listed to the title song's lyrics (head stays free on top)
        var camU = engine.missionU
        var extraYaw = 0f
        var extraPitch = 0f
        if (Game.phase == Game.TITLE) {
            val tSong = (System.currentTimeMillis() - titleStartMs) / 1000f
            cinematic.update(tSong)
            camU = cinematic.state.u
            extraYaw = cinematic.state.yawOff
            extraPitch = cinematic.state.pitchOff
        }
        val camPos = engine.railPoint(camU,
            if (engine.mission.role == Game.ROLE_PILOT && Game.phase == Game.PLAY) engine.latX else 0f,
            if (engine.mission.role == Game.ROLE_PILOT && Game.phase == Game.PLAY) engine.latY else 0f)
        val tangent = world.tangentAt(camU)
        val shipYaw = atan2(tangent[0], -tangent[2]) + extraYaw
        val shipPitch = asin(tangent[1].coerceIn(-1f, 1f))
        val yaw = shipYaw + gaze.yaw
        val pitch = (shipPitch + extraPitch + gaze.pitch).coerceIn(-1.5f, 1.5f)
        val dir = floatArrayOf(sin(yaw) * cos(pitch), sin(pitch), -cos(yaw) * cos(pitch))
        val right = norm(cross(dir, floatArrayOf(0f, 1f, 0f)))

        consumeActions(camPos, dir)
        if (Game.phase == Game.PLAY && !Game.paused && !Game.menuOpen) {
            engine.update(dt, camPos, shipYaw, dir, gaze.yaw, gaze.pitch)
        }
        drainEngineEvents()
        advanceCutscene()

        if (havePrev && dt > 1e-4f) {
            shipVel[0] = (camPos[0] - prevCam[0]) / dt
            shipVel[1] = (camPos[1] - prevCam[1]) / dt
            shipVel[2] = (camPos[2] - prevCam[2]) / dt
        }
        prevCam[0] = camPos[0]; prevCam[1] = camPos[1]; prevCam[2] = camPos[2]
        havePrev = true

        val ds = dist(camPos, SolarSystem.SUN_POS) / SolarSystem.AU_WORLD
        val sunlight = (1f / (ds * ds)).coerceIn(0.015f, 4f)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val half = width / 2
        val eyeAspect = half.toFloat() / height
        Matrix.perspectiveM(proj, 0, 58f, eyeAspect, 0.2f, 1500f)
        drawEye(0, half, camPos, dir, right, tangent, sunlight, -EYE_OFFSET, shipYaw, shipPitch, dt * 0.5f)
        drawHud(0, half, eyeAspect, -1f)
        drawEye(half, width - half, camPos, dir, right, tangent, sunlight, +EYE_OFFSET, shipYaw, shipPitch, dt * 0.5f)
        drawHud(half, width - half, eyeAspect, +1f)
    }

    // ---------------- world ----------------
    private fun drawEye(x: Int, w: Int, camPos: FloatArray, dir: FloatArray, right: FloatArray,
                        tangent: FloatArray, sunlight: Float, eyeOff: Float,
                        shipYaw: Float, shipPitch: Float, dustDt: Float) {
        GLES20.glViewport(x, 0, w, height)
        val ex = camPos[0] + right[0] * eyeOff
        val ey = camPos[1] + right[1] * eyeOff
        val ez = camPos[2] + right[2] * eyeOff
        Matrix.setLookAtM(view, 0, ex, ey, ez,
            ex + dir[0] - right[0] * eyeOff * 0.35f,
            ey + dir[1] - right[1] * eyeOff * 0.35f,
            ez + dir[2] - right[2] * eyeOff * 0.35f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
        System.arraycopy(view, 0, viewNT, 0, 16)
        viewNT[12] = 0f; viewNT[13] = 0f; viewNT[14] = 0f
        Matrix.multiplyMM(vpNT, 0, proj, 0, viewNT, 0)

        skybox.draw(vpNT, smooth(16f, 23f, engine.missionU))
        hyg?.let { starShader.draw(it.buffer, it.count, vpNT, 0f, tangent) }
        if (dist(camPos, world.nebulaCenter) < 500f) {
            val up = norm(cross(right, dir))
            nebula.draw(vp, world.nebulaCenter, right, up, world.nebulaSize, timeSec)
        }
        dust.updateAndDraw(vp, floatArrayOf(ex, ey, ez), shipVel, dustDt, 0f)
        rocks.draw(vp, floatArrayOf(ex, ey, ez), SolarSystem.SUN_POS, sunlight, timeSec)
        world.drawBodies(planetShader, sphere, vp, floatArrayOf(ex, ey, ez), sunlight, timeSec)
        hull3d.draw(vp, camPos, shipYaw, shipPitch, SolarSystem.SUN_POS, sunlight,
            0.4f, 0f)

        fx.begin()
        emitMissionFx(camPos, shipYaw)
        drawCraft(vp, camPos, sunlight)
        if (Game.phase == Game.TITLE) {
            cinematic.emitExtras(fx, craft, vp, camPos, sunlight, world,
                (System.currentTimeMillis() - titleStartMs) / 1000f)
        }
        fx.draw(vp)
    }

    private fun drawCraft(vp: FloatArray, camPos: FloatArray, sunlight: Float) {
        val m = engine.mission
        if (Game.phase != Game.PLAY && Game.phase != Game.DEBRIEF) return
        if (m.role == Game.ROLE_GUNNER) {
            for (e in engine.enemies) {
                if (!e.alive) continue
                val f = norm(floatArrayOf(e.vel[0], e.vel[1], e.vel[2] + 1e-4f))
                craft.buildModel(e.pos, f, 0.3f * sin(timeSec + e.orbitA), e.scale)
                val disabled = e.disabledUntil > engine.elapsedSec
                craft.draw(vp, e.mesh, camPos, SolarSystem.SUN_POS, sunlight,
                    floatArrayOf(0.30f, 0.30f, 0.36f), if (disabled) 0f else 0.6f)
            }
            if (engine.hasAlly) {
                craft.buildModel(engine.allyPos, floatArrayOf(0f, 0f, -1f), 0f, 2.2f)
                craft.draw(vp, 1, camPos, SolarSystem.SUN_POS, sunlight,
                    floatArrayOf(0.35f, 0.62f, 0.60f), 0f)
            }
        }
        if (m.role == Game.ROLE_PILOT) {
            // the pursuing lance looms behind, closer as pursuerDist falls
            val back = engine.railPoint(engine.missionU - 0.10f - Game.pursuerDist * 0.25f, 0f, 2f)
            val f = world.tangentAt(engine.missionU)
            craft.buildModel(back, f, 0f, 3.4f)
            craft.draw(vp, 2, camPos, SolarSystem.SUN_POS, sunlight,
                floatArrayOf(0.32f, 0.28f, 0.34f), 0.35f)
        }
    }

    /** Vector-light layer: bolts, tracers, gates, mines, stations, reticle rings. */
    private fun emitMissionFx(camPos: FloatArray, shipYaw: Float) {
        if (Game.phase != Game.PLAY) return
        val m = engine.mission
        when (m.role) {
            Game.ROLE_GUNNER -> {
                for (b in engine.bolts) {
                    if (b.ttl <= 0f) continue
                    val r = if (b.friendly) 0.4f else 1f
                    val g = if (b.friendly) 1f else 0.25f
                    fx.glow(b.pos[0], b.pos[1], b.pos[2], 9f, r, g, 0.3f, 0.9f)
                    fx.line(b.pos[0], b.pos[1], b.pos[2],
                        b.pos[0] - b.vel[0] * 0.05f, b.pos[1] - b.vel[1] * 0.05f, b.pos[2] - b.vel[2] * 0.05f,
                        r, g, 0.3f, 0.7f)
                }
                for (mi in engine.missiles) {
                    if (!mi.live) continue
                    fx.glow(mi.pos[0], mi.pos[1], mi.pos[2], 12f, 1f, 0.8f, 0.4f, 0.95f)
                }
                if (engine.tracerTtl > 0f) {
                    val t = engine.lastTracer
                    fx.line(t[0], t[1], t[2], t[3], t[4], t[5], 0.45f, 0.95f, 1f, 0.9f)
                }
            }
            Game.ROLE_PILOT -> {
                for (i in 0 until engine.gateCount) {
                    val g = engine.gates[i]
                    if (g.passed || g.missed) continue
                    val c = engine.railPoint(g.u, g.ox, g.oy)
                    val nextGate = engine.gates.take(engine.gateCount)
                        .firstOrNull { !it.passed && !it.missed } === g
                    if (nextGate) GlUtilHue(0.32f) else GlUtilHue(0.55f)
                    ring(c, g.r, if (nextGate) 1f else 0.4f)
                }
                for (mn in engine.mines) {
                    if (!mn.live) continue
                    fx.glow(mn.pos[0], mn.pos[1], mn.pos[2], 10f, 1f, 0.2f, 0.15f,
                        0.6f + 0.4f * sin(timeSec * 6f))
                }
            }
            else -> {
                // engineer stations: panels ringed around the ship at their yaw
                for ((i, st) in engine.stations.withIndex()) {
                    val a = shipYaw + st.yaw
                    val px = camPos[0] + sin(a) * 5f
                    val py = camPos[1] + 0.2f
                    val pz = camPos[2] - cos(a) * 5f
                    val sel = engine.lookStation == i
                    val red = st.fault && sin(st.flashT * 8f) > 0f
                    val cr = if (red) 1f else if (sel) 0.5f else 0.25f
                    val cg = if (red) 0.2f else if (sel) 1f else 0.6f
                    val cb = if (red) 0.15f else 0.8f
                    boxAt(px, py, pz, 1.1f, cr, cg, cb, if (sel) 1f else 0.55f)
                    if (st.fault && st.repair > 0f) {
                        fx.line(px - 1f, py - 1.4f, pz, px - 1f + 2f * st.repair, py - 1.4f, pz,
                            0.4f, 1f, 0.5f, 1f)
                    }
                }
                if (engine.fuelFlow) {
                    val a = shipYaw + engine.stations[5].yaw
                    fx.line(camPos[0] + sin(a) * 5f, camPos[1], camPos[2] - cos(a) * 5f,
                        camPos[0] + sin(a) * 40f, camPos[1] + 3f, camPos[2] - cos(a) * 40f,
                        1f, 0.75f, 0.3f, 0.5f + 0.5f * sin(timeSec * 10f))
                }
            }
        }
    }

    private fun GlUtilHue(h: Float) {
        val x = (h - kotlin.math.floor(h)) * 6f
        val i = x.toInt() % 6; val f = x - i; val q = 1f - f
        val (r, g, b) = when (i) {
            0 -> Triple(1f, f, 0f); 1 -> Triple(q, 1f, 0f); 2 -> Triple(0f, 1f, f)
            3 -> Triple(0f, q, 1f); 4 -> Triple(f, 0f, 1f); else -> Triple(1f, 0f, q)
        }
        rgb[0] = r; rgb[1] = g; rgb[2] = b
    }

    private fun ring(c: FloatArray, r: Float, a: Float) {
        val n = 20
        for (i in 0 until n) {
            val a0 = i / n.toFloat() * 6.2832f; val a1 = (i + 1) / n.toFloat() * 6.2832f
            fx.line(c[0] + cos(a0) * r, c[1] + sin(a0) * r, c[2],
                c[0] + cos(a1) * r, c[1] + sin(a1) * r, c[2], rgb[0], rgb[1], rgb[2], a)
        }
    }

    private fun boxAt(x: Float, y: Float, z: Float, r: Float,
                      cr: Float, cg: Float, cb: Float, a: Float) {
        fx.line(x - r, y - r, z, x + r, y - r, z, cr, cg, cb, a)
        fx.line(x + r, y - r, z, x + r, y + r, z, cr, cg, cb, a)
        fx.line(x + r, y + r, z, x - r, y + r, z, cr, cg, cb, a)
        fx.line(x - r, y + r, z, x - r, y - r, z, cr, cg, cb, a)
        fx.glow(x, y, z, 8f, cr, cg, cb, a * 0.5f)
    }

    // ---------------- HUD ----------------
    private fun drawHud(x: Int, w: Int, eyeAspect: Float, eyeSign: Float) {
        GLES20.glViewport(x, 0, w, height)
        val par = eyeSign * 0.006f
        val now = System.currentTimeMillis()

        if (Game.phase == Game.PLAY) {
            hudFx.begin()
            // reticle
            if (engine.mission.role == Game.ROLE_GUNNER) {
                val s = 0.035f
                hudFx.line(-s, 0f, 0f, -s * 0.3f, 0f, 0f, 0.5f, 1f, 0.9f, 0.9f)
                hudFx.line(s * 0.3f, 0f, 0f, s, 0f, 0f, 0.5f, 1f, 0.9f, 0.9f)
                hudFx.line(0f, -s, 0f, 0f, -s * 0.3f, 0f, 0.5f, 1f, 0.9f, 0.9f)
                hudFx.line(0f, s * 0.3f, 0f, 0f, s, 0f, 0.5f, 1f, 0.9f, 0.9f)
                if (Game.lockFrac > 0f) {
                    val lr = 0.09f * (1.5f - Game.lockFrac * 0.5f)
                    val col = if (Game.lockFrac >= 1f) floatArrayOf(1f, 0.3f, 0.3f) else floatArrayOf(1f, 0.8f, 0.3f)
                    val n = (Game.lockFrac * 16).toInt().coerceAtLeast(2)
                    for (i in 0 until n) {
                        val a0 = i / 16f * 6.2832f; val a1 = (i + 1) / 16f * 6.2832f
                        hudFx.line(cos(a0) * lr / eyeAspect, sin(a0) * lr, 0f,
                            cos(a1) * lr / eyeAspect, sin(a1) * lr, 0f, col[0], col[1], col[2], 0.9f)
                    }
                }
            }
            // hull + shield bars (bottom left)
            bar(-0.92f, -0.80f, Game.shield / 100f, 0.3f, 0.7f, 1f)
            bar(-0.92f, -0.87f, Game.hull / 100f, 1f, 0.45f, 0.3f)
            // objective progress (top)
            bar(-0.35f, 0.90f, Game.progress, 0.5f, 1f, 0.6f, width2 = 0.7f)
            if (engine.mission.role == Game.ROLE_PILOT) {
                bar(-0.35f, 0.84f, Game.pursuerDist, 1f, 0.5f, 0.2f, width2 = 0.7f)
            }
            hudFx.draw(hudVp)

            objText.setText(Game.objectiveText, backingBar = false)
            objText.draw(par, 0.72f, 0.07f, eyeAspect, 0.9f)
            if (Game.weaponName.isNotBlank()) {
                speakerText.setText(Game.weaponName, backingBar = false)
                speakerText.draw(par + 0.55f, -0.84f, 0.05f, eyeAspect, 0.85f)
            }
        }

        // captions (voice) — always available
        if (Game.subtitlesOn && (voice.speaking || now < voice.lineEndsAt) && Game.caption.isNotBlank()) {
            speakerText.setText(Game.captionSpeaker, backingBar = false)
            speakerText.draw(par, -0.54f, 0.11f, eyeAspect, 0.95f)
            captionText.setText(Game.caption, backingBar = true)
            captionText.draw(par, -0.72f, 0.32f, eyeAspect, 0.95f)
        }
        if (now < Game.messageUntil && Game.message.isNotBlank()) {
            msgText.setText(Game.message, backingBar = true)
            msgText.draw(par, 0.35f, 0.10f, eyeAspect, 0.95f)
        }

        when (Game.phase) {
            Game.TITLE -> {
                val m = Campaign.missions[Game.missionIdx]
                val tSong = (System.currentTimeMillis() - titleStartMs) / 1000f
                bigText.setText("SUNSARRA DRIFT", backingBar = false)
                bigText.draw(par, 0.62f, 0.13f, eyeAspect, 0.8f + 0.2f * sin(timeSec * 2f))
                // karaoke: the film is cut to these lines
                val lyr = cinematic.lyric(tSong)
                if (lyr.isNotBlank()) {
                    captionText.setText("♪ $lyr ♪", backingBar = false)
                    captionText.draw(par, -0.54f, 0.22f, eyeAspect, 0.95f)
                }
                val ca = cinematic.state.cardAlpha
                cardText.setText("▶ MISSION ${Game.missionIdx + 1}/${Campaign.missions.size}: ${m.title} · ${roleName(m.role)}\nSWIPE: MISSION · TAP: LAUNCH · DOUBLE-TAP: SETTINGS", backingBar = true)
                cardText.draw(par, -0.86f, 0.10f, eyeAspect, ca)
            }
            Game.BRIEFING -> {
                val m = engine.mission
                bigText.setText(m.title, backingBar = false)
                bigText.draw(par, 0.62f, 0.11f, eyeAspect, 0.95f)
                cardText.setText("${roleName(m.role)} SEAT\n\n${m.briefing}\n\nHOMAGE: ${m.derivedFrom}\n\nTAP WHEN READY.", backingBar = true)
                cardText.draw(par, -0.18f, 0.40f, eyeAspect, 0.95f)
            }
            Game.DEBRIEF -> {
                bigText.setText(if (Game.won) "MISSION COMPLETE" else "DRIFT DISABLED", backingBar = false)
                bigText.draw(par, 0.35f, 0.13f, eyeAspect, 0.95f)
                cardText.setText(if (Game.won) "TAP TO CONTINUE THE STORY."
                    else "The Drift limps clear. Yu patches, Ada re-plans.\nTAP TO FLY IT AGAIN.", backingBar = true)
                cardText.draw(par, -0.30f, 0.18f, eyeAspect, 0.9f)
            }
            Game.CUTSCENE -> {
                // portrait card, if the player generated one (Gemini prompts provided)
                val id = Game.sceneLines.getOrNull(Game.sceneIdx)
                val sp = id?.let { voice.lines[it]?.speaker } ?: ""
                val tex = portraits[sp] ?: 0
                if (tex != 0) drawPortrait(tex, par)
            }
        }

        if (Game.menuOpen) {
            val items = listOf(
                "RESUME",
                "RESTART MISSION",
                "SUBTITLES: ${if (Game.subtitlesOn) "ON" else "OFF"}",
                "MUSIC VOL: ${(Game.musicVol * 100).toInt()}%",
                "SFX/VOICE VOL: ${(Game.sfxVol * 100).toInt()}%",
                "INVERT STEER: ${if (Game.invertSteer) "ON" else "OFF"}",
                "RECENTER VIEW",
                "ABANDON TO TITLE")
            val b = StringBuilder("— SETTINGS —")
            items.forEachIndexed { i, s2 ->
                b.append('\n').append(if (i == Game.menuIndex) "▶ $s2" else "· $s2")
            }
            cardText.setText(b.toString(), backingBar = true)
            cardText.draw(par, 0f, 0.42f, eyeAspect, 0.97f)
        } else if (Game.paused && Game.phase == Game.PLAY) {
            msgText.setText("PAUSED — TAP TO RESUME", backingBar = true)
            msgText.draw(par, 0f, 0.10f, eyeAspect, 0.9f)
        }

        letterbox.draw(letterboxAmt)
        // white flash on impacts / EMP
        if (Game.flash > 0.02f) {
            hudFx.begin()
            for (i in 0..12) {
                hudFx.line(-1f, -1f + i / 6f, 0f, 1f, -1f + i / 6f, 0f, 1f, 1f, 1f, Game.flash * 0.12f)
            }
            hudFx.draw(hudVp)
        }
    }

    private fun bar(x0: Float, y: Float, frac: Float, r: Float, g: Float, b: Float, width2: Float = 0.45f) {
        hudFx.line(x0, y, 0f, x0 + width2, y, 0f, r, g, b, 0.25f)
        hudFx.line(x0, y, 0f, x0 + width2 * frac.coerceIn(0f, 1f), y, 0f, r, g, b, 0.95f)
        hudFx.line(x0, y - 0.012f, 0f, x0 + width2 * frac.coerceIn(0f, 1f), y - 0.012f, 0f, r, g, b, 0.95f)
    }

    // simple textured quad for cutscene portraits
    private var texProg = 0; private var tAp = 0; private var tAu = 0
    private var tUc = 0; private var tUs = 0; private var tUt = 0; private var tUa = 0
    private val texQuad = GlAssets.floatBuffer(floatArrayOf(
        -1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f))

    private fun drawPortrait(tex: Int, par: Float) {
        if (texProg == 0) {
            texProg = GlAssets.compileProgram("""
                attribute vec2 aPos; attribute vec2 aUV;
                uniform vec2 uC; uniform vec2 uS; varying vec2 vUV;
                void main() { vUV = aUV; gl_Position = vec4(uC + aPos * uS, 0.0, 1.0); }
            """, """
                precision mediump float; varying vec2 vUV;
                uniform sampler2D uT; uniform float uA;
                void main() { vec4 c = texture2D(uT, vUV); gl_FragColor = vec4(c.rgb, c.a * uA); }
            """)
            tAp = GLES20.glGetAttribLocation(texProg, "aPos")
            tAu = GLES20.glGetAttribLocation(texProg, "aUV")
            tUc = GLES20.glGetUniformLocation(texProg, "uC")
            tUs = GLES20.glGetUniformLocation(texProg, "uS")
            tUt = GLES20.glGetUniformLocation(texProg, "uT")
            tUa = GLES20.glGetUniformLocation(texProg, "uA")
        }
        GLES20.glUseProgram(texProg)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glUniform1i(tUt, 0)
        GLES20.glUniform1f(tUa, 0.95f)
        GLES20.glUniform2f(tUc, -0.58f + par, 0.10f)
        GLES20.glUniform2f(tUs, 0.24f, 0.34f)
        texQuad.position(0)
        GLES20.glVertexAttribPointer(tAp, 2, GLES20.GL_FLOAT, false, 16, texQuad)
        GLES20.glEnableVertexAttribArray(tAp)
        texQuad.position(2)
        GLES20.glVertexAttribPointer(tAu, 2, GLES20.GL_FLOAT, false, 16, texQuad)
        GLES20.glEnableVertexAttribArray(tAu)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(tAp)
        GLES20.glDisableVertexAttribArray(tAu)
    }

    // ---------------- flow ----------------
    private fun roleName(r: Int) = when (r) {
        Game.ROLE_GUNNER -> "GUNNER"; Game.ROLE_PILOT -> "PILOT"; else -> "ENGINEER"
    }

    private fun consumeActions(camPos: FloatArray, gazeDir: FloatArray) {
        while (true) {
            val a = Game.actions.poll() ?: break
            when (Game.phase) {
                Game.TITLE -> when (a) {
                    1 -> beginMission()
                    2 -> Game.missionIdx = (Game.missionIdx + 1) % Game.missionsUnlocked
                    3 -> Game.missionIdx = (Game.missionIdx + Game.missionsUnlocked - 1) % Game.missionsUnlocked
                }
                Game.CUTSCENE -> if (a == 1) skipLine()
                Game.BRIEFING -> if (a == 1) {
                    Game.phase = Game.PLAY
                    music.play(engine.mission.musicKey)
                    Game.say("ROLE: ${roleName(engine.mission.role)}", 2500)
                }
                Game.DEBRIEF -> if (a == 1) {
                    if (Game.won) {
                        startScene(engine.mission.sceneOut, Game.TITLE)
                    } else {
                        engine.start(engine.mission)
                        Game.phase = Game.BRIEFING
                    }
                }
                Game.PLAY -> when (a) {
                    1 -> if (Game.paused) Game.paused = false else engine.tap(camPos, gazeDir)
                    2 -> engine.swipe(true)
                    3 -> engine.swipe(false)
                }
            }
        }
    }

    private fun beginMission() {
        val m = Campaign.missions[Game.missionIdx]
        engine.start(m)
        Game.won = false
        startScene(m.sceneIn, Game.BRIEFING)
    }

    private fun startScene(lines: List<String>, after: Int) {
        Game.sceneLines = lines
        Game.sceneIdx = 0
        Game.sceneAfter = after
        Game.phase = Game.CUTSCENE
        lineAdvanceAt = voice.play(lines.first()) + 350
    }

    private fun advanceCutscene() {
        if (Game.phase != Game.CUTSCENE) return
        if (System.currentTimeMillis() < lineAdvanceAt || voice.speaking) return
        nextLine()
    }

    private fun skipLine() { voice.stop(); nextLine() }

    private fun nextLine() {
        Game.sceneIdx++
        if (Game.sceneIdx >= Game.sceneLines.size) {
            Game.caption = ""
            val after = Game.sceneAfter
            Game.phase = after
            if (after == Game.TITLE) {
                // scene-out finished: unlock the next mission, save progress
                if (Game.missionIdx + 1 >= Game.missionsUnlocked &&
                    Game.missionsUnlocked < Campaign.missions.size) {
                    Game.missionsUnlocked++
                    Game.missionIdx = (Game.missionIdx + 1).coerceAtMost(Campaign.missions.size - 1)
                }
                ctx.getSharedPreferences("drift", Context.MODE_PRIVATE).edit()
                    .putInt("unlocked", Game.missionsUnlocked).apply()
                music.play("title")
            }
            return
        }
        lineAdvanceAt = voice.play(Game.sceneLines[Game.sceneIdx]) + 350
    }

    private fun drainEngineEvents() {
        while (true) {
            val e = engine.events.removeFirstOrNull() ?: break
            when (e) {
                "pulse" -> sfx.play("pulse", 0.9f)
                "hit" -> sfx.play("impact", 0.7f)
                "boom" -> { sfx.play("explosion"); Game.flash = 0.35f }
                "boltIn" -> sfx.play("boltby", 0.4f)
                "shieldHit" -> sfx.play("shieldhit", 0.8f)
                "shieldLow" -> voice.radio(Campaign.radioLowShield, 15000)
                "wave" -> voice.radio(Campaign.radioWave, 12000)
                "streak" -> voice.radio(Campaign.radioKillStreak, 20000)
                "lockon" -> sfx.play("lockon", 0.8f)
                "missile" -> sfx.play("missile")
                "emp" -> sfx.play("emp")
                "deny" -> sfx.play("deny", 0.6f)
                "wswitch" -> sfx.play("uiswitch", 0.7f)
                "gate" -> { sfx.play("gate", 0.9f); voice.radio(Campaign.radioGate, 14000) }
                "gateMiss" -> { sfx.play("gatemiss"); voice.radio(Campaign.radioMiss, 12000) }
                "boost" -> sfx.play("boost")
                "throttle" -> sfx.play("uiswitch", 0.6f)
                "mineHit" -> { sfx.play("shieldhit"); Game.flash = 0.4f }
                "fault" -> sfx.play("alarm", 0.8f)
                "weld" -> sfx.play("weld", 0.9f)
                "fixed" -> { sfx.play("fixed"); voice.radio(Campaign.radioRepair, 14000) }
                "fuelWanted" -> sfx.play("alarm", 0.6f)
                "fuelStart" -> sfx.play("fuelflow", 0.9f)
                "fuelDone" -> { sfx.play("fixed"); Game.say("TRANSFER COMPLETE", 2200) }
                "allyHit" -> sfx.play("shieldhit", 0.5f)
                "win" -> {
                    Game.won = true; Game.phase = Game.DEBRIEF
                    sfx.play("winsting"); music.play("title")
                    voice.radio(Campaign.radioWin, 0)
                }
                "fail" -> {
                    Game.won = false; Game.phase = Game.DEBRIEF
                    sfx.play("failsting"); music.pause()
                    voice.radio(Campaign.radioFail, 0)
                }
            }
        }
    }

    // ---------------- math ----------------
    private fun dist(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]; val dy = a[1] - b[1]; val dz = a[2] - b[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    private fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
    private fun norm(v: FloatArray): FloatArray {
        val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).coerceAtLeast(1e-6f)
        return floatArrayOf(v[0] / l, v[1] / l, v[2] / l)
    }
    private fun smooth(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
