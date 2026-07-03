package com.sunsarradrift

import com.sunsarradrift.gl.SolarSystem
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * One engine, three seats:
 *   GUNNER   — head is the reticle, tap fires, swipe cycles PULSE/MISSILE/EMP
 *   PILOT    — head steers the ship through gates, swipe = throttle, tap = boost
 *   ENGINEER — look at a station to select, tap to repair, swipe fwd = fuel flow
 * World-frame simulation anchored to the Pale Blue rail; the renderer feeds
 * ship position/heading/gaze each frame and drains [events] for sfx/voice.
 */
class MissionEngine(private val world: SolarSystem) {

    // ---------- shared state ----------
    var mission: Campaign.Mission = Campaign.missions[0]; private set
    var missionU = 0f; private set
    var latX = 0f; private set
    var latY = 0f; private set
    val events = ArrayDeque<String>()
    private val rnd = Random(System.nanoTime())
    private var elapsed = 0f
    val elapsedSec: Float get() = elapsed
    private var over = false

    // gunner
    class Enemy {
        val pos = FloatArray(3); val vel = FloatArray(3)
        var hp = 0f; var mesh = 0; var scale = 1f
        var orbitA = 0f; var orbitR = 30f; var orbitW = 0.5f; var orbitY = 0f
        var fireCd = 0f; var disabledUntil = 0f; var alive = false
        var targetsAlly = false
    }
    val enemies = Array(14) { Enemy() }
    class Bolt { val pos = FloatArray(3); val vel = FloatArray(3); var ttl = 0f; var friendly = false }
    val bolts = Array(48) { Bolt() }
    class Missile { val pos = FloatArray(3); val vel = FloatArray(3); var target = -1; var ttl = 0f; var live = false }
    val missiles = Array(6) { Missile() }
    var kills = 0; private set
    var weapon = 0; private set                 // 0 pulse 1 missile 2 emp
    private var pulseCd = 0f; private var missileCd = 0f; private var empCd = 0f
    var missileAmmo = 10; private set
    private var lockTarget = -1; private var lockTime = 0f
    private var waveTimer = 0f
    val allyPos = FloatArray(3)
    var allyHp = 100f; private set
    var hasAlly = false; private set
    var lastTracer = FloatArray(6); var tracerTtl = 0f   // renderer draws the hitscan beam

    // pilot
    class Gate { var u = 0f; var ox = 0f; var oy = 0f; var r = 3.2f; var passed = false; var missed = false }
    val gates = Array(20) { Gate() }
    var gateCount = 0; private set
    var gatesPassed = 0; private set
    private var throttle = 1                     // 0 slow 1 cruise 2 hot
    private var boostT = 0f
    class Mine { val pos = FloatArray(3); var live = false }
    val mines = Array(24) { Mine() }

    // engineer
    class Station(val name: String, val yaw: Float) {
        var fault = false; var repair = 0f; var flashT = 0f
    }
    val stations = arrayOf(
        Station("COOLANT", 0f), Station("PUMPS", 1.05f), Station("SHIELDS", 2.09f),
        Station("GUNS", -1.05f), Station("COMMS", -2.09f), Station("FUEL", 3.14f))
    var lookStation = -1; private set
    var fuelFlow = false; private set
    private var fuelFlowT = 0f
    var transfersDone = 0; private set
    private var transferWanted = false
    private var transferTimer = 20f
    private var surviveT = 0f

