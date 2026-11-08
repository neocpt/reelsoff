package com.neo.reelsoff

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : android.app.Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "ReelsOff"
            textSize = 28f
            gravity = Gravity.START
        })

        root.addView(TextView(this).apply {
            text = "Coupe les formats courts. Les reels reçus en message privé restent visibles."
            setPadding(0, pad / 2, 0, pad)
        })

        status = TextView(this).apply { setPadding(0, 0, 0, pad / 2) }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Ouvrir les réglages d'accessibilité"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(TextView(this).apply {
            text = "Fonctionnalités à bloquer"
            textSize = 18f
            setPadding(0, pad * 2, 0, pad / 2)
        })

        val prefs = Prefs.get(this)
        for (rule in Rules.all) {
            root.addView(Switch(this).apply {
                text = rule.label
                textSize = 16f
                setPadding(0, pad / 2, 0, pad / 2)
                isChecked = prefs.getBoolean(rule.prefKey, true)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(rule.prefKey, checked).apply()
                }
            })
        }

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled()) {
            "✅ Service actif"
        } else {
            "⚠️ Service inactif : active « ReelsOff » dans les réglages d'accessibilité"
        }
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val component = ComponentName(this, BlockerService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }
}
