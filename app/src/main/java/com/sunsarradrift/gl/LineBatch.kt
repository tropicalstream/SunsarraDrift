package com.sunsarradrift.gl

import android.opengl.GLES20
import java.nio.FloatBuffer

/**
 * The whole aesthetic in one class: batched, additive, glowing vector lines —
 * an homage to the XY monitors Tempest was born on. Each frame the batch is
 * refilled and drawn TWICE: wide + faint (halo), thin + bright (core).
 */
class LineBatch(
    private val maxLines: Int = 6000,
    private val haloWidth: Float = 6f,       // blur pass width
    private val haloGain: Float = 0.35f      // blur pass brightness
) {
    private val vsrc = """
        uniform mat4 uVP;
        attribute vec3 aPos;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            vColor = aColor;
            gl_Position = uVP * vec4(aPos, 1.0);
        }
    """
    private val fsrc = """
        precision mediump float;
        varying vec4 vColor;
        uniform float uGain;
        void main() { gl_FragColor = vec4(vColor.rgb * uGain, vColor.a); }
    """
    private var prog = 0
    private var aPos = 0; private var aColor = 0; private var uVP = 0; private var uGain = 0
    private val data = FloatArray(maxLines * 2 * 7)
    private lateinit var buf: FloatBuffer
    private var fi = 0

    // point sprites for vertex glow
    private val pvsrc = """
        uniform mat4 uVP;
        attribute vec4 aPosSize;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            vColor = aColor;
            gl_Position = uVP * vec4(aPosSize.xyz, 1.0);
            gl_PointSize = aPosSize.w;
        }
    """
    private val pfsrc = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            float a = smoothstep(0.5, 0.05, length(gl_PointCoord - vec2(0.5)));
            gl_FragColor = vec4(vColor.rgb, vColor.a * a);
        }
    """
    private var pProg = 0
    private var paPosSize = 0; private var paColor = 0; private var puVP = 0
    private val pData = FloatArray(2048 * 8)
    private lateinit var pBuf: FloatBuffer
    private var pi = 0

    fun init() {
        prog = com.sunsarradrift.GlAssets.compileProgram(vsrc, fsrc)
        aPos = GLES20.glGetAttribLocation(prog, "aPos")
        aColor = GLES20.glGetAttribLocation(prog, "aColor")
        uVP = GLES20.glGetUniformLocation(prog, "uVP")
        uGain = GLES20.glGetUniformLocation(prog, "uGain")
        buf = com.sunsarradrift.GlAssets.floatBuffer(data.size)
        pProg = com.sunsarradrift.GlAssets.compileProgram(pvsrc, pfsrc)
        paPosSize = GLES20.glGetAttribLocation(pProg, "aPosSize")
        paColor = GLES20.glGetAttribLocation(pProg, "aColor")
        puVP = GLES20.glGetUniformLocation(pProg, "uVP")
        pBuf = com.sunsarradrift.GlAssets.floatBuffer(pData.size)
    }

    fun begin() { fi = 0; pi = 0 }

    fun line(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
             r: Float, g: Float, b: Float, a: Float) {
        if (fi + 14 > data.size) return
        data[fi++] = x0; data[fi++] = y0; data[fi++] = z0
        data[fi++] = r; data[fi++] = g; data[fi++] = b; data[fi++] = a
        data[fi++] = x1; data[fi++] = y1; data[fi++] = z1
        data[fi++] = r; data[fi++] = g; data[fi++] = b; data[fi++] = a
    }

    fun glow(x: Float, y: Float, z: Float, size: Float, r: Float, g: Float, b: Float, a: Float) {
        if (pi + 8 > pData.size) return
        pData[pi++] = x; pData[pi++] = y; pData[pi++] = z; pData[pi++] = size
        pData[pi++] = r; pData[pi++] = g; pData[pi++] = b; pData[pi++] = a
    }

    fun draw(vp: FloatArray) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)   // additive: the neon lie
        GLES20.glDepthMask(false)
        if (fi > 0) {
            buf.position(0); buf.put(data, 0, fi); buf.position(0)
            GLES20.glUseProgram(prog)
            GLES20.glUniformMatrix4fv(uVP, 1, false, vp, 0)
            buf.position(0)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 28, buf)
            GLES20.glEnableVertexAttribArray(aPos)
            buf.position(3)
            GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 28, buf)
            GLES20.glEnableVertexAttribArray(aColor)
            // pass 1: halo (configurable — blocks use a 30%-reduced blur)
            GLES20.glLineWidth(haloWidth)
            GLES20.glUniform1f(uGain, haloGain)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, fi / 7)
            // pass 2: core
            GLES20.glLineWidth(2.2f)
            GLES20.glUniform1f(uGain, 1.35f)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, fi / 7)
            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glDisableVertexAttribArray(aColor)
        }
        if (pi > 0) {
            pBuf.position(0); pBuf.put(pData, 0, pi); pBuf.position(0)
            GLES20.glUseProgram(pProg)
            GLES20.glUniformMatrix4fv(puVP, 1, false, vp, 0)
            pBuf.position(0)
            GLES20.glVertexAttribPointer(paPosSize, 4, GLES20.GL_FLOAT, false, 32, pBuf)
            GLES20.glEnableVertexAttribArray(paPosSize)
            pBuf.position(4)
            GLES20.glVertexAttribPointer(paColor, 4, GLES20.GL_FLOAT, false, 32, pBuf)
            GLES20.glEnableVertexAttribArray(paColor)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pi / 8)
            GLES20.glDisableVertexAttribArray(paPosSize)
            GLES20.glDisableVertexAttribArray(paColor)
        }
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