    // ---------- lifecycle ----------
    fun start(m: Campaign.Mission) {
        mission = m
        missionU = m.u; latX = 0f; latY = 0f
        elapsed = 0f; over = false
        kills = 0; weapon = 0; missileAmmo = 10
        pulseCd = 0f; missileCd = 0f; empCd = 0f; lockTarget = -1; lockTime = 0f
        enemies.forEach { it.alive = false }
        bolts.forEach { it.ttl = 0f }
        missiles.forEach { it.live = false }
        waveTimer = 1.5f
        hasAlly = m.id in setOf("m01", "m04", "m07", "m10")
        allyHp = 100f
        gatesPassed = 0; throttle = 1; boostT = 0f
        transfersDone = 0; transferWanted = false; transferTimer = 16f
        fuelFlow = false; surviveT = 0f
        stations.forEach { it.fault = false; it.repair = 0f }
        Game.hull = 100f; Game.shield = 100f
        Game.progress = 0f
        Game.pursuerDist = 1f
        if (m.role == Game.ROLE_PILOT) buildGates()
        Game.weaponName = if (m.role == Game.ROLE_GUNNER) "PULSE" else ""
        Game.objectiveText = when (m.role) {
            Game.ROLE_GUNNER -> "DESTROY 0/${m.goal}"
            Game.ROLE_PILOT -> "GATES 0/${m.goal}"
            else -> "HOLD ${m.goal % 1000}s · FUEL 0/${m.goal / 1000}"
        }
    }

    private fun buildGates() {
        gateCount = mission.goal
        var u = mission.u + 0.35f
        for (i in 0 until gateCount) {
            val g = gates[i]
            g.u = u; g.passed = false; g.missed = false
            g.ox = sin(i * 1.7f) * 5.5f
            g.oy = cos(i * 2.3f) * 3.5f
            g.r = 3.4f - mission.intensity * 0.6f
            u += 0.09f
        }
        // mines sprinkled between gates
        for (i in mines.indices) {
            val m = mines[i]
            m.live = i < gateCount
            if (m.live) {
                val p = railPoint(mission.u + 0.30f + i * 0.09f + 0.045f,
                    sin(i * 2.9f) * 6f, cos(i * 1.3f) * 4f)
                m.pos[0] = p[0]; m.pos[1] = p[1]; m.pos[2] = p[2]
            }
        }
    }

    // ---------- input (routed by the renderer) ----------
    fun tap(shipPos: FloatArray, gazeDir: FloatArray) {
        if (over) return
        when (mission.role) {
            Game.ROLE_GUNNER -> fire(shipPos, gazeDir)
            Game.ROLE_PILOT -> if (boostT <= 0f && Game.boostCd <= 0f) {
                boostT = 2.2f; Game.boostCd = 6f; events.add("boost")
            }
            else -> repairTap()
        }
    }

    fun swipe(forward: Boolean) {
        if (over) return
        when (mission.role) {
            Game.ROLE_GUNNER -> {
                weapon = (weapon + if (forward) 1 else 2) % 3
                Game.weaponName = arrayOf("PULSE", "MISSILE ×$missileAmmo", "EMP")[weapon]
                events.add("wswitch")
            }
            Game.ROLE_PILOT -> {
                throttle = (throttle + if (forward) 1 else -1).coerceIn(0, 2)
                events.add("throttle")
            }
            else -> {
                if (lookStation == 5 && transferWanted) {
                    fuelFlow = forward
                    events.add(if (forward) "fuelStart" else "fuelStop")
                } else events.add("wswitch")
            }
        }
    }

    // ---------- update ----------
    fun update(dt: Float, shipPos: FloatArray, shipYaw: Float, gazeDir: FloatArray,
               gazeYaw: Float, gazePitch: Float) {
        if (over) return
        elapsed += dt
        Game.boostCd = (Game.boostCd - dt).coerceAtLeast(0f)
        // shield regen after 4s calm
        if (System.currentTimeMillis() - Game.lastHitAt > 4000) {
            Game.shield = (Game.shield + dt * 4f).coerceAtMost(100f)
        }
        when (mission.role) {
            Game.ROLE_GUNNER -> updateGunner(dt, shipPos, gazeDir)
            Game.ROLE_PILOT -> updatePilot(dt, gazeYaw, gazePitch)
            else -> updateEngineer(dt, gazeYaw, gazePitch)
        }
        missionU += mission.driftU * dt * (if (mission.role == Game.ROLE_PILOT) 0f else 1f)
        if (Game.hull <= 0f) finish(false)
    }

