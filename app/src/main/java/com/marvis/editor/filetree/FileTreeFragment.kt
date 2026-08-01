package com.marvis.editor.filetree

import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.marvis.editor.R
import com.marvis.editor.databinding.FragmentFileTreeBinding
import java.io.File

class FileTreeFragment(private val onFileSelected: (File) -> Unit) : Fragment() {

    private var _binding: FragmentFileTreeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FileTreeAdapter
    private lateinit var currentDir: File

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentDir = context.getExternalFilesDir(null) ?: Environment.getExternalStorageDirectory()
        adapter = FileTreeAdapter(requireContext(), onFileSelected)

        binding.fileList.adapter = adapter
        binding.fileList.setOnItemClickListener { _, _, pos, _ ->
            val file = adapter.getItem(pos)
            if (file.isDirectory) {
                currentDir = file
                refreshList()
            } else {
                onFileSelected(file)
            }
        }
        binding.fileList.setOnItemLongClickListener { _, _, pos, _ ->
            showFileOptions(adapter.getItem(pos))
            true
        }
        binding.btnNewFile.setOnClickListener { showNewFileDialog(false) }
        binding.btnNewFolder.setOnClickListener { showNewFileDialog(true) }
        binding.btnImport.setOnClickListener { showImportDialog() }
        binding.btnUp.setOnClickListener {
            currentDir.parentFile?.let {
                currentDir = it
                refreshList()
            }
        }

        refreshList()
    }

    private fun refreshList() {
        binding.currentPath.text = currentDir.absolutePath
        val rawFiles = currentDir.listFiles() ?: emptyArray()
        val files = rawFiles.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
        adapter.setFiles(files)
    }

    private fun showNewFileDialog(isFolder: Boolean) {
        val input = EditText(requireContext()).apply { hint = if (isFolder) "folder name" else "file name" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isFolder) R.string.new_folder else R.string.new_file)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val f = File(currentDir, name)
                    if (isFolder) f.mkdirs() else f.createNewFile()
                    refreshList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showImportDialog() {
        // List directories under /sdcard for import
        val importRoot = Environment.getExternalStorageDirectory()
        val importDirs = importRoot.listFiles { f: File -> f.isDirectory && !f.name.startsWith(".") } ?: emptyArray()
        val dirs = importDirs.sortedBy { it.name }
        val names = dirs.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_project)
            .setItems(names) { _, i -> copyProject(dirs[i]) }
            .show()
    }

    private fun copyProject(src: File) {
        val dest = File(currentDir, src.name)
        src.copyRecursively(dest, true)
        refreshList()
        Toast.makeText(requireContext(), getString(R.string.imported, src.name), Toast.LENGTH_SHORT).show()
    }

    private fun showFileOptions(file: File) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.name)
            .setItems(arrayOf(getString(R.string.rename), getString(R.string.delete))) { _, i ->
                when (i) {
                    0 -> showRenameDialog(file)
                    1 -> {
                        file.deleteRecursively()
                        refreshList()
                    }
                }
            }
            .show()
    }

    private fun showRenameDialog(file: File) {
        val input = EditText(requireContext()).apply { setText(file.name) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != file.name) {
                    file.renameTo(File(file.parent, newName))
                    refreshList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
