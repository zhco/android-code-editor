package com.marvis.editor.editor

import android.app.Dialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton

class ReplaceDialog(context: Context, private val onReplace: (String, String) -> Unit) : Dialog(context) {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(context.getString(com.marvis.editor.R.string.replace))
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val oldInput = EditText(context).apply { hint = context.getString(com.marvis.editor.R.string.find); setSingleLine() }
        val newInput = EditText(context).apply { hint = context.getString(com.marvis.editor.R.string.replace_with); setSingleLine() }
        val btn = MaterialButton(context).apply {
            text = context.getString(com.marvis.editor.R.string.replace_all)
            setOnClickListener { onReplace(oldInput.text.toString(), newInput.text.toString()); dismiss() }
        }
        layout.addView(oldInput)
        layout.addView(newInput)
        layout.addView(btn)
        setContentView(layout)
    }
}
