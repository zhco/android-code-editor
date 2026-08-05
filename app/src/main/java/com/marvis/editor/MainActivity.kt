package com.marvis.editor

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.marvis.editor.editor.EditorFragment
import com.marvis.editor.filetree.FileTreeFragment

class MainActivity : AppCompatActivity() {
    private var fileTreeFragment: FileTreeFragment? = null
    private var editorFragment: EditorFragment? = null
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerLayout = DrawerLayout(this).apply {
            fitsSystemWindows = true
        }

        val mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val editorContainer = FrameLayout(this).apply {
            id = ViewGroup.generateViewId()
        }
        mainContent.addView(editorContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

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
            drawerLayout.closeDrawers()
            editorFragment?.openFile(file)
        }
        editorFragment = EditorFragment()

        supportFragmentManager.beginTransaction()
            .replace(fileTreeContainer.id, fileTreeFragment!!)
            .replace(editorContainer.id, editorFragment!!)
            .commit()

        editorContainer.post {
            editorFragment?.setDrawerCallback {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}