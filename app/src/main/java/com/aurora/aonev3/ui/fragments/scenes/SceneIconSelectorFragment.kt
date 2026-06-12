package com.aurora.aonev3.ui.fragments.scenes

import com.aurora.aonev3.synthetic.*
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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.R
import com.aurora.aonev3.databinding.FragmentSceneIconSelectorBinding
import com.aurora.aonev3.ui.ColourScenes
import com.aurora.aonev3.ui.IconsScenes

class SceneIconSelectorFragment : Fragment() {

    protected var _binding: FragmentSceneIconSelectorBinding? = null
    protected val binding get() = _binding!!

    private val args: SceneIconSelectorFragmentArgs by navArgs()

    private var selectedColour: String = ColourScenes.DEFAULT.stringValue
    private var selectedIcon: IconsScenes = IconsScenes.NULL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSceneIconSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedColour = args.colour ?: ColourScenes.DEFAULT.stringValue
        selectedIcon = args.icon

        val selectorAdapter = SceneStyleAdapter(
            initialColour = selectedColour,
            initialIcon = selectedIcon,
            onColourSelected = { colour ->
                selectedColour = colour
            },
            onIconSelected = { icon ->
                selectedIcon = icon
            }
        )

        binding.rvColours.apply {
            adapter = selectorAdapter
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(requireContext(), 4)
        }

        binding.btnSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("colour", selectedColour)
            findNavController().previousBackStackEntry?.savedStateHandle?.set("icon", selectedIcon)
            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class SceneStyleAdapter(
        initialColour: String,
        initialIcon: IconsScenes,
        private val onColourSelected: (String) -> Unit,
        private val onIconSelected: (IconsScenes) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val colours = ColourScenes.values().toList()
        private val icons = IconsScenes.values().filter { it != IconsScenes.NULL }

        private var selectedColour = normaliseColour(initialColour)
        private var selectedIcon = initialIcon

        override fun getItemCount(): Int = colours.size + icons.size

        override fun getItemViewType(position: Int): Int {
            return if (position < colours.size) VIEW_TYPE_COLOUR else VIEW_TYPE_ICON
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == VIEW_TYPE_COLOUR) {
                val view = inflater.inflate(R.layout.layout_scene_colour, parent, false)
                ColourViewHolder(view)
            } else {
                val view = inflater.inflate(R.layout.layout_scene_icon, parent, false)
                IconViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ColourViewHolder) {
                val colour = colours[position]
                val colourString = normaliseColour(colour.stringValue)

                holder.fill.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(colourString))
                holder.selectedView.visibility =
                    if (colourString.equals(selectedColour, ignoreCase = true)) View.VISIBLE else View.INVISIBLE

                holder.itemView.setOnClickListener {
                    selectedColour = colourString
                    onColourSelected(colourString)
                    notifyDataSetChanged()
                }
            } else if (holder is IconViewHolder) {
                val icon = icons[position - colours.size]

                holder.fill.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(selectedColour))
                holder.icon.setImageDrawable(
                    ContextCompat.getDrawable(holder.itemView.context, icon.resourceValue)
                )
                holder.selectedView.visibility =
                    if (icon == selectedIcon) View.VISIBLE else View.INVISIBLE

                holder.itemView.setOnClickListener {
                    selectedIcon = icon
                    onIconSelected(icon)
                    notifyDataSetChanged()
                }
            }
        }

        private class ColourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fill: ConstraintLayout = itemView.findViewById(R.id.fill)
            val selectedView: ConstraintLayout = itemView.findViewById(R.id.selected)
        }

        private class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fill: ConstraintLayout = itemView.findViewById(R.id.fill)
            val icon: ImageView = itemView.findViewById(R.id.iconIv)
            val selectedView: ConstraintLayout = itemView.findViewById(R.id.selected)
        }

        companion object {
            private const val VIEW_TYPE_COLOUR = 0
            private const val VIEW_TYPE_ICON = 1

            private fun normaliseColour(colour: String): String {
                return if (colour.startsWith("#")) colour else "#$colour"
            }
        }
    }
}
