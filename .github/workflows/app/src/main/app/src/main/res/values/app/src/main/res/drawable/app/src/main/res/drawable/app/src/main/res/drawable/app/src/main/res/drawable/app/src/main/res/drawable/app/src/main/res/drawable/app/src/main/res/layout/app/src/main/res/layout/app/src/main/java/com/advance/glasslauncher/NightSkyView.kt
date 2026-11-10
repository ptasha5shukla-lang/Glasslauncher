package com.advance.glasslauncher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin
import kotlin.random.Random

class NightSkyView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val n = 200
    private val rnd = Random(7)
    private val sx = FloatArray(n) { rnd.nextFloat() }
    private val sy = FloatArray(n) { rnd.nextFloat() * 0.72f }
    private val sr = FloatArray(n) { 0.6f + rnd.nextFloat() * 1.9f }
    private val sp = FloatArray(n) { rnd.nextFloat() * 6.2832f }
    private var phase = 0f
    private var sky: Shader? = null
    private var glow: Shader? = null
    private val moon = Path()
    private val farHills = Path()
    private val nearHills = Path()
    private var mx = 0f
    private var my = 0f
    private var mr = 0f

    private val anim = ValueAnimator.ofFloat(0f, 6.2832f).apply {
        duration = 4500
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val wf = w.toFloat(); val hf = h.toFloat()
        sky = LinearGradient(0f, 0f, 0f, hf,
            intArrayOf(0xFF01020A.toInt(), 0xFF0A1030.toInt(), 0xFF1A2255.toInt()),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        mx = wf * 0.78f; my = hf * 0.22f; mr = hf * 0.07f
        glow = RadialGradient(mx, my, mr * 5, 0x55CFD8FF, 0x00CFD8FF, Shader.TileMode.CLAMP)
        moon.reset()
        moon.addCircle(mx, my, mr, Path.Direction.CW)
        val cut = Path().apply { addCircle(mx + mr * 0.5f, my - mr * 0.1f, mr * 0.88f, Path.Direction.CW) }
        moon.op(cut, Path.Op.DIFFERENCE)
        buildHills(farHills, wf, hf, hf * 0.80f, hf * 0.06f, 1.3f, 0.5f)
        buildHills(nearHills, wf, hf, hf * 0.90f, hf * 0.05f, 2.1f, 2.0f)
    }

    private fun buildHills(p: Path, w: Float, h: Float, base: Float, amp: Float, freq: Float, off: Float) {
        p.reset(); p.moveTo(0f, h)
        val steps = 48
        for (i in 0..steps) {
            val x = w * i / steps
            val y = base - amp * (0.6f * sin(i / steps.toFloat() * 6.2832f * freq + off) +
                0.4f * sin(i / steps.toFloat() * 6.2832f * freq * 2.3f + off * 1.7f))
            p.lineTo(x, y)
        }
        p.lineTo(w, h); p.close()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = sky; canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        for (i in 0 until n) {
            val tw = 0.55f + 0.45f * sin(phase + sp[i])
            paint.color = Color.argb((255 * tw * (0.45f + sr[i] / 4f)).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(sx[i] * w, sy[i] * h, sr[i], paint)
        }
        paint.shader = glow; canvas.drawCircle(mx, my, mr * 5, paint)
        paint.shader = null
        paint.color = 0xFFF4F1E1.toInt(); canvas.drawPath(moon, paint)
        paint.color = 0xFF0C1030.toInt(); canvas.drawPath(farHills, paint)
        paint.color = 0xFF04060F.toInt(); canvas.drawPath(nearHills, paint)
    }
}
