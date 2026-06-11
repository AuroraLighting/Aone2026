package com.aurora.aonev3.ui.fragments.gateways.sharing.gateway

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentShareGatewayBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.AccessTemplate
import com.aurora.aonev3.network.handlers.Share
import com.aurora.aonev3.ui.activities.MainActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class ShareGatewayFragment : Fragment() {

    private var _binding: FragmentShareGatewayBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = ShareGatewayFragment()
    }

    private val viewModel: ShareGatewayViewModel by viewModels()
    private val args: ShareGatewayFragmentArgs by navArgs()
    private val mGatewaySerial: String by lazy { args.gatewaySerial }
    private val userAdapter = UserShareRecyclerViewAdapter()
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentShareGatewayBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)

        with(rvUsers) {
            adapter = userAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, 0, 0))
        }

        userAdapter.onItemClickListener = object : ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = userAdapter.getItem(position) ?: return

                val action = ShareGatewayFragmentDirections.actionShareGatewayFragmentToSharedUserFragment(item.toString(), mGatewaySerial)
                findNavController().navigate(action)
            }
        }

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            getShares()
        }

        binding.btnShare.setOnClickListener {
            share(it)
        }

        etEmail.apply {
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        share(btnShare)
                    }
                }
                false
            }
        }
    }

    private fun share(view: View?) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val email = etEmail?.text?.toString()?.trim() ?: return@launch

            activity?.runOnUiThread {
                view?.isEnabled = false
                etEmail?.text?.clear()
            }

            try {
                viewModel.share(
                    mGatewaySerial,
                    email,
                    JSONArray()
                        .put(
                            JSONObject()
                                .put(
                                    "TP",
                                    if (binding.isVirtualSwitch.isChecked) AccessTemplate.EDIT_ROOT.displayName else AccessTemplate.USE_ROOT.displayName
                                )
                        )
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
            } catch (err: VolleyError) {
                App.actionFailed()
                crashlytics.recordException(Exception("failed to share"))

                var errorHandled = false

                val error = try {
                    JSONObject(String(err.networkResponse?.data ?: byteArrayOf()))
                } catch (ex: JSONException) {
                    JSONObject()
                }

                if (error.optBoolean("fail")) {
                    val errorCode = error.optInt("err_code")

                    if (errorCode == 502) {
                        activity?.runOnUiThread {
                            if (activity?.isFinishing == false) {
                                AlertDialog.Builder(activity)
                                    .setMessage("You're trying to share this Hub with yourself. This is not allowed.")
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .create()
                                    .show()
                            }
                        }
                        errorHandled = true
                    } else if (errorCode == 501) {
                        activity?.runOnUiThread {
                            if (activity?.isFinishing == false) {
                                AlertDialog.Builder(activity)
                                    .setMessage("No Account found for User, the person you're trying to share with must already have an Account")
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .create()
                                    .show()
                            }
                        }
                        errorHandled = true
                    }
                }

                if (!errorHandled) {
                    activity?.runOnUiThread {
                        if (activity?.isFinishing == false) {
                            AlertDialog.Builder(activity)
                                .setMessage("Failed to share due to an unknown error, this has been logged. Sorry for the inconvenience")
                                .setPositiveButton(getString(R.string.ok), null)
                                .create()
                                .show()
                        }
                    }
                }
            }

            getShares()
            activity?.runOnUiThread {
                view?.isEnabled = true
            }
        }
    }

    suspend fun getShares() {
        val shares = viewModel.getShares(mGatewaySerial) ?: ArrayList()

        activity?.runOnUiThread {
            userAdapter.setShares(shares)

            if (shares.isNotEmpty()) {
                tvSharedUsers.visibility = View.VISIBLE
                binding.rvUsers.visibility = View.VISIBLE
            } else {
                tvSharedUsers.visibility = View.GONE
                binding.rvUsers.visibility = View.GONE
            }
        }
    }

    class UserShareRecyclerViewAdapter: RecyclerView.Adapter<UserShareRecyclerViewAdapter.UserShareViewHolder>() {

        private var shares: ArrayList<Share> = ArrayList()
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserShareViewHolder {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_share_item, parent, false)
            return UserShareViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: UserShareViewHolder, position: Int) {
            val item = getItem(position) ?: return

            holder.setShare(item)
        }

        override fun getItemCount() = shares.count()

        fun getItem(position: Int) = shares.getOrNull(position)

        fun setShares(shares: ArrayList<Share>) {
            this.shares = shares
            notifyDataSetChanged()
        }

        inner class UserShareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val binding.tvName = itemView.findViewById(R.id.tvName)
            private val binding.tvEmail = itemView.findViewById(R.id.tvEmail)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                onItemClickListener?.onItemClick(itemView, adapterPosition)
            }

            fun setShare(share: Share) {
                binding.tvName.text = share.name
                binding.tvEmail.text = share.email
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
