package com.aurora.aonev3.ui.fragments.scenes

//class IconViewAdapter internal constructor(val context: Context) :
//    RecyclerView.Adapter<IconViewAdapter.ColourViewHolder>() {
//
//    private var iconList = emptyList<IconsScenes>()
//    private var colour: String = ColourScenes.DEFAULT.stringValue
//    var selected = emptyList<Boolean>()
//    var onItemClickListener: ItemClickListener? = null
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColourViewHolder {
//        val layoutView =
//            LayoutInflater.from(parent.context).inflate(R.layout.layout_scene_icon, parent, false)
//        return ColourViewHolder(layoutView)
//    }
//
//    override fun onBindViewHolder(holder: ColourViewHolder, position: Int) {
//        if (position < iconList.size) {
//            val icon = iconList[position].resourceValue
//
//            holder.selectedView.visibility = if  (selected[position]) { View.VISIBLE } else { View.INVISIBLE }
//            holder.fill.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colour))
//
//            holder.icon.setImageDrawable(ContextCompat.getDrawable(context, icon))
//        }
//    }
//
//    override fun getItemCount() = iconList.size
//
//    fun getItem(position: Int): IconsScenes? =
//        if (position in iconList.indices) iconList[position] else null
//
//    internal fun setIcons(icons: List<IconsScenes>) {
//        this.iconList = icons.filter { it != IconsScenes.NULL }
//        selected = MutableList(icons.size) { false }
//        notifyDataSetChanged()
//    }
//
//    internal fun setColour(colour: String) {
//        this.colour = colour
//        notifyDataSetChanged()
//    }
//
//    inner class ColourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
//        View.OnClickListener {
//        val fill: ConstraintLayout = itemView.fill
//        val icon: ImageView = itemView.iconIv
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