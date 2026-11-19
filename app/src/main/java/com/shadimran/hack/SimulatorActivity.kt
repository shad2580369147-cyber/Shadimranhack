package com.shadimran.hack

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class SimulatorActivity : Activity() {

    private lateinit var sim: SimulatorView

    private fun seek(max: Int, progress: Int, onChange: (Int) -> Unit) = SeekBar(this).apply {
        this.max = max
        this.progress = progress
        progressTintList = ColorStateList.valueOf(Neon.RED)
        thumbTintList = ColorStateList.valueOf(Neon.RED)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, user: Boolean) { onChange(p) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        layoutParams = LinearLayout.LayoutParams(dp(200), WRAP)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sim = SimulatorView(this)
        val root = FrameLayout(this)
        root.addView(sim, FrameLayout.LayoutParams(MATCH, MATCH))

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val espB = chip(this, "ESP") { b -> sim.esp = !sim.esp; setToggle(b, sim.esp) }
        val lockB = chip(this, "Aim-lock") { b -> sim.lock = !sim.lock; setToggle(b, sim.lock) }
        val headB = chip(this, "Headshot") { b -> sim.headOnly = !sim.headOnly; setToggle(b, sim.headOnly) }
        setToggle(espB, sim.esp)
        setToggle(lockB, sim.lock)
        setToggle(headB, sim.headOnly)
        bar.addView(espB); bar.addView(lockB); bar.addView(headB)
        bar.addView(chip(this, "Reset") { sim.reset() })
        bar.addView(chip(this, "Exit") { finish() })
        root.addView(bar, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.START).apply {
            leftMargin = dp(28); topMargin = dp(14)
        })

        val fovLabel = TextView(this).apply { setTextColor(Neon.TEXT); textSize = 12f }
        val sensLabel = TextView(this).apply { setTextColor(Neon.TEXT); textSize = 12f }
        fun updFov() { fovLabel.text = "FOV ${sim.fov.toInt()}" }
        fun updSens() { sensLabel.text = "Sensitivity ${"%.2f".format(sim.sens)}x" }
        updFov(); updSens()
        val sliders = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(fovLabel)
            addView(seek(60, (sim.fov - 60f).toInt()) { p -> sim.fov = 60f + p; Prefs.setFov(this@SimulatorActivity, sim.fov); updFov() })
            addView(sensLabel)
            addView(seek(100, ((sim.sens - 0.2f) / 0.028f).toInt().coerceIn(0, 100)) { p ->
                sim.sens = 0.2f + p * 0.028f; Prefs.setSens(this@SimulatorActivity, sim.sens); updSens()
            })
        }
        root.addView(sliders, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = dp(28); bottomMargin = dp(16)
        })

        val fire = TextView(this).apply {
            text = "FIRE"
            gravity = Gravity.CENTER
            setTextColor(Neon.RED)
            textSize = 16f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Neon.BG)
                setStroke(dp(3), Neon.RED)
            }
            setOnTouchListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) sim.fire()
                true
            }
        }
        root.addView(fire, FrameLayout.LayoutParams(dp(96), dp(96), Gravity.BOTTOM or Gravity.END).apply {
            rightMargin = dp(36); bottomMargin = dp(24)
        })

        setContentView(root)

        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()
        Prefs.saveBest(this, sim.accuracy(), sim.score, sim.shots)
    }
}