    // ---------- gunner ----------
    private fun updateGunner(dt: Float, ship: FloatArray, gaze: FloatArray) {
        pulseCd -= dt; missileCd -= dt; empCd -= dt; tracerTtl -= dt
        // waves
        waveTimer -= dt
        val aliveCount = enemies.count { it.alive }
        if (waveTimer <= 0f && aliveCount < 4 && kills < mission.goal) {
            spawnWave(ship, (3 + (mission.intensity * 2).toInt()).coerceAtMost(6))
            waveTimer = 9f / mission.intensity
            events.add("wave")
        }
        if (hasAlly) {
            // ally holds formation off the starboard beam
            allyPos[0] = ship[0] + cos(elapsed * 0.2f) * 2f + 18f
            allyPos[1] = ship[1] + sin(elapsed * 0.3f) * 1.5f
            allyPos[2] = ship[2] - 6f
        }
        // enemies steer + shoot
        val now = elapsed
        for ((i, e) in enemies.withIndex()) {
            if (!e.alive) continue
            if (now < e.disabledUntil) {          // EMP'd: drift dark
                e.pos[0] += e.vel[0] * dt * 0.2f
                e.pos[1] += e.vel[1] * dt * 0.2f
                e.pos[2] += e.vel[2] * dt * 0.2f
                continue
            }
            e.orbitA += e.orbitW * dt
            val tx = ship[0] + cos(e.orbitA) * e.orbitR
            val ty = ship[1] + e.orbitY + sin(e.orbitA * 0.7f) * 4f
            val tz = ship[2] + sin(e.orbitA) * e.orbitR
            val ax = tx - e.pos[0]; val ay = ty - e.pos[1]; val az = tz - e.pos[2]
            e.vel[0] += ax * dt * 1.6f; e.vel[1] += ay * dt * 1.6f; e.vel[2] += az * dt * 1.6f
            val sp = len(e.vel)
            val maxSp = 22f * mission.intensity
            if (sp > maxSp) { e.vel[0] *= maxSp / sp; e.vel[1] *= maxSp / sp; e.vel[2] *= maxSp / sp }
            e.pos[0] += e.vel[0] * dt; e.pos[1] += e.vel[1] * dt; e.pos[2] += e.vel[2] * dt
            e.fireCd -= dt
            if (e.fireCd <= 0f) {
                e.fireCd = 2.6f / mission.intensity + rnd.nextFloat()
                val tgt = if (e.targetsAlly && hasAlly) allyPos else ship
                spawnBolt(e.pos, tgt, friendly = false, speed = 42f)
                events.add("boltIn")
            }
        }
        // missile lock: nearest enemy within 6 degrees of gaze
        val lt = aimTarget(ship, gaze, cos(6f * PI.toFloat() / 180f))
        if (weapon == 1 && lt >= 0) {
            if (lt == lockTarget) {
                lockTime += dt
                if (lockTime > 0.7f && Game.lockFrac < 1f) events.add("lockon")
            } else { lockTarget = lt; lockTime = 0f }
            Game.lockFrac = (lockTime / 0.7f).coerceAtMost(1f)
        } else { lockTarget = -1; lockTime = 0f; Game.lockFrac = 0f }

        updateProjectiles(dt, ship)
        Game.progress = kills / mission.goal.toFloat()
        Game.objectiveText = "DESTROY $kills/${mission.goal}" +
                if (hasAlly) " · ALLY ${allyHp.toInt()}%" else ""
        if (hasAlly && allyHp <= 0f) finish(false)
        if (kills >= mission.goal) finish(true)
    }

