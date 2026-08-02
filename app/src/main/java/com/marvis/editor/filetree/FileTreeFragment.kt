package com.marvis.editor.filetree

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File

class FileTreeFragment(private val onFileSelected: (File) -> Unit) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return TextView(requireContext()).apply {
            text = "Fragment OK"
            textSize = 24f
            setPadding(64, 64, 64, 64)
        }
    }
}