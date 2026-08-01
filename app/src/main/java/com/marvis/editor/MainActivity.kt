package com.marvis.editor

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val info = buildString {
            appendLine("Code Editor v40")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine()
            appendLine("All good - activity loaded successfully.")
        }

        val scroll = ScrollView(this)
        val text = TextView(this).apply {
            textSize = 14f
            setPadding(48, 48, 48, 48)
            text = info
        }
        scroll.addView(text)
        setContentView(scroll)
    }
}