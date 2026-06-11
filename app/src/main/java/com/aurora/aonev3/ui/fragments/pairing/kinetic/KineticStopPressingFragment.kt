package com.aurora.aonev3.ui.fragments.pairing.kinetic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.databinding.FragmentKineticStopPressingBinding
import com.aurora.aonev3.R

class KineticStopPressingFragment : Fragment() {

    private var _binding: FragmentKineticStopPressingBinding? = null
    private val binding get() = _binding!!


    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        val action = KineticStopPressingFragmentDirections.actionKineticStopFragmentToKineticStoreChannelFragment()

        activity?.runOnUiThread {
            try {
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
                Log.e(TAG, "Tried to navigate from incorrect destination")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentKineticStopPressingBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler.postDelayed(runnable, 2500)
    }

    override fun onDetach() {
        handler.removeCallbacks(runnable)
        super.onDetach()
    }

    companion object {
        private const val TAG = "KineticStopPressingFrag"
        fun newInstance() =
            KineticStopPressingFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
