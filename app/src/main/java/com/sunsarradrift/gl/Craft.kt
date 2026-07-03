package com.sunsarradrift.gl

import android.opengl.GLES20
import com.sunsarradrift.GlAssets
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Craft meshes + a shared lit renderer (ported from Pale Blue's traffic).
 * Meshes: 0 dart (Empire interceptor), 1 freighter (allies/convoy),
 * 2 lance (Empire heavy), 3 the Drift's visible cockpit frame pieces reuse ShipHull.
 */
class Craft {
    companion object {
        private fun tri(t: ArrayList<Float>, a: FloatArray, b: FloatArray, c: FloatArray) {
            val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
            val vx = c[0] - a[0]; val vy = c[1] - a[1]; val vz = c[2] - a[2]
            var nx = uy * vz - uz * vy; var ny = uz * vx - ux * vz; var nz = ux * vy - uy * vx
            val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
            nx /= l; ny /= l; nz /= l
            for (p in arrayOf(a, b, c)) {
                t.add(p[0]); t.add(p[1]); t.add(p[2]); t.add(nx); t.add(ny); t.add(nz)
            }
        }

        private fun box(t: ArrayList<Float>, cx: Float, cy: Float, cz: Float,
                        hx: Float, hy: Float, hz: Float) {
            val v = arrayOf(
                floatArrayOf(cx - hx, cy - hy, cz - hz), floatArrayOf(cx + hx, cy - hy, cz - hz),
                floatArrayOf(cx + hx, cy + hy, cz - hz), floatArrayOf(cx - hx, cy + hy, cz - hz),
                floatArrayOf(cx - hx, cy - hy, cz + hz), floatArrayOf(cx + hx, cy - hy, cz + hz),
                floatArrayOf(cx + hx, cy + hy, cz + hz), floatArrayOf(cx - hx, cy + hy, cz + hz))
            val f = arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(5, 4, 7, 6), intArrayOf(4, 0, 3, 7),
                intArrayOf(1, 5, 6, 2), intArrayOf(3, 2, 6, 7), intArrayOf(4, 5, 1, 0))
            for (q in f) { tri(t, v[q[0]], v[q[1]], v[q[2]]); tri(t, v[q[0]], v[q[2]], v[q[3]]) }
        }

        /** Empire interceptor: aggressive dart, raked wings. */
        fun dart(): FloatArray {
            val t = ArrayList<Float>()
            val nose = floatArrayOf(0f, 0f, -1.9f)
            val mT = floatArrayOf(0f, 0.26f, 0f); val mB = floatArrayOf(0f, -0.26f, 0f)
            val mL = floatArrayOf(-0.34f, 0f, 0f); val mR = floatArrayOf(0.34f, 0f, 0f)
            val tail = floatArrayOf(0f, 0.05f, 1.2f)
            tri(t, nose, mT, mR); tri(t, nose, mR, mB); tri(t, nose, mB, mL); tri(t, nose, mL, mT)
            tri(t, mT, tail, mR); tri(t, mR, tail, mB); tri(t, mB, tail, mL); tri(t, mL, tail, mT)
            val wl = floatArrayOf(-1.6f, -0.12f, 0.9f); val wr = floatArrayOf(1.6f, -0.12f, 0.9f)
            tri(t, mL, wl, tail); tri(t, mR, tail, wr)
            tri(t, mT, floatArrayOf(0f, 0.7f, 0.9f), tail)
            return t.toFloatArray()
        }

        /** Ally/convoy freighter: cab + pods + engine block. */
        fun freighter(): FloatArray {
            val t = ArrayList<Float>()
            box(t, 0f, 0.05f, -1.35f, 0.30f, 0.26f, 0.35f)
            tri(t, floatArrayOf(0f, 0.05f, -2.0f),
                floatArrayOf(-0.30f, -0.18f, -1.7f), floatArrayOf(0.30f, -0.18f, -1.7f))
            box(t, 0f, -0.05f, 0.1f, 0.10f, 0.10f, 1.25f)
            box(t, -0.38f, 0.16f, -0.35f, 0.26f, 0.20f, 0.48f)
            box(t, 0.38f, 0.16f, -0.35f, 0.26f, 0.20f, 0.48f)
            box(t, -0.38f, 0.16f, 0.65f, 0.26f, 0.20f, 0.48f)
            box(t, 0.38f, 0.16f, 0.65f, 0.26f, 0.20f, 0.48f)
            box(t, 0f, 0f, 1.45f, 0.34f, 0.30f, 0.22f)
            return t.toFloatArray()
        }

        /** Empire lance: heavy pursuer — long spine, twin prongs. */
        fun lance(): FloatArray {
            val t = ArrayList<Float>()
            box(t, 0f, 0f, 0f, 0.28f, 0.34f, 2.1f)                 // spine
            box(t, -0.65f, 0f, -1.5f, 0.16f, 0.16f, 0.9f)          // prongs
            box(t, 0.65f, 0f, -1.5f, 0.16f, 0.16f, 0.9f)
            tri(t, floatArrayOf(-0.65f, 0f, -2.6f), floatArrayOf(-0.5f, 0.14f, -2.3f),
                floatArrayOf(-0.8f, -0.14f, -2.3f))
            tri(t, floatArrayOf(0.65f, 0f, -2.6f), floatArrayOf(0.8f, 0.14f, -2.3f),
                floatArrayOf(0.5f, -0.14f, -2.3f))
            box(t, 0f, 0.55f, 0.8f, 0.10f, 0.30f, 0.5f)            // conning fin
            box(t, 0f, 0f, 2.3f, 0.45f, 0.45f, 0.25f)              // engine block
            return t.toFloatArray()
        }
    }

    private val vsrc = """
        uniform mat4 uMVP;
        uniform mat4 uModel;
        attribute vec3 aPos;
        attribute vec3 aNormal;
        varying vec3 vN;
        varying vec3 vWorld;
        void main() {
            vN = normalize((uModel * vec4(aNormal, 0.0)).xyz);
            vWorld = (uModel * vec4(aPos, 1.0)).xyz;
            gl_Position = uMVP * vec4(aPos, 1.0);
        }
    """
    private val fsrc = """
        precision mediump float;
        varying vec3 vN;
        varying vec3 vWorld;
        uniform vec3 uSunPos;
        uniform vec3 uCamPos;
        uniform float uSunlight;
        uniform vec3 uHull;
        uniform float uEmissive;
        void main() {
            vec3 N = normalize(vN);
            vec3 L = normalize(uSunPos - vWorld);
            vec3 V = normalize(uCamPos - vWorld);
            float ndl = max(dot(N, L), 0.0);
            float rim = pow(1.0 - max(dot(N, V), 0.0), 2.5);
            vec3 col = uHull * (0.20 + 1.0 * ndl * min(uSunlight, 1.6))
                     + vec3(0.30, 0.45, 0.62) * rim * 0.4
                     + uHull * 0.10
                     + vec3(1.0, 0.25, 0.15) * uEmissive * (0.4 + rim);
            gl_FragColor = vec4(col, 1.0);
        }
    """
    private var prog = 0
    private var aPos = 0; private var aNormal = 0
    private var uMVP = 0; private var uModel = 0; private var uSunPos = 0; private var uCamPos = 0
    private var uSunlight = 0; private var uHull = 0; private var uEmissive = 0
    private val meshes = arrayOfNulls<FloatBuffer>(3)
    private val verts = IntArray(3)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    fun init() {
        prog = GlAssets.compileProgram(vsrc, fsrc)
        aPos = GLES20.glGetAttribLocation(prog, "aPos")
        aNormal = GLES20.glGetAttribLocation(prog, "aNormal")
        uMVP = GLES20.glGetUniformLocation(prog, "uMVP")
        uModel = GLES20.glGetUniformLocation(prog, "uModel")
        uSunPos = GLES20.glGetUniformLocation(prog, "uSunPos")
        uCamPos = GLES20.glGetUniformLocation(prog, "uCamPos")
        uSunlight = GLES20.glGetUniformLocation(prog, "uSunlight")
        uHull = GLES20.glGetUniformLocation(prog, "uHull")
        uEmissive = GLES20.glGetUniformLocation(prog, "uEmissive")
        val ms = arrayOf(dart(), freighter(), lance())
        for (i in ms.indices) {
            meshes[i] = GlAssets.floatBuffer(ms[i]); verts[i] = ms[i].size / 6
        }
    }

    /** Orientation from forward vector + roll bank; uniform scale. */
    fun buildModel(pos: FloatArray, f: FloatArray, bank: Float, scale: Float): FloatArray {
        var r0 = -f[2]; var r1 = 0f; var r2 = f[0]
        val rl = sqrt(r0 * r0 + r1 * r1 + r2 * r2).coerceAtLeast(1e-4f)
        r0 /= rl; r1 /= rl; r2 /= rl
        var u0 = r1 * f[2] - r2 * f[1]; var u1 = r2 * f[0] - r0 * f[2]; var u2 = r0 * f[1] - r1 * f[0]
        val cb = cos(bank); val sb = sin(bank)
        val ux = u0 * cb + r0 * sb; val uy = u1 * cb + r1 * sb; val uz = u2 * cb + r2 * sb
        r0 = f[1] * uz - f[2] * uy; r1 = f[2] * ux - f[0] * uz; r2 = f[0] * uy - f[1] * ux
        model[0] = r0 * scale; model[1] = r1 * scale; model[2] = r2 * scale; model[3] = 0f
        model[4] = ux * scale; model[5] = uy * scale; model[6] = uz * scale; model[7] = 0f
        model[8] = -f[0] * scale; model[9] = -f[1] * scale; model[10] = -f[2] * scale; model[11] = 0f
        model[12] = pos[0]; model[13] = pos[1]; model[14] = pos[2]; model[15] = 1f
        return model
    }

    fun draw(vp: FloatArray, meshIdx: Int, camPos: FloatArray, sunPos: FloatArray,
             sunlight: Float, hull: FloatArray, emissive: Float) {
        val mesh = meshes[meshIdx] ?: return
        GLES20.glUseProgram(prog)
        android.opengl.Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform3fv(uSunPos, 1, sunPos, 0)
        GLES20.glUniform3fv(uCamPos, 1, camPos, 0)
        GLES20.glUniform1f(uSunlight, sunlight)
        GLES20.glUniform3fv(uHull, 1, hull, 0)
        GLES20.glUniform1f(uEmissive, emissive)
        mesh.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, mesh)
        GLES20.glEnableVertexAttribArray(aPos)
        mesh.position(3)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, mesh)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, verts[meshIdx])
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aNormal)
    }
}
