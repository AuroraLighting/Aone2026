package com.aurora.aonev3.ui.fragments.gateways.acquiregateway

import com.aurora.aonev3.synthetic.*
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.databinding.FragmentAcquireGatewayBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.signOut
import com.aurora.aonev3.ui.activities.BarcodeActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity

class AcquireGatewayFragment : Fragment() {

    protected var _binding: FragmentAcquireGatewayBinding? = null
    protected val binding get() = _binding!!

    private var TAG = this::class.simpleName
    private val SCAN_BARCODE_RQ = 0

    private val viewModel: AcquireGatewayViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentAcquireGatewayBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.error != "" && activity?.isFinishing == false) {
            AlertDialog.Builder(activity)
                .setMessage(viewModel.error)
                .setPositiveButton(getString(R.string.ok), null)
                .create()
                .show()
        }
        viewModel.error = ""

        binding.btnScan.setOnClickListener {
            it.isEnabled = false
            startActivityForResult(Intent(activity, BarcodeActivity::class.java), SCAN_BARCODE_RQ)
        }

        binding.btnHelp.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroralightinghelp.zendesk.com"))
            startActivity(browserIntent)
        }

        binding.btnSignOut.setOnClickListener {
            signOut()
            activity?.startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finishAffinity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_BARCODE_RQ) {
            val euid = data?.getStringExtra("euid")

            activity?.runOnUiThread {
                if (euid != null) {
                    viewModel.gatewayEuid = euid

                    val action =
                        AcquireGatewayFragmentDirections.actionAcquireGatewayFragmentToAcquiringFragment()

                    try {
                        findNavController().navigate(action)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Tried to navigate from incorrect destination")
                    }
                } else {
                    if (activity?.isFinishing == false) {
                        AlertDialog.Builder(activity)
                            .setMessage("Failed to acquire Hub")
                            .setPositiveButton(getString(R.string.ok), null)
                            .create()
                            .show()
                    }
                }
            }
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = AcquireGatewayFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
