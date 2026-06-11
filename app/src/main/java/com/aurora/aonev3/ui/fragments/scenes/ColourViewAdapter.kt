package com.aurora.aonev3.ui.fragments.scenes

//class ColourViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<ColourViewAdapter.ColourViewHolder>() {
//
//    private var colourList = emptyList<String>()
//    var selected = emptyList<Boolean>()
//    var onItemClickListener: ItemClickListener? = null
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColourViewHolder {
//        val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_scene_colour, parent, false)
//        return ColourViewHolder(layoutView)
//    }
//
//    override fun onBindViewHolder(holder: ColourViewHolder, position: Int) {
//        if (position < colourList.size) {
//            val colour = colourList[position]
//
//            holder.selectedView.visibility = if  (selected[position]) { View.VISIBLE } else { View.INVISIBLE }
//            holder.fill.backgroundTintList = ColorStateList.valueOf( Color.parseColor(colour))
//        }
//    }
//
//    override fun getItemCount() = colourList.size
//
//    fun getItem(position: Int): String? = if (position in colourList.indices) colourList[position] else null
//
//    internal fun setColours(colours: List<String>) {
//        this.colourList = colours
//        selected = MutableList(colours.size) { false }
//        notifyDataSetChanged()
//    }
//
//    inner class ColourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
//        val fill: ConstraintLayout = itemView.fill
//        val selectedView: ConstraintLayout = itemView.selected
//
//        init {
//            itemView.setOnClickListener(this)
//        }
//
//        override fun onClick(p0: View?) {
//            onItemClickListener?.onItemClick(itemView, adapterPosition)
//        }
//    }
//}