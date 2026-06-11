package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.logic.TriggerEnum
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

class EventTriggerSelectorFragment : Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!

    private var sender: String? = null

    private lateinit var viewModel: IEventTriggerSelectorViewModel
    private val args: EventTriggerSelectorFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        sender = args.sender

        when (sender) {
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

        val eventTriggerAdapter = TriggerViewAdapter(activity)

        tvTitle.text = getString(R.string.select_a_trigger)

        with(recyclerView) {
            adapter = eventTriggerAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin * 2, margin * 2))
        }

        btnSave.setOnClickListener {
            val trigger = eventTriggerAdapter.selected ?: return@setOnClickListener
            viewModel.trigger.postValue(trigger)

            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.trigger.observe(viewLifecycleOwner, { trigger ->
            eventTriggerAdapter.selected = trigger
        })
    }

    companion object {
        fun newInstance(sender: String?) =
            EventTriggerSelectorFragment().apply {
                arguments = Bundle().apply {
                    putString(SENDER, sender)
                }
            }
    }

    class TriggerViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<TriggerViewAdapter.TriggerCardViewHolder>() {

        private var triggers = TriggerEnum.values().filter { it == TriggerEnum.OPEN || it == TriggerEnum.CLOSE }.toList()
        var selected: TriggerEnum? = null
            set(value) {
                val previousIndex = triggers.indexOf(field)
                val newIndex = triggers.indexOf(value)

                field = if (value != field) {
                    notifyItemChanged(newIndex)
                    value
                } else {
                    null
                }

                notifyItemChanged(previousIndex)
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.binding.layout.layout_group_selector_tile, parent, false)
            return TriggerCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: TriggerCardViewHolder, position: Int) {
            val trigger = getItem(position) ?: return

            holder.setTrigger(trigger)
        }

        override fun getItemCount() = triggers.size

        fun getItem(position: Int): TriggerEnum? = triggers.getOrNull(position)

        inner class TriggerCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            private var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById(R.id.ivIcon).visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                getItem(adapterPosition)?.let {
                    selected = it
                }
            }

            fun setTrigger(trigger: TriggerEnum) {
                name.text = trigger.name.toCapitalisedLowerCase()

                if (trigger == selected) {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }
    }
}

interface IEventTriggerSelectorViewModel {
    var trigger: MutableLiveData<TriggerEnum?>


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
