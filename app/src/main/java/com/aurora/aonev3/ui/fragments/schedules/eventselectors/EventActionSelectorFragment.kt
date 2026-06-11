package com.aurora.aonev3.ui.fragments.schedules.eventselectors

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel
import com.aurora.aonev3.ui.fragments.schedules.EventAction
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventViewModel
import com.google.android.material.card.MaterialCardView

class EventActionSelectorFragment : Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!


    private lateinit var viewModel: IEventActionSelectorViewModel
    private val args: EventActionSelectorFragmentArgs by navArgs()
    private val isLock: Boolean by lazy { args.isLock }
    private val sender: String by lazy { args.sender }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        viewModel = if (sender == ScheduleEventFragment::class.simpleName) {
            ViewModelProvider(activity).get(ScheduleEventViewModel::class.java)
        } else {
            ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
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

        val eventActionAdapter = EventActionViewAdapter(
            activity
        ).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val items = getItems().mapIndexed { index, item -> if (index == position) { Pair(item.first, !item.second) } else Pair(item.first, false) }
                    setItems(items)
                }
            }
        }

        tvTitle.text = getString(R.string.set_event)

        with(recyclerView) {
            adapter = eventActionAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        btnSave.setOnClickListener {
            val action = eventActionAdapter.getSelected()?.first ?: return@setOnClickListener
            viewModel.eventAction.postValue(action)

            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.eventAction.observe(viewLifecycleOwner, { eventAction ->
            eventAction?.let {
                val items = eventActionAdapter.getItems().map { item -> if (item.first == eventAction) { Pair(eventAction, true) } else { item } }
                eventActionAdapter.setItems(items)
            }
        })
    }

    companion object {
        fun newInstance() =
            EventActionSelectorFragment()
    }

    inner class EventActionViewAdapter(val context: Context) : RecyclerView.Adapter<EventActionViewAdapter.EventActionCardViewHolder>() {

        private var actionList = if (!isLock) {
            arrayOf(EventAction.ON, EventAction.OFF).map { Pair(it, false) }.toList()
        } else {
            arrayOf(EventAction.LOCK, EventAction.UNLOCK).map { Pair(it, false) }.toList()
        }
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventActionCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_group_selector_tile, parent, false)
            return EventActionCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: EventActionCardViewHolder, position: Int) {
            if (position < actionList.size) {
                val action = actionList[position]

                holder.name.text = action.first.displayName

                if (action.second) {
                    holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    holder.name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    holder.binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    holder.name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        override fun getItemCount() = actionList.size

        fun getItem(position: Int): Pair<EventAction, Boolean>? = if (position in actionList.indices) actionList[position] else null

        fun getItems() = actionList

        fun getSelected(): Pair<EventAction, Boolean>? = actionList.find { it.second }

        fun setItems(items: List<Pair<EventAction, Boolean>>) {
            actionList = items
            notifyDataSetChanged()
        }

        inner class EventActionCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var binding.cardView: MaterialCardView = itemView.binding.cardView
            var name: TextView = itemView.binding.tvName

            init {
                itemView.setOnClickListener(this)
                itemView.binding.ivIcon.visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(binding.cardView, adapterPosition)
            }
        }
    }
}

interface IEventActionSelectorViewModel {
    var eventAction: MutableLiveData<EventAction?>


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
