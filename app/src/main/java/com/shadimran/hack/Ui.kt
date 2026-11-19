package com.shadimran.hack

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object Neon {
    val BG = Color.parseColor("#0A0A0A")
    val RED = Color.parseColor("#FF1744")
    val DIM = Color.parseColor("#661020")
    val CARD = Color.parseColor("#14080A")
    val TEXT = Color.parseColor("#F2F2F2")
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun View.applySystemBarPadding() {
    setOnApplyWindowInsetsListener { v, insets ->
        val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        v.setPadding(b.left, b.top, b.right, b.bottom)
        WindowInsets.CONSUMED
    }
}

fun neonBox(ctx: Context, fill: Int = Neon.CARD, stroke: Int = Neon.RED): GradientDrawable =
    GradientDrawable().apply {
        setColor(fill)
        setStroke(ctx.dp(1), stroke)
        cornerRadius = ctx.dp(10).toFloat()
    }

fun label(ctx: Context, text: String, size: Float = 14f, color: Int = Neon.TEXT, bold: Boolean = false) =
    TextView(ctx).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

fun neonButton(ctx: Context, text: String, onClick: () -> Unit): Button = Button(ctx).apply {
    this.text = text
    isAllCaps = true
    setTextColor(Neon.RED)
    typeface = Typeface.DEFAULT_BOLD
    background = neonBox(ctx, Neon.BG)
    stateListAnimator = null
    layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ctx.dp(10) }
    setOnClickListener { onClick() }
}

fun chip(ctx: Context, text: String, onClick: (Button) -> Unit): Button = Button(ctx).apply {
    this.text = text
    textSize = 12f
    isAllCaps = true
    minWidth = 0
    minHeight = 0
    minimumWidth = 0
    minimumHeight = 0
    setPadding(ctx.dp(14), ctx.dp(8), ctx.dp(14), ctx.dp(8))
    typeface = Typeface.DEFAULT_BOLD
    setTextColor(Neon.RED)
    background = neonBox(ctx, Neon.BG)
    stateListAnimator = null
    layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = ctx.dp(8) }
    setOnClickListener { onClick(this) }
}

fun setToggle(b: Button, on: Boolean) {
    b.setTextColor(if (on) Neon.BG else Neon.RED)
    (b.background as GradientDrawable).setColor(if (on) Neon.RED else Neon.BG)
}

fun drawCrosshair(c: Canvas, cx: Float, cy: Float, size: Float, style: Int, p: Paint) {
    val r = size / 2f
    p.color = Neon.RED
    p.strokeWidth = max(2f, size / 14f)
    when (style) {
        0 -> {
            p.style = Paint.Style.FILL
            c.drawCircle(cx, cy, r * 0.18f, p)
        }
        1 -> {
            p.style = Paint.Style.STROKE
            val gap = r * 0.25f
            c.drawLine(cx - r, cy, cx - gap, cy, p)
            c.drawLine(cx + gap, cy, cx + r, cy, p)
            c.drawLine(cx, cy - r, cx, cy - gap, p)
            c.drawLine(cx, cy + gap, cx, cy + r, p)
        }
        else -> {
            p.style = Paint.Style.STROKE
            c.drawCircle(cx, cy, r * 0.8f, p)
            p.style = Paint.Style.FILL
            c.drawCircle(cx, cy, r * 0.12f, p)
        }
    }
}

class CrosshairView(ctx: Context, private val style: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        drawCrosshair(canvas, width / 2f, height / 2f, minOf(width, height) * 0.9f, style, p)
    }
}

object Prefs {
    private fun sp(c: Context): SharedPreferences = c.getSharedPreferences("shad", Context.MODE_PRIVATE)

    fun style(c: Context) = sp(c).getInt("style", 1)
    fun setStyle(c: Context, v: Int) = sp(c).edit().putInt("style", v).apply()

    fun sizeDp(c: Context) = sp(c).getInt("size", 40)
    fun setSizeDp(c: Context, v: Int) = sp(c).edit().putInt("size", v).apply()

    fun sens(c: Context) = sp(c).getFloat("sens", 1f)
    fun setSens(c: Context, v: Float) = sp(c).edit().putFloat("sens", v).apply()

    fun fov(c: Context) = sp(c).getFloat("fov", 90f)
    fun setFov(c: Context, v: Float) = sp(c).edit().putFloat("fov", v).apply()

    fun bestAcc(c: Context) = sp(c).getInt("bestAcc", 0)
    fun bestScore(c: Context) = sp(c).getInt("bestScore", 0)
    fun saveBest(c: Context, acc: Int, score: Int, shots: Int) {
        val e = sp(c).edit()
        if (shots >= 10 && acc > bestAcc(c)) e.putInt("bestAcc", acc)
        if (score > bestScore(c)) e.putInt("bestScore", score)
        e.apply()
    }
}
