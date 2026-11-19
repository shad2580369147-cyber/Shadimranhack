package com.shadimran.hack

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

class SimulatorView(ctx: Context) : View(ctx) {

    private class Target(var x: Float, var y: Float, var vx: Float)

    private val targets = ArrayList<Target>()
    private val rnd = Random()

    var esp = true
    var lock = false
    var headOnly = false
    var fov = Prefs.fov(ctx)
    var sens = Prefs.sens(ctx)
    var crosshairStyle = Prefs.style(ctx)

    var shots = 0
        private set
    var hits = 0
        private set
    var heads = 0
        private set
    var score = 0
        private set

    private var aimX = -1f
    private var aimY = -1f
    private var lastNs = 0L
    private var lastTx = 0f
    private var lastTy = 0f
    private var msg = ""
    private var msgUntil = 0L

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private val rect = RectF()

    private fun d(v: Float) = v * resources.displayMetrics.density

    fun accuracy(): Int = if (shots == 0) 0 else hits * 100 / shots

    fun reset() {
        shots = 0; hits = 0; heads = 0; score = 0
        targets.clear()
        repeat(3) { spawn() }
    }

    private fun th() = height * 0.34f * (90f / fov)
    private fun headR() = th() * 0.085f
    private fun torsoTop(t: Target) = t.y - th() * 0.20f
    private fun torsoBottom(t: Target) = t.y + th() * 0.40f
    private fun halfW() = th() * 0.13f
    private fun headCy(t: Target) = torsoTop(t) - headR() * 1.15f

    private fun spawn() {
        val speed = width * (0.06f + 0.10f * rnd.nextFloat())
        targets.add(
            Target(
                width * (0.15f + 0.70f * rnd.nextFloat()),
                height * (0.45f + 0.15f * rnd.nextFloat()),
                if (rnd.nextBoolean()) speed else -speed
            )
        )
    }

    private fun flash(text: String) {
        msg = text
        msgUntil = SystemClock.elapsedRealtime() + 700
    }