    private fun spawnWave(ship: FloatArray, n: Int) {
        var spawned = 0
        for (e in enemies) {
            if (e.alive || spawned >= n) continue
            e.alive = true; spawned++
            e.hp = if (rnd.nextFloat() < 0.2f) 140f else 60f
            e.mesh = if (e.hp > 100f) 2 else 0
            e.scale = if (e.hp > 100f) 2.0f else 1.1f
            e.orbitA = rnd.nextFloat() * 6.28f
            e.orbitR = 22f + rnd.nextFloat() * 22f
            e.orbitW = (0.3f + rnd.nextFloat() * 0.5f) * (if (rnd.nextBoolean()) 1f else -1f)
            e.orbitY = (rnd.nextFloat() - 0.4f) * 14f
            e.targetsAlly = hasAlly && rnd.nextFloat() < 0.3f
            val a = rnd.nextFloat() * 6.28f
            e.pos[0] = ship[0] + cos(a) * 90f
            e.pos[1] = ship[1] + (rnd.nextFloat() - 0.5f) * 30f
            e.pos[2] = ship[2] + sin(a) * 90f
            e.vel.fill(0f)
            e.fireCd = 1.5f + rnd.nextFloat() * 2f
            e.disabledUntil = 0f
        }
    }

    private fun aimTarget(ship: FloatArray, gaze: FloatArray, cosMax: Float): Int {
        var best = -1; var bestDot = cosMax
        for ((i, e) in enemies.withIndex()) {
            if (!e.alive) continue
            val dx = e.pos[0] - ship[0]; val dy = e.pos[1] - ship[1]; val dz = e.pos[2] - ship[2]
            val d = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-3f)
            val dot = (dx * gaze[0] + dy * gaze[1] + dz * gaze[2]) / d
            if (dot > bestDot) { bestDot = dot; best = i }
        }
        return best
    }

    private fun fire(ship: FloatArray, gaze: FloatArray) {
        when (weapon) {
            0 -> {
                if (pulseCd > 0f) return
                pulseCd = 0.18f
                events.add("pulse")
                val t = aimTarget(ship, gaze, cos(4.5f * PI.toFloat() / 180f))
                val end = if (t >= 0) enemies[t].pos
                          else floatArrayOf(ship[0] + gaze[0] * 160f, ship[1] + gaze[1] * 160f, ship[2] + gaze[2] * 160f)
                lastTracer = floatArrayOf(ship[0], ship[1] - 0.6f, ship[2], end[0], end[1], end[2])
                tracerTtl = 0.09f
                if (t >= 0) hurt(t, 26f)
            }
            1 -> {
                if (missileCd > 0f || missileAmmo <= 0 || Game.lockFrac < 1f || lockTarget < 0) {
                    events.add("deny"); return
                }
                missileCd = 2.2f; missileAmmo--
                Game.weaponName = "MISSILE ×$missileAmmo"
                for (m in missiles) {
                    if (m.live) continue
                    m.live = true; m.ttl = 6f; m.target = lockTarget
                    m.pos[0] = ship[0]; m.pos[1] = ship[1] - 1f; m.pos[2] = ship[2]
                    m.vel[0] = gaze[0] * 30f; m.vel[1] = gaze[1] * 30f; m.vel[2] = gaze[2] * 30f
                    break
                }
                events.add("missile")
            }
            else -> {
                if (empCd > 0f) { events.add("deny"); return }
                empCd = 18f
                events.add("emp")
                Game.flash = 0.7f
                for (e in enemies) {
                    if (!e.alive) continue
                    val d = dist(e.pos, ship)
                    if (d < 60f) { e.disabledUntil = elapsed + 3.2f; hurt(enemies.indexOf(e), 12f) }
                }
            }
        }
    }

    private fun hurt(idx: Int, dmg: Float) {
        val e = enemies[idx]
        if (!e.alive) return
        e.hp -= dmg
        events.add("hit")
        if (e.hp <= 0f) {
            e.alive = false
            kills++
            events.add("boom")
            if (kills % 5 == 0) events.add("streak")
        }
    }

    private fun spawnBolt(from: FloatArray, to: FloatArray, friendly: Boolean, speed: Float) {
        for (b in bolts) {
            if (b.ttl > 0f) continue
            b.friendly = friendly; b.ttl = 4f
            b.pos[0] = from[0]; b.pos[1] = from[1]; b.pos[2] = from[2]
            val dx = to[0] - from[0]; val dy = to[1] - from[1]; val dz = to[2] - from[2]
            val d = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-3f)
            // slight spread so the player can dodge by drifting
            b.vel[0] = dx / d * speed + (rnd.nextFloat() - 0.5f) * 3f
            b.vel[1] = dy / d * speed + (rnd.nextFloat() - 0.5f) * 3f
            b.vel[2] = dz / d * speed + (rnd.nextFloat() - 0.5f) * 3f
            return
        }
    }

    private fun updateProjectiles(dt: Float, ship: FloatArray) {
        for (b in bolts) {
            if (b.ttl <= 0f) continue
            b.ttl -= dt
            b.pos[0] += b.vel[0] * dt; b.pos[1] += b.vel[1] * dt; b.pos[2] += b.vel[2] * dt
            if (!b.friendly) {
                if (dist(b.pos, ship) < 2.4f) {
                    b.ttl = 0f; Game.damage(9f); events.add("shieldHit")
                    if (Game.shield <= 25f) events.add("shieldLow")
                } else if (hasAlly && dist(b.pos, allyPos) < 3.2f) {
                    b.ttl = 0f; allyHp -= 7f; events.add("allyHit")
                }
            }
        }
        for (m in missiles) {
            if (!m.live) continue
            m.ttl -= dt
            if (m.ttl <= 0f) { m.live = false; continue }
            val t = m.target
            if (t in enemies.indices && enemies[t].alive) {
                val e = enemies[t]
                val dx = e.pos[0] - m.pos[0]; val dy = e.pos[1] - m.pos[1]; val dz = e.pos[2] - m.pos[2]
                val d = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-3f)
                val sp = 58f
                m.vel[0] += (dx / d * sp - m.vel[0]) * dt * 3f
                m.vel[1] += (dy / d * sp - m.vel[1]) * dt * 3f
                m.vel[2] += (dz / d * sp - m.vel[2]) * dt * 3f
                if (d < 3.5f) { m.live = false; hurt(t, 110f); continue }
            }
            m.pos[0] += m.vel[0] * dt; m.pos[1] += m.vel[1] * dt; m.pos[2] += m.vel[2] * dt
        }
    }

    // ---------- pilot ----------
    private fun updatePilot(dt: Float, gazeYaw: Float, gazePitch: Float) {
        // head steers lateral offset inside the corridor
        var sy = gazeYaw
        if (Game.invertSteer) sy = -sy
        val txp = (sy / 0.6f).coerceIn(-1f, 1f) * 8f
        val typ = (gazePitch / 0.5f).coerceIn(-1f, 1f) * 5f
        latX += (txp - latX) * dt * 3.2f
        latY += (typ - latY) * dt * 3.2f
        boostT -= dt
        val base = mission.driftU * (0.55f + 0.45f * throttle)
        val speed = base * (if (boostT > 0f) 2.1f else 1f)
        missionU += speed * dt * 60f * 0.01f
        // gates
        for (i in 0 until gateCount) {
            val g = gates[i]
            if (g.passed || g.missed || missionU < g.u) continue
            val dx = latX - g.ox; val dy = latY - g.oy
            if (sqrt(dx * dx + dy * dy) <= g.r) {
                g.passed = true; gatesPassed++
                Game.pursuerDist = (Game.pursuerDist + 0.09f).coerceAtMost(1f)
                events.add("gate")
            } else {
                g.missed = true
                Game.pursuerDist -= 0.16f
                events.add("gateMiss")
            }
        }
        // mines
        for (m in mines) {
            if (!m.live) continue
            val sp = railPoint(missionU, latX, latY)
            if (dist(m.pos, sp) < 2.6f) { m.live = false; Game.damage(18f); events.add("mineHit") }
        }
        // the lance closes relentlessly; boost and gates buy distance
        Game.pursuerDist -= dt * 0.012f * mission.intensity
        if (boostT > 0f) Game.pursuerDist += dt * 0.05f
        Game.pursuerDist = Game.pursuerDist.coerceIn(0f, 1f)
        Game.progress = gatesPassed / mission.goal.toFloat()
        Game.objectiveText = "GATES $gatesPassed/${mission.goal} · THR ${throttle + 1}"
        if (Game.pursuerDist <= 0f) finish(false)
        if (gatesPassed >= mission.goal) finish(true)
    }

    // ---------- engineer ----------
    private fun updateEngineer(dt: Float, gazeYaw: Float, gazePitch: Float) {
        surviveT += dt
        val holdGoal = (mission.goal % 1000).toFloat()
        val fuelGoal = mission.goal / 1000
        // which station is the player looking at?
        lookStation = -1
        if (kotlin.math.abs(gazePitch) < 0.5f) {
            for ((i, st) in stations.withIndex()) {
                var d = st.yaw - gazeYaw
                while (d > PI) d -= 2 * PI.toFloat()
                while (d < -PI) d += 2 * PI.toFloat()
                if (kotlin.math.abs(d) < 0.30f) { lookStation = i; break }
            }
        }
        // faults spawn; unfixed faults gnaw the hull
        if (rnd.nextFloat() < dt * 0.16f * mission.intensity) {
            val candidates = stations.filter { !it.fault }
            if (candidates.isNotEmpty()) {
                candidates.random(rnd).apply { fault = true; repair = 0f }
                events.add("fault")
            }
        }
        var burning = 0
        for (st in stations) {
            st.flashT += dt
            if (st.fault) burning++
        }
        if (burning > 0) Game.damage(dt * 0.9f * burning * 0.5f)
        // fuel transfer requests arrive on a clock
        transferTimer -= dt
        if (!transferWanted && transfersDone < fuelGoal && transferTimer <= 0f) {
            transferWanted = true
            events.add("fuelWanted")
            Game.say("KESTREL NEEDS FUEL — LOOK AT FUEL STATION, SWIPE FORWARD", 4200)
        }
        if (fuelFlow) {
            fuelFlowT += dt
            if (fuelFlowT >= 3f) {
                fuelFlow = false; fuelFlowT = 0f
                transfersDone++; transferWanted = false
                transferTimer = 18f + rnd.nextFloat() * 10f
                events.add("fuelDone")
            }
        } else fuelFlowT = 0f
        Game.progress = ((surviveT / holdGoal) * 0.6f + (transfersDone / fuelGoal.toFloat()) * 0.4f)
            .coerceAtMost(1f)
        Game.objectiveText = "HOLD ${surviveT.toInt()}/${holdGoal.toInt()}s · FUEL $transfersDone/$fuelGoal" +
                (if (burning > 0) " · FIRES $burning" else "")
        if (surviveT >= holdGoal && transfersDone >= fuelGoal) finish(true)
    }

    private fun repairTap() {
        val i = lookStation
        if (i < 0) { events.add("deny"); return }
        val st = stations[i]
        if (!st.fault) { events.add("wswitch"); return }
        st.repair += 0.34f
        events.add("weld")
        if (st.repair >= 1f) {
            st.fault = false; st.repair = 0f
            events.add("fixed")
        }
    }

    // ---------- helpers ----------
    fun railPoint(u: Float, ox: Float, oy: Float): FloatArray {
        val p = world.camPosAt(u)
        val t = world.tangentAt(u)
        // right = cross(t, up)
        val rx = -t[2]; val rz = t[0]
        val rl = sqrt(rx * rx + rz * rz).coerceAtLeast(1e-4f)
        return floatArrayOf(
            p[0] + rx / rl * ox, p[1] + oy, p[2] + rz / rl * ox)
    }

    private fun finish(win: Boolean) {
        if (over) return
        over = true
        events.add(if (win) "win" else "fail")
    }

    private fun len(v: FloatArray) = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    private fun dist(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]; val dy = a[1] - b[1]; val dz = a[2] - b[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
