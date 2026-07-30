package com.marvis.editor.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.marvis.editor.databinding.FragmentEditorBinding
import java.io.File

class EditorFragment : Fragment() {
    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    private var currentFile: File? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setEditor(binding.editor)
    }

    fun openFile(file: File) {
        currentFile = file
        binding.fileName.text = file.name
        binding.editor.setText(file.readText())
        binding.toolbar.updateUndoRedo()
    }

    fun getCurrentFile(): File? = currentFile

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
