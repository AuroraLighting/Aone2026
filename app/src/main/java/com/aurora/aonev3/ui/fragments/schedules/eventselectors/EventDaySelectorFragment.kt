package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import com.aurora.aonev3.synthetic.*
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
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
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventFragment
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventViewModel
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventViewModel
import com.google.android.material.card.MaterialCardView

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val SENDER = "sender"

class EventDaySelectorFragment : Fragment() {

    protected var _binding: FragmentSelectorBinding? = null
    protected val binding get() = _binding!!

    private var sender: String? = null

    private lateinit var viewModel: IEventDaySelectorViewModel

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
            DynamicEventFragment::class.simpleName -> {
                viewModel = navGraphViewModels<DynamicEventViewModel>(R.id.dynamicEventFragment).value
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

        val eventDayAdapter = EventDayViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val items = getItems().mapIndexed { index, item -> if (index == position) { Pair(item.first, !item.second) } else Pair(item.first, false) }
                    setItems(items)
                }
            }
        }

        binding.tvTitle.text = getString(R.string.set_day_for_event)

        with(binding.recyclerView) {
            adapter = eventDayAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        binding.btnSave.setOnClickListener {
            val day = eventDayAdapter.getSelected()?.first ?: return@setOnClickListener
            viewModel.eventDay.postValue(day)

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.eventDay.observe(viewLifecycleOwner, { eventDay ->
            eventDay?.let {
                val items = eventDayAdapter.getItems().map { item -> if (item.first == eventDay) { Pair(eventDay, true) } else { item } }
                eventDayAdapter.setItems(items)
            }
        })
    }

    companion object {
        fun newInstance(sender: String?) =
            EventDaySelectorFragment().apply {
                arguments = Bundle().apply {
                    putString(SENDER, sender)
                }
            }
    }

    class EventDayViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<EventDayViewAdapter.EventDayCardViewHolder>() {

        private var dayList = EventDay.values().map { Pair(it, false) }.toList()
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventDayCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_group_selector_tile, parent, false)
            return EventDayCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: EventDayCardViewHolder, position: Int) {
            if (position < dayList.size) {
                val day = dayList[position]

                holder.name.text = day.first.displayName

                if (day.second) {
                    holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    holder.name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    holder.name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        override fun getItemCount() = dayList.size

        fun getItem(position: Int): Pair<EventDay, Boolean>? = if (position in dayList.indices) dayList[position] else null

        fun getItems() = dayList

        fun getSelected(): Pair<EventDay, Boolean>? = dayList.find { it.second }

        fun setItems(items: List<Pair<EventDay, Boolean>>) {
            dayList = items
            notifyDataSetChanged()
        }

        inner class EventDayCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById<android.view.View>(R.id.ivIcon).visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(cardView, adapterPosition)
            }
        }
    }
}

interface IEventDaySelectorViewModel {
    var eventDay: MutableLiveData<EventDay>

}
