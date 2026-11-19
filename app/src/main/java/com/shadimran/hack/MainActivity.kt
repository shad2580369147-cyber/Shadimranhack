package com.shadimran.hack

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var info: TextView
    private lateinit var overlayBtn: Button
    private lateinit var styleBtn: Button
    private lateinit var sizeLabel: TextView
    private val styleNames = arrayOf("Dot", "Cross", "Ring")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Neon.BG)
            addView(root)
            applySystemBarPadding()
        }
        setContentView(scroll)

        root.addView(label(this, "SHAD IMRAN HACK", 28f, Neon.RED, true))
        root.addView(label(this, "Aim training suite  |  Android 15", 13f, Neon.TEXT))

        info = label(this, "", 14f).apply {
            background = neonBox(this@MainActivity)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) }
        }
        root.addView(info)

        root.addView(neonButton(this, "Training simulator") {
            startActivity(Intent(this, SimulatorActivity::class.java))
        })
        root.addView(neonButton(this, "Sensitivity calculator") {
            startActivity(Intent(this, SensitivityActivity::class.java))
        })

        overlayBtn = neonButton(this, "") { toggleOverlay() }
        root.addView(overlayBtn)

        styleBtn = neonButton(this, "") {
            Prefs.setStyle(this, (Prefs.style(this) + 1) % styleNames.size)
            refreshOverlay()
            updateLabels()
        }
        root.addView(styleBtn)

        sizeLabel = label(this, "", 14f).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(16) }
        }
        root.addView(sizeLabel)
        root.addView(SeekBar(this).apply {
            max = 80
            progress = Prefs.sizeDp(this@MainActivity) - 20
            progressTintList = android.content.res.ColorStateList.valueOf(Neon.RED)
            thumbTintList = android.content.res.ColorStateList.valueOf(Neon.RED)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, user: Boolean) {
                    Prefs.setSizeDp(this@MainActivity, p + 20)
                    updateLabels()
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) { refreshOverlay() }
            })
        })

        root.addView(label(
            this,
            "The simulator is self-contained and is not connected to any game. " +
                "Using overlays on top of online games may violate that game's rules - use at your own risk.",
            11f, Neon.TEXT
        ).apply {
            gravity = Gravity.START
            alpha = 0.6f
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(20) }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshInfo()
        updateLabels()
    }

    private fun updateLabels() {
        overlayBtn.text = if (CrosshairOverlayService.running) "Stop crosshair overlay" else "Start crosshair overlay"
        styleBtn.text = "Crosshair style: " + styleNames[Prefs.style(this)]
        sizeLabel.text = "Crosshair size: ${Prefs.sizeDp(this)} dp"
    }

    private fun refreshInfo() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val pct = bat?.let {
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / scale
        } ?: -1
        val hz = display?.refreshRate ?: 60f
        info.text = buildString {
            appendLine("DASHBOARD")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Display: ${"%.0f".format(hz)} Hz")
            appendLine("RAM free: ${mi.availMem / 1048576} / ${mi.totalMem / 1048576} MB")
            appendLine("Battery: $pct%")
            appendLine("Best accuracy: ${Prefs.bestAcc(this@MainActivity)}%")
            append("Best score: ${Prefs.bestScore(this@MainActivity)}")
        }
    }

    private fun toggleOverlay() {
        val svc = Intent(this, CrosshairOverlayService::class.java)
        if (CrosshairOverlayService.running) {
            stopService(svc)
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow 'Display over other apps', then come back", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            startForegroundService(svc)
        }
        overlayBtn.postDelayed({ updateLabels() }, 400)
    }

    private fun refreshOverlay() {
        if (CrosshairOverlayService.running) {
            startForegroundService(Intent(this, CrosshairOverlayService::class.java))
        }
    }
}
