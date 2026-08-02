package com.marvis.editor

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.marvis.editor.filetree.FileTreeFragment

class MainActivity : AppCompatActivity() {
    private var fileTreeFragment: FileTreeFragment? = null
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerLayout = DrawerLayout(this).apply {
            fitsSystemWindows = true
        }

        // Main content: placeholder
        val mainContent = TextView(this).apply {
            text = "select a file from the drawer"
            gravity = Gravity.CENTER
        }

        // Side drawer: file tree
        val fileTreeContainer = FrameLayout(this).apply {
            id = ViewGroup.generateViewId()
        }

        val drawerParams = DrawerLayout.LayoutParams(900, ViewGroup.LayoutParams.MATCH_PARENT)
        drawerParams.gravity = Gravity.START

        drawerLayout.addView(mainContent, DrawerLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        drawerLayout.addView(fileTreeContainer, drawerParams)

        setContentView(drawerLayout)

        fileTreeFragment = FileTreeFragment { file ->
            // TODO: open in editor
        }

        supportFragmentManager.beginTransaction()
            .replace(fileTreeContainer.id, fileTreeFragment!!)
            .commit()
    }
}