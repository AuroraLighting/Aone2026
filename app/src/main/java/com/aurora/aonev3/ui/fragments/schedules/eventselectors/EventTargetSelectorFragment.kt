package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import com.aurora.aonev3.synthetic.*
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorEventViewModel
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventViewModel
import com.google.android.material.card.MaterialCardView

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val SENDER = "sender"

class EventTargetSelectorFragment : Fragment() {

    protected var _binding: FragmentSelectorBinding? = null
    protected val binding get() = _binding!!

    private var sender: String? = null

    private lateinit var viewModel: IEventTargetSelectorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        arguments?.let {
            sender = it.getString(SENDER)
        }

        when (sender) {
            ScheduleEventFragment::class.simpleName -> {
                viewModel = ViewModelProvider(activity).get(ScheduleEventViewModel::class.java)
            }
            MotionSensorEventFragment::class.simpleName -> {
                viewModel = ViewModelProvider(activity).get(MotionSensorEventViewModel::class.java)
            }
            DoorSensorEventFragment::class.simpleName -> {
                viewModel = ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
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

        val eventTargetAdapter = EventTargetViewAdapter(
            activity
        ).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val items = getItems().mapIndexed { index, item -> if (index == position) { Pair(item.first, !item.second) } else Pair(item.first, false) }
                    setItems(items)
                }
            }
        }

        binding.tvTitle.text = getString(R.string.select_a_target)

        with(binding.recyclerView) {
            adapter = eventTargetAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin * 2, margin * 2))
        }

        binding.btnSave.setOnClickListener {
            val target = eventTargetAdapter.getSelected()?.first ?: return@setOnClickListener
            viewModel.eventTarget.postValue(target)

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.eventTarget.observe(viewLifecycleOwner, { eventTarget ->
            eventTarget?.let {
                val items = eventTargetAdapter.getItems().map { item ->
                    if (item.first == eventTarget) {
                        Pair(eventTarget, true)
                    } else {
                        item
                    }
                }
                eventTargetAdapter.setItems(items)
            }
        })
    }

    companion object {
        fun newInstance(sender: String?) =
            EventTargetSelectorFragment().apply {
                arguments = Bundle().apply {
                    putString(SENDER, sender)
                }
            }
    }

    class EventTargetViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<EventTargetViewAdapter.EventTargetCardViewHolder>() {

        private var targetList = EventTarget.values().map { Pair(it, false) }.toList()
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventTargetCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_group_selector_tile, parent, false)
            return EventTargetCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: EventTargetCardViewHolder, position: Int) {
            if (position < targetList.size) {
                val target = targetList[position]

                holder.name.text = target.first.displayName

                if (target.second) {
                    holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    holder.name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    holder.name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        override fun getItemCount() = targetList.size

        fun getItem(position: Int): Pair<EventTarget, Boolean>? = if (position in targetList.indices) targetList[position] else null

        fun getItems() = targetList

        fun getSelected(): Pair<EventTarget, Boolean>? = targetList.find { it.second }

        fun setItems(items: List<Pair<EventTarget, Boolean>>) {
            targetList = items
            notifyDataSetChanged()
        }

        inner class EventTargetCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById<android.widget.ImageView>(R.id.ivIcon).visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(cardView, adapterPosition)
            }
        }
    }
}

interface IEventTargetSelectorViewModel {
    var eventTarget: MutableLiveData<EventTarget?>
}
