package com.marvis.editor.filetree

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.marvis.editor.R
import java.io.File

class FileTreeAdapter(
    private val context: Context,
    private val onFileSelected: (File) -> Unit
) : BaseAdapter() {

    private var files: List<File> = emptyList()

    fun setFiles(list: List<File>) {
        files = list
        notifyDataSetChanged()
    }

    override fun getCount() = files.size
    override fun getItem(pos: Int) = files[pos]
    override fun getItemId(pos: Int) = pos.toLong()

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
        val icon = view.findViewById<ImageView>(R.id.file_icon)
        val name = view.findViewById<TextView>(R.id.file_name)
        val file = files[pos]

        name.text = file.name
        icon.setImageResource(
            if (file.isDirectory) R.drawable.ic_folder
            else when {
                file.extension in listOf("kt", "java") -> R.drawable.ic_kotlin
                file.extension in listOf("xml") -> R.drawable.ic_xml
                file.extension in listOf("gradle", "kts") -> R.drawable.ic_gradle
                file.extension in listOf("pro") -> R.drawable.ic_gradle
                else -> R.drawable.ic_file
            }
        )
        return view
    }
}
