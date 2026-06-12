package com.aurora.aonev3.ui.fragments.gateways

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.ItemLongClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GatewaySwitchFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<GatewaySwitchViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gateway_switch_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gatewayAdapter = GatewayAdapter(
            requireActivity()
        ).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val gateway = getItem(position) ?: return
                    NabtoHandler.selectedGateway = gateway
                    dismiss()
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val gateway = getItem(position) ?: return false

                    if (activity?.isFinishing != true) {
                        if (gateway.accessLevel == NabtoHandler.GatewayAccessLevel.OWNER) {
                            AlertDialog.Builder(activity)
                                .setMessage("Are you sure you want to remove this Hub?")
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                        viewModel.releaseGateway(gateway)

                                        activity?.runOnUiThread {
                                            when {
                                                NabtoHandler.nabtoGateways.isEmpty() -> {
                                                    val action =
                                                        GatewaySwitchFragmentDirections.actionGatewaySwitchFragmentToAcquiringFlow()

                                                    try {
                                                        findNavController().navigate(action)
                                                    } catch (ex: IllegalArgumentException) {
                                                        ex.printStackTrace()
                                                        Log.e(
                                                            TAG,
                                                            "Tried to navigate from incorrect destination"
                                                        )
                                                    }
                                                }
                                                NabtoHandler.nabtoGateways.count() == 1 -> {
                                                    activity?.finishActivity(0)
                                                    activity?.startActivity(
                                                        Intent(
                                                            context,
                                                            MainActivity::class.java
                                                        )
                                                    )
                                                }
                                                NabtoHandler.nabtoGateways.count() > 1 -> {
                                                    val credentials = CloudHandler.getCredentials()
                                                    if (credentials.first.isEmpty()) {
                                                        activity?.finishAffinity()
                                                        startActivity(Intent(context, SplashscreenActivity::class.java))
                                                    }
                                                    NabtoHandler.openTunnels(credentials.first)
                                                    setGateways(NabtoHandler.nabtoGateways)
                                                }
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton(R.string.no, null)
                                .create()
                                .show()
                        } else {
                            AlertDialog.Builder(activity)
                                .setMessage("Only the owner of can remove a Hub")
                                .setPositiveButton(R.string.ok, null)
                                .create()
                                .show()
                        }
                    }

                    return true
                }
            }
        }

        with(binding.rvGateways) {
            adapter = gatewayAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin * 2, margin * 2))
        }

        NabtoHandler.gatewaysConnected.observe(viewLifecycleOwner, {
            gatewayAdapter.setGateways(NabtoHandler.nabtoGateways)
        })

        NabtoHandler.gatewaysConnecting.observe(viewLifecycleOwner, {
            gatewayAdapter.setGateways(NabtoHandler.nabtoGateways)
        })

        addNewGatewayCard.setOnClickListener {
            val action = GatewaySwitchFragmentDirections.actionGatewaySwitchFragmentToAcquiringFlow()
            findNavController().navigate(action)
            dismiss()
        }

        if (NabtoHandler.nabtoGateways.isEmpty()) {
            val action = GatewaySwitchFragmentDirections.actionGatewaySwitchFragmentToAcquiringFlow()
            findNavController().navigate(action)
            dismiss()
        }
    }

    companion object {
        private const val TAG = "GatewaySwitchFragment"
        fun newInstance() =
            GatewaySwitchFragment()
    }
}

private class GatewayAdapter(val context: Context): RecyclerView.Adapter<GatewayAdapter.ViewHolder>() {
    var onItemClickListener: ItemClickListener? = null
    var onItemLongClickListener: ItemLongClickListener? = null

    private var gatewayList = emptyList<NabtoHandler.NabtoGateway>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < gatewayList.size) {
            val gateway = gatewayList[position]

            holder.name.text = gateway.name.replace("Gateway Lite ", "")

            if (gateway.isConnected) {
                holder.itemView.backgroundTintList =
                    context.getColorStateList(R.color.colorTileActive)
                holder.online.text = context.getString(R.string.online)
                holder.state.backgroundTintList = context.getColorStateList(R.color.colorAccent)

                if (NabtoHandler.selectedGateway != null) {
                    if (gateway != NabtoHandler.selectedGateway) {
                        holder.state.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_cloud
                            )
                        )
                    } else {
                        holder.state.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_tick
                            )
                        )
                    }
                } else {
                    holder.state.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_cloud
                        )
                    )
                }
            } else {
                if (gateway.isConnecting) {
                    holder.itemView.backgroundTintList =
                        context.getColorStateList(R.color.colorTileActive)
                    holder.online.text = context.getString(R.string.connecting)
                    holder.state.backgroundTintList = context.getColorStateList(R.color.colorAccent)
                    holder.state.setImageDrawable(null)
                } else {
                    holder.online.text = context.getString(R.string.offline)
                    holder.state.backgroundTintList =
                        context.getColorStateList(R.color.colorPrimary)
                    holder.state.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_no_cloud
                        )
                    )
                    holder.itemView.backgroundTintList =
                        context.getColorStateList(R.color.colorTileInactive)
                }
            }
        }
    }

    override fun getItemCount() = gatewayList.size

    fun getItem(position: Int): NabtoHandler.NabtoGateway? =
        if (position in gatewayList.indices) gatewayList[position] else null

    fun setGateways(gateways: List<NabtoHandler.NabtoGateway>) {
        this.gatewayList = gateways
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(inflater.inflate(R.layout.layout_gateway_tile, parent, false)),
        View.OnClickListener, View.OnLongClickListener {

        val name: TextView = itemView.findViewById(R.id.tvName)
        val online: TextView = itemView.findViewById(R.id.tvOnline)
        val state: ImageView = itemView.findViewById(R.id.ivState)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(itemView, adapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(itemView, adapterPosition) ?: false
        }
    }
}