    fun fire() {
        shots++
        var result = 0
        var hit: Target? = null
        for (t in targets) {
            if (hypot(aimX - t.x, aimY - headCy(t)) <= headR() * 1.1f) {
                result = 2; hit = t; break
            }
            if (abs(aimX - t.x) <= halfW() && aimY >= torsoTop(t) && aimY <= torsoBottom(t)) {
                result = 1; hit = t
            }
        }
        when {
            result == 2 -> {
                heads++; hits++; score += 100
                flash("HEADSHOT +100")
                hit?.let { targets.remove(it) }; spawn()
            }
            result == 1 && !headOnly -> {
                hits++; score += 25
                flash("HIT +25")
                hit?.let { targets.remove(it) }; spawn()
            }
            result == 1 -> flash("BODY - NO SCORE")
            else -> flash("MISS")
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (aimX < 0f) { aimX = w / 2f; aimY = h / 2f }
        if (targets.isEmpty() && w > 0) repeat(3) { spawn() }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.getX(0); lastTy = e.getY(0) }
            MotionEvent.ACTION_MOVE -> {
                val x = e.getX(0); val y = e.getY(0)
                aimX = (aimX + (x - lastTx) * sens * 1.6f).coerceIn(0f, width.toFloat())
                aimY = (aimY + (y - lastTy) * sens * 1.6f).coerceIn(0f, height.toFloat())
                lastTx = x; lastTy = y
            }
        }
        return true
    }

    private fun step(dt: Float) {
        for (t in targets) {
            t.x += t.vx * dt
            if (t.x < width * 0.08f) { t.x = width * 0.08f; t.vx = abs(t.vx) }
            else if (t.x > width * 0.92f) { t.x = width * 0.92f; t.vx = -abs(t.vx) }
            if (rnd.nextFloat() < dt * 0.4f) t.vx = -t.vx
        }
        if (lock) {
            val n = targets.minByOrNull { hypot(it.x - aimX, headCy(it) - aimY) }
            if (n != null) {
                val k = 1f - exp(-9f * dt)
                aimX += (n.x - aimX) * k
                aimY += (headCy(n) - aimY) * k
            }
        }
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (lastNs == 0L) 0.016f else ((now - lastNs) / 1e9f).coerceAtMost(0.05f)
        lastNs = now
        step(dt)

        c.drawColor(Neon.BG)
        drawGrid(c)
        for (t in targets) drawTarget(c, t)
        if (esp) drawEsp(c)
        drawRadar(c)
        drawCrosshair(c, aimX, aimY, d(36f), crosshairStyle, p)
        drawHud(c)
        postInvalidateOnAnimation()
    }

    private fun drawGrid(c: Canvas) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        p.color = Color.parseColor("#2A0A10")
        val step = d(48f)
        var x = 0f
        while (x < width) { c.drawLine(x, 0f, x, height.toFloat(), p); x += step }
        var y = 0f
        while (y < height) { c.drawLine(0f, y, width.toFloat(), y, p); y += step }
        p.color = Neon.DIM
        c.drawLine(0f, height * 0.5f, width.toFloat(), height * 0.5f, p)
    }

    private fun drawTarget(c: Canvas, t: Target) {
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#7A0E1C")
        rect.set(t.x - halfW(), torsoTop(t), t.x + halfW(), torsoBottom(t))
        c.drawRoundRect(rect, d(6f), d(6f), p)
        p.color = Color.parseColor("#C21A30")
        c.drawCircle(t.x, headCy(t), headR(), p)
    }

    private fun drawEsp(c: Canvas) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = d(1.5f)
        for (t in targets) {
            p.color = Neon.RED
            val top = headCy(t) - headR()
            rect.set(t.x - halfW() * 1.3f, top, t.x + halfW() * 1.3f, torsoBottom(t))
            c.drawRect(rect, p)
            p.color = Neon.DIM
            c.drawLine(width / 2f, height.toFloat(), t.x, torsoBottom(t), p)
            p.color = Color.WHITE
            c.drawCircle(t.x, headCy(t), headR() * 1.1f, p)
            tp.color = Neon.RED
            tp.textSize = d(11f)
            c.drawText("ENEMY", rect.left, top - d(4f), tp)
        }
    }

    private fun drawRadar(c: Canvas) {
        val r = d(56f)
        val cx = width - d(28f) - r
        val cy = d(60f) + r
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#CC120508")
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = d(1.5f)
        p.color = Neon.RED
        c.drawCircle(cx, cy, r, p)
        p.color = Neon.DIM
        c.drawLine(cx - r, cy, cx + r, cy, p)
        c.drawLine(cx, cy - r, cx, cy + r, p)
        c.drawCircle(cx, cy, r * 0.5f, p)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        c.drawCircle(cx, cy, d(2.5f), p)
        p.color = Neon.RED
        for (t in targets) {
            val rx = cx + (t.x / width - 0.5f) * 2f * r * 0.9f
            val ry = cy + (t.y / height - 0.5f) * 2f * r * 0.9f
            c.drawCircle(rx, ry, d(3.5f), p)
        }
    }

    private fun drawHud(c: Canvas) {
        tp.color = Neon.TEXT
        tp.textSize = d(14f)
        val x = d(28f)
        var y = d(96f)
        val mode = (if (headOnly) "HEADSHOT" else "NORMAL") + (if (lock) " + LOCK" else "")
        for (line in listOf(
            "SCORE  $score",
            "ACC    ${accuracy()}%  ($hits/$shots)",
            "HEADS  $heads",
            "MODE   $mode",
            "FOV ${fov.toInt()}   SENS ${"%.2f".format(sens)}x"
        )) {
            c.drawText(line, x, y, tp)
            y += d(20f)
        }
        if (SystemClock.elapsedRealtime() < msgUntil) {
            tp.color = Neon.RED
            tp.textSize = d(22f)
            tp.textAlign = Paint.Align.CENTER
            c.drawText(msg, width / 2f, height * 0.2f, tp)
            tp.textAlign = Paint.Align.LEFT
        }
    }
}
