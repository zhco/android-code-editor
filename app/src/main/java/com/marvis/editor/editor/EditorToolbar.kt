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
import com.marvis.editor.build.BuildManager
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {

    private val btnHamburger: Button
    private val btnCopy: Button
    private val btnPaste: Button
    private val btnSelectAll: Button
    private val btnUndo: Button
    private val btnRedo: Button
    private val btnReplace: Button
    private val btnSave: Button
    private val btnBuild: Button
    private var editor: CodeEditor? = null
    private var drawerCallback: (() -> Unit)? = null
    private var currentFile: File? = null

    init {
        isHorizontalScrollBarEnabled = false
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF2a2a2a.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(4, 4, 4, 4)
        }
        fun btn(l: String) = Button(context).apply {
            setPadding(8, 8, 8, 8); minimumWidth = 80; minimumHeight = 80
            text = l; setTextColor(0xFFFFFFFF.toInt()); row.addView(this)
        }
        fun sep() = row.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(2, 60).apply { setMargins(6, 0, 6, 0) }
            setBackgroundColor(0xFF555555.toInt())
        })
        btnHamburger = btn("☰"); sep()
        btnCopy = btn("复制"); btnPaste = btn("粘贴"); btnSelectAll = btn("全选"); sep()
        btnUndo = btn("撤销"); btnRedo = btn("重做"); sep()
        btnReplace = btn("替换"); btnSave = btn("保存"); sep()
        btnBuild = btn("构建")
        btnHamburger.setOnClickListener { drawerCallback?.invoke() }
        addView(row)
    }

    fun setDrawerCallback(c: () -> Unit) { drawerCallback = c }
    fun setCurrentFile(f: File?) { currentFile = f }

    fun setEditor(e: CodeEditor) {
        editor = e
        btnCopy.setOnClickListener { copy() }; btnPaste.setOnClickListener { paste() }
        btnSelectAll.setOnClickListener { selectAll() }; btnUndo.setOnClickListener { e.undo() }
        btnRedo.setOnClickListener { e.redo() }; btnReplace.setOnClickListener { showReplaceDialog() }
        btnSave.setOnClickListener { save() }; btnBuild.setOnClickListener { build() }
    }

    private fun copy() {
        val e = editor ?: return
        val t = e.text?.substring(e.cursor.left, e.cursor.right) ?: return
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("code", t))
    }

    private fun paste() {
        val s = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .primaryClip?.getItemAt(0)?.text?.toString() ?: return
        editor?.commitText(s)
    }

    private fun selectAll() { editor?.selectAll() }

    private fun showReplaceDialog() {
        ReplaceDialog(context) { old, new ->
            editor?.setText(editor?.text?.toString()?.replace(old, new, true) ?: "")
        }.show()
    }

    private fun save() { currentFile?.writeText(editor?.text?.toString() ?: return) }

    private fun build() {
        val f = currentFile ?: return
        var d: File? = f.parentFile
        while (d != null) {
            if (File(d, "gradlew").exists()) {
                BuildManager(context).showOutputDialog(context, d); return
            }
            d = d.parentFile
        }
    }
}