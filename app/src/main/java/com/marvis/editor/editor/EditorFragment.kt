package com.marvis.editor.editor

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File

class EditorFragment : Fragment() {

    private var currentFile: File? = null
    private lateinit var editor: io.github.rosemoe.sora.widget.CodeEditor
    private lateinit var fileName: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL

            fileName = TextView(ctx).apply {
                setPadding(8, 8, 8, 8)
                textSize = 12f
            }
            addView(fileName, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            val toolbar = EditorToolbar(ctx)
            addView(toolbar, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            editor = io.github.rosemoe.sora.widget.CodeEditor(ctx)
            addView(editor, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

            toolbar.setEditor(editor)
        }
    }

    fun openFile(file: File) {
        currentFile = file
        fileName.text = file.name
        editor.setText(file.readText())
    }

    fun getCurrentFile(): File? = currentFile
}