package com.marvis.editor.editor

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorFragment : Fragment() {

    private var currentFile: File? = null
    private lateinit var fileName: TextView
    private lateinit var editor: CodeEditor

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL

            fileName = TextView(ctx).apply {
                setPadding(8, 8, 8, 8)
                textSize = 12f
                text = "Editor"
            }
            addView(fileName, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            // Toolbar: instantiate only, don't add to layout
            try {
                val toolbar = EditorToolbar(ctx)
                toolbar.setEditor(editor)
                addView(toolbar, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                fileName.text = "Toolbar ok"
            } catch (e: Exception) {
                fileName.text = "Toolbar ERR: " + e.message
            }

            editor = CodeEditor(ctx)
            addView(editor, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    fun openFile(file: File) {
        currentFile = file
        fileName.text = file.name
        editor.setText(file.readText())
    }

    fun getCurrentFile(): File? = currentFile
}