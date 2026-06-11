package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.ColourScenes
import com.aurora.aonev3.ui.IconsScenes
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorEventViewModel
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventFragment
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventViewModel
import com.aurora.aonev3.ui.fragments.groups.eventgroupselector.IEventGroupSelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventViewModel
import com.aurora.aonev3.ui.fragments.schedules.ScheduleViewModel
import com.google.android.material.card.MaterialCardView

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val SENDER = "sender"

class EventSceneSelectorFragment : Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!


    private lateinit var viewModel: IEventSceneSelectorViewModel
    private var mGroup: Group? = null
    private lateinit var mScenes: List<Scene>

    private val args: EventSceneSelectorFragmentArgs by navArgs()
    private val sender: String by lazy { args.sender }
    private val groupArg: Group? by lazy { args.group }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        when (sender) {
            ScheduleEventFragment::class.simpleName -> {
                mGroup = groupArg
                mScenes = SyncHandler
                    .scenesList
                    .filter { it.parentGateway == mGroup?.parentGateway ?: "" && it.groupId == mGroup?.id }
                viewModel =
                    ViewModelProvider(activity).get(ScheduleEventViewModel::class.java)
            }
            DynamicEventFragment::class.simpleName -> {
                mGroup = groupArg
                mScenes = SyncHandler
                    .scenesList
                    .filter { it.parentGateway == mGroup?.parentGateway ?: "" && it.groupId == mGroup?.id }
                viewModel = navGraphViewModels<DynamicEventViewModel>(R.id.dynamicEventFragment).value
            }
            else -> {
                if (sender == DoorSensorEventFragment::class.simpleName) {
                    viewModel =
                        ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
                } else if (sender == MotionSensorEventFragment::class.simpleName) {
                    viewModel =
                        ViewModelProvider(activity).get(MotionSensorEventViewModel::class.java)
                }
                viewModel.targetGroup.value?.let { group ->
                    mGroup = group
                    mScenes = SyncHandler
                        .scenesList
                        .filter { it.parentGateway == mGroup?.parentGateway ?: "" && it.groupId == mGroup?.id }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return
        debug(sender)

        val eventSceneAdapter = EventSceneViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val items = getItems().mapIndexed { index, item ->
                        if (index == position) {
                            Pair(item.first, !item.second)
                        } else Pair(item.first, false)
                    }
                    setItems(items)
                }
            }

            setItems(mScenes.map { Pair(it, false) })
        }

        tvTitle.text = getString(R.string.select_scene)

        with(recyclerView) {
            adapter = eventSceneAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin * 2, margin * 2))
        }

        btnSave.setOnClickListener {
            val scene = eventSceneAdapter.getSelected()?.first ?: return@setOnClickListener
            viewModel.scene.postValue(scene)

            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.scene.observe(viewLifecycleOwner, { scene ->
            scene?.let {
                val items = eventSceneAdapter.getItems().map { item ->
                    if (item.first == scene) {
                        Pair(scene, true)
                    } else {
                        item
                    }
                }
                eventSceneAdapter.setItems(items)
            }
        })
    }

    companion object {
        fun newInstance(sender: String?) =
            EventSceneSelectorFragment().apply {
                arguments = Bundle().apply {
                    putString(SENDER, sender)
                }
            }
    }

    class EventSceneViewAdapter internal constructor(val context: Context) :
        RecyclerView.Adapter<EventSceneViewAdapter.EventSceneCardViewHolder>() {

        private var sceneList = emptyList<Pair<Scene, Boolean>>()
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): EventSceneCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_scene_selector_tile, parent, false)
            return EventSceneCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: EventSceneCardViewHolder, position: Int) {
            if (position < sceneList.size) {
                val scene = sceneList[position]
                val icon = IconsScenes.fromString(scene.first.metadata.optString("icon"))
                var colour = scene.first.metadata.optString("icon_colour")

                if (colour == "") {
                    colour = ColourScenes.DEFAULT.stringValue
                } else if (!colour.startsWith("#")) {
                    colour = "#$colour"
                }

                val colourStateList = try {
                    ColorStateList.valueOf(Color.parseColor(colour))
                } catch (ex: Exception) {
                    ColorStateList.valueOf(Color.parseColor(ColourScenes.DEFAULT.stringValue))
                }

                holder.name.text = scene.first.name
                holder.icon.setImageDrawable(
                    ContextCompat.getDrawable(context, icon.resourceValue)
                )
                holder.binding.iconLayout.backgroundTintList = colourStateList

                if (scene.second) {
                    holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    holder.name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    holder.name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        override fun getItemCount() = sceneList.size

        fun getItem(position: Int): Pair<Scene, Boolean>? =
            if (position in sceneList.indices) sceneList[position] else null

        fun getItems() = sceneList

        fun getSelected(): Pair<Scene, Boolean>? = sceneList.find { it.second }

        fun setItems(items: List<Pair<Scene, Boolean>>) {
            sceneList = items
            notifyDataSetChanged()
        }

        inner class EventSceneCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var binding.cardView: MaterialCardView = itemView.binding.cardView
            var name: TextView = itemView.binding.tvName
            var icon: ImageView = itemView.binding.sceneIconIv
            var binding.iconLayout: ConstraintLayout = itemView.binding.iconLayout

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(binding.cardView, adapterPosition)
            }
        }
    }
}

interface IEventSceneSelectorViewModel: IEventGroupSelectorViewModel {
    var scene: MutableLiveData<Scene?>


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
