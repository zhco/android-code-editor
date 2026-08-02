package com.marvis.editor.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import com.marvis.editor.R
import com.marvis.editor.build.BuildManager
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {

    private val btnCopy: Button
    private val btnPaste: Button
    private val btnSelectAll: Button
    private val btnUndo: Button
    private val btnRedo: Button
    private val btnReplace: Button
    private val btnSave: Button
    private val btnBuild: Button
    private var editor: CodeEditor? = null

    init {
        isHorizontalScrollBarEnabled = false
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF2a2a2a.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(4, 4, 4, 4)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        fun makeBtn(label: String): Button {
            val btn = Button(context).apply {
                val pad = (4 * context.resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                minimumWidth = (36 * context.resources.displayMetrics.density).toInt()
                minimumHeight = (36 * context.resources.displayMetrics.density).toInt()
                text = label
                setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)
            }
            row.addView(btn)
            return btn
        }

        fun separator() {
            row.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (1 * context.resources.displayMetrics.density).toInt(),
                    (24 * context.resources.displayMetrics.density).toInt()
                ).apply { setMargins(4, 0, 4, 0) }
                setBackgroundColor(0xFF555555.toInt())
            })
        }

        btnCopy = makeBtn("Cp")
        btnPaste = makeBtn("Ps")
        btnSelectAll = makeBtn("SA")
        separator()
        btnUndo = makeBtn("<-")
        btnRedo = makeBtn("->")
        separator()
        btnReplace = makeBtn("Rp")
        btnSave = makeBtn("Sv")
        separator()
        btnBuild = makeBtn("Bld")

        addView(row)
    }

    fun setEditor(editor: CodeEditor) {
        this.editor = editor
        btnCopy.setOnClickListener { copy() }
        btnPaste.setOnClickListener { paste() }
        btnSelectAll.setOnClickListener { selectAll() }
        btnUndo.setOnClickListener { editor.undo() }
        btnRedo.setOnClickListener { editor.redo() }
        btnReplace.setOnClickListener { showReplaceDialog() }
        btnSave.setOnClickListener { save() }
        btnBuild.setOnClickListener { build() }
    }

    fun updateUndoRedo() {
        btnUndo.isEnabled = editor?.canUndo() == true
        btnRedo.isEnabled = editor?.canRedo() == true
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
        BuildManager(context).showOutputDialog(context, projectDir)
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