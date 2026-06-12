package com.aurora.aonev3

import com.aurora.aonev3.synthetic.*
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridItemDecoration(private val topPadding: Int, private val bottomPadding: Int, private val startPadding: Int, private val endPadding: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = topPadding
        outRect.bottom = bottomPadding
        outRect.left = startPadding
        outRect.right = endPadding
    }
}
