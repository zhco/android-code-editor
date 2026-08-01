package com.marvis.editor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.marvis.editor.databinding.ActivityMainBinding
import com.marvis.editor.editor.EditorFragment
import com.marvis.editor.filetree.FileTreeFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var fileTreeFragment: FileTreeFragment? = null
    private var editorFragment: EditorFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            fileTreeFragment = FileTreeFragment { file ->
                binding.drawerLayout.close()
                editorFragment?.openFile(file)
            }
            editorFragment = EditorFragment()

            supportFragmentManager.beginTransaction()
                .replace(R.id.file_tree_container, fileTreeFragment!!)
                .replace(R.id.editor_container, editorFragment!!)
                .commit()
        } catch (e: Exception) {
            Log.e("MainActivity", "Startup crash", e)
            showCrashScreen(e)
        }
    }

    private fun showCrashScreen(e: Exception) {
        val scroll = ScrollView(this)
        val text = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(32, 32, 32, 32)
            text = "App crashed on startup:\n\n${e.javaClass.name}\n${e.message}\n\n"
            for (line in e.stackTrace.take(20)) {
                append("${line}\n")
            }
        }
        scroll.addView(text)
        setContentView(scroll)
    }
}