package com.shadimran.hack

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SensitivityActivity : Activity() {

    private lateinit var out: TextView
    private var newSens = 0f

    private fun field(root: LinearLayout, title: String, default: String): EditText {
        root.addView(label(this, title, 13f).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(14) }
        })
        val e = EditText(this).apply {
            setText(default)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Neon.TEXT)
            background = neonBox(this@SensitivityActivity, Neon.BG)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(e, LinearLayout.LayoutParams(MATCH, WRAP))
        return e
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Neon.BG)
            addView(root)
            applySystemBarPadding()
        })

        root.addView(label(this, "SENSITIVITY CALCULATOR", 22f, Neon.RED, true))
        root.addView(label(this, "Keeps your effective sensitivity (DPI x sens) the same when DPI changes.", 12f).apply { alpha = 0.7f })

        val oldDpi = field(root, "Current DPI", "420")
        val oldSens = field(root, "Current in-game sensitivity (0-100)", "50")
        val newDpi = field(root, "New DPI", "480")

        out = label(this, "", 15f).apply {
            background = neonBox(this@SensitivityActivity)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(18) }
        }

        root.addView(neonButton(this, "Calculate") {
            val d1 = oldDpi.text.toString().toFloatOrNull()
            val s1 = oldSens.text.toString().toFloatOrNull()
            val d2 = newDpi.text.toString().toFloatOrNull()
            if (d1 == null || s1 == null || d2 == null || d2 <= 0f) {
                Toast.makeText(this, "Enter valid numbers", Toast.LENGTH_SHORT).show()
            } else {
                val edpi = d1 * s1
                newSens = edpi / d2
                out.text = "eDPI: ${"%.1f".format(edpi)}\n" +
                    "New sensitivity: ${"%.1f".format(newSens)}\n" +
                    "Simulator multiplier: ${"%.2f".format((newSens / 50f).coerceIn(0.2f, 3f))}x"
            }
        })
        root.addView(out)
        root.addView(neonButton(this, "Apply to simulator") {
            if (newSens > 0f) {
                Prefs.setSens(this, (newSens / 50f).coerceIn(0.2f, 3f))
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Calculate first", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
