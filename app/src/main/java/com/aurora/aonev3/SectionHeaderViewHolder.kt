package com.aurora.aonev3

import com.aurora.aonev3.synthetic.*
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val section: TextView = itemView.findViewById(R.id.sectionTv)

    fun setSectionHeader(header: String) {
        section.text = header
    }
}
