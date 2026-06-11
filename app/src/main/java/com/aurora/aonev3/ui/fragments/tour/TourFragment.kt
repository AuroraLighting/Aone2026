package com.aurora.aonev3.ui.fragments.tour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.aurora.aonev3.databinding.FragmentTourBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_SCREEN = "screen"
private const val ARG_INDEX = "index"

/**
 * A simple [Fragment] subclass.
 * Use the [TourFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TourFragment : Fragment() {

    private var _binding: FragmentTourBinding? = null
    private val binding get() = _binding!!

    // TODO: Rename and change types of parameters
    private var screen: String? = null
    private var index: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            screen = it.getString(ARG_SCREEN)
            index = it.getString(ARG_INDEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentTourBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (screen) {
            "home" -> {
                when (index?.toInt()) {
                    1 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_1))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    2 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_2))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    3 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_3))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            0,
                            params.rightMargin,
                            params.bottomMargin + 100
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text =
                            getString(R.string.tour_home_2)

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    4 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_4))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            0,
                            params.rightMargin,
                            params.bottomMargin + 80
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text =
                            getString(R.string.tour_home_3)

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.INVISIBLE

                        binding.btnDone.visibility = View.VISIBLE
                    }
                }

                binding.btnDone.setOnClickListener {
                    SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                        putBoolean("homeTourDone", true)
                    }

                    activity?.finish()
                }
            }
            "all_devices" -> {
                when (index?.toInt()) {
                    1 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_all_devices_1))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            0,
                            params.rightMargin,
                            params.bottomMargin + 100
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text =
                            getString(R.string.tour_all_devices_1)

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    2 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_all_devices_2))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.INVISIBLE

                        binding.btnDone.visibility = View.VISIBLE
                    }
                }

                binding.btnDone.setOnClickListener {
                    SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                        putBoolean("allDevicesTourDone", true)
                    }

                    activity?.finish()
                }
            }
            "group" -> {
                when (index?.toInt()) {
                    1 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_1))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    2 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_2))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    3 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_3))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.VISIBLE

                        binding.btnDone.visibility = View.GONE
                    }
                    4 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_4))
                        val params = binding.imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            binding.imageView.layoutParams = p
                        }

                        binding.textView.text = ""

                        activity?.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)?.visibility = View.INVISIBLE

                        binding.btnDone.visibility = View.VISIBLE
                    }
                }

                binding.btnDone.setOnClickListener {
                    SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                        putBoolean("groupTourDone", true)
                    }

                    activity?.finish()
                }
            }

        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param screen Parameter 1.
         * @param index Parameter 2.
         * @return A new instance of fragment TourFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(screen: String, index: String) =
            TourFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SCREEN, screen)
                    putString(ARG_INDEX, index)
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
