package com.sunsarradrift

import com.sunsarradrift.gl.Craft
import com.sunsarradrift.gl.LineBatch
import com.sunsarradrift.gl.SolarSystem
import kotlin.math.cos
import kotlin.math.sin

/**
 * TITLE CINEMATOGRAPHY — an attract-mode film cut to the title song,
 * shot-for-lyric (see SUNO_PROMPTS.md for the matching section timings):
 *
 *   V1  0:00–0:26  "born where the engines sing / borrowed light and broken
 *                   rings" — drifting past LUNA, Earthlight over the shoulder,
 *                   wreck-ring debris tumbling past the glass.
 *   V2  0:26–0:52  "took her voice beyond the moon" — the camera turns from
 *                   home to the dark; a lone Empire LANCE recedes with its
 *                   prize; sparks of dust become lanterns.
 *   CH1 0:52–1:18  "Drift, drift toward morning / past the fire" — hard burn
 *                   sunward into the glare of dawn; allied freighters —
 *                   "us all" — slide into formation alongside.
 *   V3  1:18–1:44  "Yu…gears / Ada…fears / Tern draws paths" — Orion nursery
 *                   wonder; Tern's nav-gates ignite in a curve ahead; the
 *                   camera dips to admire the Drift's own hull.
 *   CH2 1:44–2:10  reprise at speed — Kuiper-to-Orion rush, dust streaming,
 *                   the convoy holding the line.
 *   OUT 2:10–2:40  "One blue world behind me… I carry humankind" — full turn:
 *                   Earth, small and blue, centered and shrinking as we pull
 *                   away. Title and menu bloom in.
 *
 * The head stays free the whole time — the film happens around the player.
 */
class TitleCinematic {

    class State {
        var u = 2.05f
        var yawOff = 0f          // added to rail heading (radians)
        var pitchOff = 0f
        var convoy = 0f          // 0..1 formation presence
        var lance = 0f           // 0..1 abduction lance visibility
        var gates = 0f           // 0..1 Tern's path rings
        var cardAlpha = 0.35f    // menu card fade (blooms in the outro)
    }

    val state = State()
    private val tmp = FloatArray(3)

