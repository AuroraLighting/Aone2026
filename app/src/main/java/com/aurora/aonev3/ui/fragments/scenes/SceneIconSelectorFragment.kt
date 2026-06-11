package com.aurora.aonev3.ui.fragments.scenes

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSceneIconSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.ui.ColourScenes
import com.aurora.aonev3.ui.IconsScenes

class SceneIconSelectorFragment : Fragment() {

    private var _binding: FragmentSceneIconSelectorBinding? = null
    private val binding get() = _binding!!


    private val viewModel: NewSceneViewModel by viewModels()
    private val args: SceneIconSelectorFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentSceneIconSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val iconsAdapter = IconViewAdapter()

        with(rvColours) {
            adapter = iconsAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 4, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = SceneIconSpanSizeLookup(iconsAdapter, 4)
            }

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        binding.btnSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("colour", viewModel.selectedColour)
            findNavController().previousBackStackEntry?.savedStateHandle?.set("icon", viewModel.selectedIcon)
            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.selectedColour = args.colour
        viewModel.selectedIcon = args.icon
    }

    companion object {
        fun newInstance() =
            SceneIconSelectorFragment()
    }

    private class SceneIconSpanSizeLookup(
        private val adapter: IconViewAdapter,
        private val spanCount: Int
    ) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (adapter.getItemViewType(position) == SceneIconDataType.SECTION.ordinal) {
                spanCount / 1
            } else {
                spanCount / 4
            }
        }
    }

    data class SceneIconData(val icon: IconsScenes? = null, val colour: String? = null, val section: String? = null, val type: SceneIconDataType)

    enum class SceneIconDataType {
        ICON,
        COLOUR,
        SECTION
    }

    inner class IconViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var iconList = ArrayList<SceneIconData>().apply {
            add(SceneIconData(section = "Select a colour:", type = SceneIconDataType.SECTION))

            enumValues<ColourScenes>().map {  it.stringValue }.forEach {
                add(SceneIconData(colour = it, type = SceneIconDataType.COLOUR))
            }

            add(SceneIconData(section = "Select an icon:", type = SceneIconDataType.SECTION))

            enumValues<IconsScenes>().forEach {
                add(SceneIconData(icon = it, type = SceneIconDataType.ICON))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                SceneIconDataType.ICON.ordinal -> {
                    val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_scene_icon, parent, false)
                    IconViewHolder(layoutView)
                }
                SceneIconDataType.COLOUR.ordinal -> {
                    val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_scene_colour, parent, false)
                    ColourViewHolder(layoutView)
                }
                else -> {
                    val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_section_header, parent, false)
                    SectionHeaderViewHolder(layoutView)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position < iconList.size) {
                val item = iconList[position]

                when (item.type) {
                    SceneIconDataType.ICON -> {
                        val icon = item.icon ?: return

                        (holder as? IconViewHolder)?.setIcon(icon)
                    }
                    SceneIconDataType.COLOUR -> {
                        val colour = item.colour ?: return

                        (holder as? ColourViewHolder)?.setColour(colour)
                    }
                    SceneIconDataType.SECTION -> {
                        val section = item.section ?: return

                        (holder as? SectionHeaderViewHolder)?.setSectionHeader(section)
                    }
                }
            }
        }

        override fun getItemCount() = iconList.size

        fun getItem(position: Int): SceneIconData? = if (position in iconList.indices) iconList[position] else null

        override fun getItemViewType(position: Int): Int {
            return if (position in iconList.indices) {
                iconList[position].type.ordinal
            } else {
                SceneDeviceDataType.SECTION.ordinal
            }
        }

        inner class ColourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val binding.fill: ConstraintLayout = itemView.findViewById(R.id.binding).fill
            private val selectedView: ConstraintLayout = itemView.findViewById(R.id.binding).selected

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                val colour = iconList[adapterPosition].colour
                val previousColour = viewModel.selectedColour
                if (colour != previousColour) {
                    viewModel.selectedColour = colour
                    notifyItemChanged(iconList.indexOfFirst { it.colour == previousColour })
                } else {
                    viewModel.selectedColour = null
                }
                notifyItemRangeChanged(
                    iconList.indexOfFirst { it.type == SceneIconDataType.ICON },
                    iconList.count { it.type == SceneIconDataType.ICON}
                )
                notifyItemChanged(adapterPosition)
            }

            fun setColour(colour: String) {
                binding.fill.backgroundTintList = ColorStateList.valueOf( Color.parseColor(colour))
                selectedView.visibility = if (colour == viewModel.selectedColour) { View.VISIBLE } else { View.INVISIBLE }
            }
        }

        inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val binding.fill: ConstraintLayout = itemView.findViewById(R.id.binding).fill
            private val ivIcon: ImageView = itemView.findViewById(R.id.binding).iconIv
            private val selectedView: ConstraintLayout = itemView.findViewById(R.id.binding).selected

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                val icon = iconList[adapterPosition].icon ?: IconsScenes.NULL
                val previousIcon = viewModel.selectedIcon
                if (icon != previousIcon) {
                    viewModel.selectedIcon = icon
                    notifyItemChanged(iconList.indexOfFirst { it.icon == previousIcon })
                } else {
                    viewModel.selectedIcon = IconsScenes.NULL
                }
                notifyItemChanged(adapterPosition)
            }

            fun setIcon(icon: IconsScenes) {
                val colour = if (!viewModel.selectedColour.isNullOrBlank()) viewModel.selectedColour else ColourScenes.DEFAULT.stringValue
                binding.fill.backgroundTintList = ColorStateList.valueOf( Color.parseColor(colour))
                selectedView.visibility = if (icon == viewModel.selectedIcon) { View.VISIBLE } else { View.INVISIBLE }

                ivIcon.setImageDrawable(
                    if (icon != IconsScenes.NULL) {
                        ContextCompat.getDrawable(requireContext(), icon.resourceValue)
                    } else {
                        null
                    })
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
