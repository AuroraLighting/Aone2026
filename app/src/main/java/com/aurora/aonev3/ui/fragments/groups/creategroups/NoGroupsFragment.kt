package com.aurora.aonev3.ui.fragments.groups.creategroups

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentNoGroupsBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class NoGroupsFragment : Fragment() {

    private var _binding: FragmentNoGroupsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentNoGroupsBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return

        val listAdapter = DefaultGroupsViewAdapter().apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val group = getItem(position) ?: return
                    if (selectedGroups.contains(group)) {
                        selectedGroups.remove(group)
                    } else {
                        selectedGroups.add(group)
                    }

                    notifyDataSetChanged()
                }
            }
        }
        val existingGroupNames = SyncHandler
            .groupsList
            .filter { it.parentGateway == gateway.serial }
            .map { it.name.toLowerCase() }
        listAdapter.setGroups(DefaultGroups.values().toList().filterNot { it.name.toLowerCase() in existingGroupNames })

        with(recyclerView) {
            adapter = listAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack(R.id.groupsFragment, false)
        }

        btnDone.setOnClickListener {
            activity?.layoutGreyOut?.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                listAdapter.selectedGroups.forEach { group ->
                    try {
                        DevelcoHandler.postGroups(
                            gateway, JSONObject()
                                .put("name", group.displayName)
                                .put("grpType", "generic")
                        )
                    } catch (err: VolleyError) {
                        App.actionFailed()
                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                            gateway.isConnected = false
                            val credentials = CloudHandler.getCredentials()
                            if (credentials.first.isEmpty()) {
                                activity?.finishAffinity()
                                startActivity(Intent(context, SplashscreenActivity::class.java))
                            }
                            NabtoHandler.openTunnel(gateway, credentials.first)
                        }
                        err.printStackTrace()
                    }
                }

                activity?.runOnUiThread {
                    activity?.layoutGreyOut?.visibility = View.GONE
                    val action = NavGraphDirections.actionGlobalCreateGroupFragment()
                    try {
                        findNavController().navigate(action)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Tried to navigate from incorrect destination")
                    } catch (ex: IllegalStateException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Failed to navigate")
                    }
                }
            }
        }
    }

    internal enum class DefaultGroups(val displayName: String) {
        KITCHEN("Kitchen"),
        LOUNGE("Lounge"),
        HALL("Hall"),
        LIVING_ROOM("Living room"),
        BEDROOM("Bedroom"),
        BATHROOM("Bathroom"),
        DINING_ROOM("Dining room"),
        LANDING("Landing"),
        MASTER_BEDROOM("Master bedroom"),
        PORCH("Porch")
    }

    companion object {
        private const val TAG = "NoGroupsFragment"
        fun newInstance() = NoGroupsFragment()
    }

    inner class DefaultGroupsViewAdapter internal constructor() :
        RecyclerView.Adapter<DefaultGroupsViewAdapter.GroupCardViewHolder>() {

        private var groupList = emptyList<DefaultGroups>()
        internal var selectedGroups = ArrayList<DefaultGroups>()
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_create_group_tile, parent, false)
            return GroupCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: GroupCardViewHolder, position: Int) {
//        if (position < groupList.size) {
            val group = groupList[position]

            holder.name.text = group.displayName
            holder.name.setTextSize(COMPLEX_UNIT_SP, 12f)
            holder.name.textAlignment = View.TEXT_ALIGNMENT_CENTER
            holder.icon.visibility = View.GONE
            if (selectedGroups.contains(group)) {
                context?.getColor(R.color.colorTileActive)?.let {
                    holder.cardView.setCardBackgroundColor(
                        it
                    )
                }
            } else {
                context?.getColor(R.color.colorTileInactive)?.let {
                    holder.cardView.setCardBackgroundColor(
                        it
                    )
                }
            }
        }

        override fun getItemCount() = groupList.size

        internal fun getItem(position: Int): DefaultGroups? =
            if (position in groupList.indices) groupList[position] else null

        internal fun setGroups(groups: List<DefaultGroups>) {
            this.groupList = groups
            notifyDataSetChanged()
        }

        inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)
            var icon: ImageView = itemView.findViewById(R.id.ivIcon)

            init {
                binding.cardView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(binding.cardView, adapterPosition)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
