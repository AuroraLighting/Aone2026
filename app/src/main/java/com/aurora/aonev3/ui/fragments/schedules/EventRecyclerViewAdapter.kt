package com.aurora.aonev3.ui.fragments.schedules

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.logic.*
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs

class EventRecyclerViewAdapter internal constructor(val context: Context, val logicCollection: LogicCollection) : RecyclerView.Adapter<EventRecyclerViewAdapter.EventCardViewHolder>() {

    private var logicRulesList = emptyList<LogicRule>()
    var onItemClickListener: ItemClickListener? = null
    var onItemLongClickListener: ItemLongClickListener? = null
    var onMenuClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventCardViewHolder {

        return if (logicCollection.metadata.collectionType == CollectionType.SCHEDULE) {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_schedule_event_tile, parent, false)
            ScheduleEventCardViewHolder(layoutView)
        } else {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_schedule_event_tile, parent, false)
            DynamicEventCardViewHolder(layoutView)
        }
    }

    override fun onBindViewHolder(holder: EventCardViewHolder, position: Int) {
        val logicRule = getItem(position) ?: return

        holder.setEvent(logicRule)
    }

    override fun getItemCount() = logicRulesList.size

    fun getItem(position: Int): LogicRule? = if (position in logicRulesList.indices) logicRulesList[position] else null

    internal fun setLogicRules(logicRules: List<LogicRule>) {
        this.logicRulesList = logicRules
        notifyDataSetChanged()
    }

    abstract class EventCardViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        abstract fun setEvent(rule: LogicRule)
    }

    inner class ScheduleEventCardViewHolder(itemView: View) : EventCardViewHolder(itemView) {
        var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        var name: TextView = itemView.tvName
        var days: TextView = itemView.tvDays
        var menu: ImageView = itemView.findViewById(R.id.menu)

        init {
            if (allowEditing()) {
                binding.menu.visibility = View.VISIBLE
                binding.menu.setOnClickListener {
                    onMenuClickListener?.onItemClick(it, bindingAdapterPosition)
                }
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            } else {
                binding.menu.visibility = View.GONE
            }
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(binding.cardView, bindingAdapterPosition) ?: false
        }

        override fun setEvent(rule: LogicRule) {
            val triggers = rule.triggers ?: emptyArray()
            val conditions = rule.conditions ?: emptyArray()
            val event = rule.metadata.event ?: EventMetadata()
            var triggerTime: Pair<Int, Int> = Pair(0, 0)
            var day: EventDay? = null
            val action = event.action
            val spaceId = logicCollection.metadata.parentSpace
            val group = SyncHandler
                .groupsList
                .find { it.parentGateway == rule.parentGateway && it.id == spaceId } ?: return
            var title = ""

            for (trigger in triggers) {
                if (trigger is TimeOfDayTrigger) {
                    val (hour, minute) = trigger
                    if (hour is Number && minute is Number) {
                        triggerTime = Pair(hour.toInt(), minute.toInt())
                    }
                }
            }

            for (condition in conditions) {
                if (condition !is DayOfWeekCondition) continue
                val days = condition.days

                day = when {
                    days.count() == 7 -> EventDay.EVERYDAY
                    days.count() == 5 -> EventDay.WEEKDAYS
                    days.count() == 2 -> EventDay.WEEKEND
                    days.count() == 1 -> {
                        try {
                            EventDay.valueOf(days[0].name)
                        } catch (ex: IllegalArgumentException) {
                            null
                        }
                    }
                    else -> null
                }
                break
            }

            val offset = rule.metadata.event?.triggerOffset ?: 0
            val absOffset = abs(offset)

            val timeString = when (rule.metadata.event?.trigger) {
                TriggerEnum.SUNRISE -> {
                    when {
                        offset > 0 -> "${context.getString(R.string.sunrise)} + $offset"
                        offset < 0 -> "${context.getString(R.string.sunrise)} - $absOffset"
                        else -> context.getString(R.string.sunrise)
                    }
                }
                TriggerEnum.SUNSET -> {
                    when {
                        offset > 0 -> "${context.getString(R.string.sunset)} + $offset"
                        offset < 0 -> "${context.getString(R.string.sunset)} - $absOffset"
                        else -> context.getString(R.string.sunset)
                    }
                }
                else -> context.getString(R.string.event_time, triggerTime.first, triggerTime.second)
            }

            when {
                event.device != null -> {
                    val deviceId = event.device?.id ?: return
                    val ldev = event.device?.ldev
                    val device = SyncHandler
                        .devicesList
                        .find { it.parentGateway == rule.parentGateway && it.id == deviceId } ?: return
                    title = if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) {
                        if (action == RuleMetadataType.ON) {
                            String.format(context.getString(R.string.event_turn_device_on), timeString, device.name)
                        } else {
                            String.format(context.getString(R.string.event_turn_device_off), timeString, device.name)
                        }
                    } else {
                        val name = when (ldev) {
                            "socket1" -> {
                                "${device.name} - left socket"
                            }
                            "socket2" -> {
                                "${device.name} - right socket"
                            }
                            else -> {
                                device.name
                            }
                        }
                        when (action) {
                            RuleMetadataType.OFF -> {
                                String.format(context.getString(R.string.event_turn_device_off), timeString, name)
                            }
                            RuleMetadataType.LOCK -> {
                                String.format(context.getString(R.string.event_turn_device_lock), timeString, name)
                            }
                            RuleMetadataType.UNLOCK -> {
                                String.format(context.getString(R.string.event_turn_device_unlock), timeString, name)
                            }
                            else -> {
                                String.format(context.getString(R.string.event_turn_device_on), timeString, name)
                            }
                        }
                    }
                }
                event.scene != null -> {
                    val sceneId = event.scene?.id ?: return
                    val scene = SyncHandler
                        .scenesList
                        .find { it.parentGateway == rule.parentGateway && it.groupId == group.id && it.id == sceneId } ?: return
                    title = String.format(context.getString(R.string.event_activate_scene), timeString, scene.name)
                }
                event.group != null -> {
                    title = if (action == RuleMetadataType.ON) {
                        String.format(context.getString(R.string.event_turn_space_on), timeString)
                    } else {
                        String.format(context.getString(R.string.event_turn_space_off), timeString)
                    }
                }
            }

            if (rule.isEnabled) {
                binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                name.setTextColor(context.getColor(R.color.colorPrimary))
                days.setTextColor(context.getColor(R.color.colorPrimary))
            } else {
                binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                name.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                days.setTextColor(context.getColor(R.color.colorPrimaryBackground))
            }

            name.text = title
            days.text = day?.displayName ?: ""
        }
    }

    inner class DynamicEventCardViewHolder(itemView: View) : EventCardViewHolder(itemView) {
        var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        var name: TextView = itemView.tvName
        var days: TextView = itemView.tvDays
        var menu: ImageView = itemView.findViewById(R.id.menu)

        init {
            if (allowEditing()) {
                binding.menu.visibility = View.VISIBLE
                binding.menu.setOnClickListener {
                    onMenuClickListener?.onItemClick(it, bindingAdapterPosition)
                }
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
            } else {
                binding.menu.visibility = View.GONE
            }
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(binding.cardView, bindingAdapterPosition) ?: false
        }

        override fun setEvent(rule: LogicRule) {

            val event = rule.metadata.event ?: EventMetadata()

            val timeString = if (event.time != null) {
                val time = event.time ?: TimeEventMetadata()
                val startTimeString = if (time.startHour != null) {
                    String.format(
                        "%02d:%02d",
                        time.startHour,
                        time.startMinute
                    )
                } else {
                    val offset = time.startOffset ?: 0
                    when {
                        offset > 0 -> {
                            "${time.start?.toCapitalisedLowerCase()} (+ $offset)"
                        }
                        offset < 0 -> {
                            "${time.start?.toCapitalisedLowerCase()} (- ${abs(offset)})"
                        }
                        else -> {
                            time.start?.toCapitalisedLowerCase()
                        }
                    }
                }
                val endTimeString = if (time.endHour != null) {
                    String.format(
                        "%02d:%02d",
                        time.endHour,
                        time.endMinute
                    )
                } else {
                    val offset = time.endOffset ?: 0
                    when {
                        offset > 0 -> {
                            "${time.end?.toCapitalisedLowerCase()} (+ $offset)"
                        }
                        offset < 0 -> {
                            "${time.end?.toCapitalisedLowerCase()} (- ${abs(offset)})"
                        }
                        else -> {
                            time.end?.toCapitalisedLowerCase()
                        }
                    }
                }

                arrayOf(startTimeString, endTimeString).joinToString(" - ")
            } else {
                context.getString(R.string.all_day)
            }
            val actionString = when {
                event.group != null -> {
                    val groupId = event.group?.id ?: -1
                    val group = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == rule.parentGateway
                                    && it.id == groupId
                        }
                    String.format(context.getString(R.string.event_turn_on), group?.name ?: "")
                }
                event.device != null -> {
                    val deviceId = event.device?.id ?: -1
                    val device = SyncHandler
                        .devicesList
                        .find { it.parentGateway == rule.parentGateway && it.id == deviceId }
                    String.format(context.getString(R.string.event_turn_on), device?.name ?: "")
                }
                event.scene != null -> {
                    val sceneId = event.scene?.id ?: -1
                    val groupId = event.scene?.group ?: -1
                    val scene = SyncHandler
                        .scenesList
                        .find { it.parentGateway == rule.parentGateway && it.id == sceneId && it.groupId == groupId }
                    String.format(
                        context.getString(R.string.motion_event_activate_scene),
                        scene?.name ?: ""
                    )
                }
                else -> {
                    ""
                }
            }
            val nameString = "$timeString | $actionString"
            val dayString = try {
                EventDay.valueOf(event.days?.uppercase() ?: "").displayName
            } catch (ex: IllegalArgumentException) {
                ""
            }

            name.text = nameString
            days.text = dayString
            if (rule.isEnabled) {
                binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                name.setTextColor(context.getColor(R.color.colorPrimary))
                days.setTextColor(context.getColor(R.color.colorPrimary))
            } else {
                binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                name.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                days.setTextColor(context.getColor(R.color.colorPrimaryBackground))
            }
        }
    }
}