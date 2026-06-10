package com.aurora.aonev3

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_section_header.view.*


class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val section: TextView = itemView.sectionTv

    fun setSectionHeader(header: String) {
        section.text = header
    }
}