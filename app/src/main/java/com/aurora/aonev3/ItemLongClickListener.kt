package com.aurora.aonev3

import com.aurora.aonev3.synthetic.*
import android.view.View

interface ItemLongClickListener {
    fun onItemLongClick(view: View, position: Int): Boolean
}
