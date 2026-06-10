package com.aurora.aonev3

import android.view.View

interface ItemLongClickListener {
    fun onItemLongClick(view: View, position: Int): Boolean
}