package com.marvis.editor

import android.os.Bundle
import android.view.View
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
    }
}
