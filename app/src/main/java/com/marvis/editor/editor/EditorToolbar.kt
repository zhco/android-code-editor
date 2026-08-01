package com.marvis.editor.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import com.marvis.editor.R
import com.marvis.editor.build.BuildManager
import com.marvis.editor.databinding.ViewEditorToolbarBinding
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val binding = ViewEditorToolbarBinding.inflate(LayoutInflater.from(context), this)
    private var editor: CodeEditor? = null

    fun setEditor(editor: CodeEditor) {
        this.editor = editor
        binding.btnCopy.setOnClickListener { copy() }
        binding.btnPaste.setOnClickListener { paste() }
        binding.btnSelectAll.setOnClickListener { selectAll() }
        binding.btnUndo.setOnClickListener { editor.undo() }
        binding.btnRedo.setOnClickListener { editor.redo() }
        binding.btnReplace.setOnClickListener { showReplaceDialog() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnBuild.setOnClickListener { build() }
    }

    fun updateUndoRedo() {
        binding.btnUndo.isEnabled = editor?.canUndo() == true
        binding.btnRedo.isEnabled = editor?.canRedo() == true
    }

    private fun copy() {
        val text = editor?.text?.substring(editor!!.cursor.left, editor!!.cursor.right) ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("code", text))
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun paste() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        editor?.commitText(clip)
    }

    private fun selectAll() {
        editor?.selectAll()
    }

    private fun showReplaceDialog() {
        val dialog = ReplaceDialog(context) { old, new ->
            val text = editor?.text?.toString() ?: return@ReplaceDialog
            editor?.setText(text.replace(old, new, true))
        }
        dialog.show()
    }

    private fun save() {
        val file = (context as? androidx.fragment.app.FragmentActivity)
            ?.supportFragmentManager
            ?.findFragmentById(R.id.editor_container) as? EditorFragment
        val f = file?.getCurrentFile() ?: return
        f.writeText(editor?.text?.toString() ?: return)
        Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun build() {
        val file = (context as? androidx.fragment.app.FragmentActivity)
            ?.supportFragmentManager
            ?.findFragmentById(R.id.editor_container) as? EditorFragment
        val f = file?.getCurrentFile() ?: run {
            Toast.makeText(context, "No file open", Toast.LENGTH_SHORT).show()
            return
        }
        val projectDir = findProjectRoot(f) ?: run {
            Toast.makeText(context, "gradlew not found in parent directories", Toast.LENGTH_SHORT).show()
            return
        }
        BuildManager.showOutputDialog(context, projectDir)
    }

    private fun findProjectRoot(file: File): File? {
        var dir: File? = file.parentFile
        while (dir != null) {
            if (File(dir, "gradlew").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }
}