    private fun ss(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** [t] = seconds since the title screen (and title.mp3) began; loops. */
    fun update(tIn: Float) {
        val t = tIn % SONG_LEN
        val s = state
        s.convoy = 0f; s.lance = 0f; s.gates = 0f
        when {
            t < 26f -> {              // V1 — Luna, earthlight, broken rings
                val k = t / 26f
                s.u = 1.95f + k * 0.25f
                s.yawOff = 2.55f - ss(0f, 1f, k) * 0.35f      // looking back toward Earth
                s.pitchOff = -0.06f
                s.cardAlpha = 0.30f
            }
            t < 52f -> {              // V2 — the turn to the dark; the lance recedes
                val k = (t - 26f) / 26f
                s.u = 2.20f + k * 0.10f
                s.yawOff = (2.20f) * (1f - ss(0f, 1f, k))     // home → forward
                s.pitchOff = -0.02f
                s.lance = ss(0.05f, 0.35f, k) * (1f - ss(0.75f, 1f, k))
                s.cardAlpha = 0.30f
            }
            t < 78f -> {              // CH1 — burn toward morning, convoy joins
                val k = (t - 52f) / 26f
                s.u = 2.3f + ss(0f, 1f, k) * 1.55f            // toward the Sun's glare
                s.yawOff = 0f
                s.pitchOff = 0.03f
                s.convoy = ss(0.1f, 0.4f, k)
                s.cardAlpha = 0.30f
            }
            t < 104f -> {             // V3 — the crew verse: nebula, gates, hull
                val k = (t - 78f) / 26f
                s.u = 17.25f + k * 0.35f
                s.yawOff = sin(k * 3.14f) * 0.25f
                s.pitchOff = -0.10f + ss(0f, 0.3f, k) * 0.12f // dip to the hull, rise to the sky
                s.gates = ss(0.35f, 0.6f, k)                  // "Tern draws paths…"
                s.cardAlpha = 0.30f
            }
            t < 130f -> {             // CH2 — the rush, everyone flying
                val k = (t - 104f) / 26f
                s.u = 13.0f + ss(0f, 1f, k) * 3.5f
                s.yawOff = 0f
                s.pitchOff = 0.02f
                s.convoy = 1f
                s.cardAlpha = 0.30f
            }
            else -> {                 // OUTRO — one blue world behind me
                val k = (t - 130f) / 30f
                s.u = 0.65f + ss(0f, 1f, k) * 0.85f           // pulling away from Earth
                s.yawOff = 3.14159f                           // full look back
                s.pitchOff = -0.04f
                s.cardAlpha = 0.30f + ss(0.35f, 0.8f, k) * 0.65f
            }
        }
    }

    /** Karaoke caption for the current moment of the song. */
    fun lyric(tIn: Float): String {
        val t = tIn % SONG_LEN
        for (i in LYRICS.indices.reversed()) {
            if (t >= LYRICS[i].first) return LYRICS[i].second
        }
        return ""
    }

    /** Extra staged actors: convoy, the abduction lance, Tern's gate path. */
    fun emitExtras(fx: LineBatch, craft: Craft, vp: FloatArray, camPos: FloatArray,
                   sunlight: Float, world: SolarSystem, t: Float) {
        val s = state
        val tan = world.tangentAt(s.u)
        // "us all": three freighters riding formation
        if (s.convoy > 0.05f) {
            val offs = arrayOf(floatArrayOf(14f, -1f, 8f), floatArrayOf(-13f, 2f, 12f),
                floatArrayOf(6f, 5f, 16f))
            for ((i, o) in offs.withIndex()) {
                val bob = sin(t * 0.8f + i * 2f) * 0.5f
                // off-beam laterally (right = (-tan.z, 0, tan.x)) and trailing along the rail
                tmp[0] = camPos[0] + (-tan[2]) * o[0] * s.convoy - tan[0] * o[2]
                tmp[1] = camPos[1] + o[1] + bob
                tmp[2] = camPos[2] + (tan[0]) * o[0] * s.convoy - tan[2] * o[2]
                craft.buildModel(tmp, tan, 0.08f * sin(t + i), 2.0f)
                craft.draw(vp, 1, camPos, SolarSystem.SUN_POS, sunlight,
                    floatArrayOf(0.35f, 0.62f, 0.60f), 0f)
                fx.glow(tmp[0] - tan[0] * 2.5f, tmp[1], tmp[2] - tan[2] * 2.5f,
                    10f, 0.5f, 0.85f, 1f, 0.7f * s.convoy)
            }
        }
        // "they took her voice beyond the moon": the lance dwindles into the dark
        if (s.lance > 0.05f) {
            val d = 30f + (1f - s.lance) * 160f
            tmp[0] = camPos[0] + tan[0] * d
            tmp[1] = camPos[1] + 4f
            tmp[2] = camPos[2] + tan[2] * d
            craft.buildModel(tmp, tan, 0f, 3.2f)
            craft.draw(vp, 2, camPos, SolarSystem.SUN_POS, sunlight,
                floatArrayOf(0.30f, 0.26f, 0.33f), 0.5f * s.lance)
            // the silver room's cold running light
            fx.glow(tmp[0], tmp[1] + 1f, tmp[2], 8f, 0.8f, 0.85f, 1f, s.lance)
        }
        // "Tern draws paths through dust and rain": nav rings arcing ahead
        if (s.gates > 0.05f) {
            for (i in 0 until 6) {
                val gu = s.u + 0.10f + i * 0.07f
                val gp = world.camPosAt(gu)
                val r = 3.0f
                val a = s.gates * (1f - i / 7f)
                val n = 18
                for (kSeg in 0 until n) {
                    val a0 = kSeg / n.toFloat() * 6.2832f
                    val a1 = (kSeg + 1) / n.toFloat() * 6.2832f
                    fx.line(gp[0] + cos(a0) * r, gp[1] + sin(a0) * r + sin(i * 1.7f) * 2f, gp[2],
                        gp[0] + cos(a1) * r, gp[1] + sin(a1) * r + sin(i * 1.7f) * 2f, gp[2],
                        0.45f, 0.95f, 0.85f, a)
                }
            }
        }
    }

    companion object LyricSheet {
        const val SONG_LEN = 160f

        /** (start-second, line) — nudge to taste once your render of the song lands. */
        val LYRICS = listOf(
            1.5f to "I was born where the engines sing",
            8f to "Under borrowed light and broken rings",
            14.5f to "Mama said, child, keep your name",
            20f to "When the maps go dark and the stars catch flame",
            27.5f to "They took her voice beyond the moon",
            34f to "Locked the truth in a silver room",
            40.5f to "But every scar and every spark",
            46f to "Is a lantern burning through the dark",
            53.5f to "Drift, drift toward morning",
            60f to "Past the fire, past the warning",
            66.5f to "If the night says we are small",
            72f to "We will answer with us all",
            79.5f to "Yu keeps faith in the turning gears",
            86f to "Ada names what the tyrant fears",
            92.5f to "Tern draws paths through dust and rain",
            98f to "And I fly on through love and pain",
            105.5f to "Drift, drift toward morning",
            112f to "Past the fire, past the warning",
            118.5f to "If the night says we are small",
            124f to "We will answer with us all",
            131f to "One blue world behind me",
            138f to "One true voice to find",
            145f to "I am not alone here",
            152f to "I carry humankind",
            158.5f to "")
    }
}
