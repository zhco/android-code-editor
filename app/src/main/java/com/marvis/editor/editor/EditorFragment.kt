package com.marvis.editor.editor

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File

class EditorFragment : Fragment() {

    private var currentFile: File? = null
    private lateinit var fileName: TextView
    private lateinit var contentView: TextView

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

            contentView = TextView(ctx).apply {
                setPadding(16, 16, 16, 16)
                textSize = 14f
                text = "Editor ready"
            }
            addView(contentView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    fun openFile(file: File) {
        currentFile = file
        fileName.text = file.name
        contentView.text = file.readText()
    }

    fun getCurrentFile(): File? = currentFile
}