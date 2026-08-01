package com.marvis.editor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.marvis.editor.filetree.FileTreeFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "Step 1: onCreate", Toast.LENGTH_SHORT).show()

        val fragment = FileTreeFragment { file ->
            Toast.makeText(this, "File selected: ${file.name}", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "Step 2: Fragment created", Toast.LENGTH_SHORT).show()

        setContentView(android.widget.FrameLayout(this).apply {
            id = android.R.id.content
        })

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit()

        Toast.makeText(this, "Step 3: Fragment added", Toast.LENGTH_SHORT).show()
    }
}