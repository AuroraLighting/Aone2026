package com.aurora.aonev3.ui.fragments.tour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import kotlinx.android.synthetic.main.activity_tour.*
import kotlinx.android.synthetic.main.fragment_tour.*

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
        return inflater.inflate(R.layout.fragment_tour, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (screen) {
            "home" -> {
                when (index?.toInt()) {
                    1 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_1))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    2 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_2))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    3 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_3))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            0,
                            params.rightMargin,
                            params.bottomMargin + 100
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text =
                            getString(R.string.tour_home_2)

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    4 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_home_4))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            0,
                            params.rightMargin,
                            params.bottomMargin + 80
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text =
                            getString(R.string.tour_home_3)

                        activity?.tabLayout?.visibility = View.INVISIBLE

                        btnDone.visibility = View.VISIBLE
                    }
                }

                btnDone.setOnClickListener {
                    SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                        putBoolean("homeTourDone", true)
                    }

                    activity?.finish()
                }
            }
            "all_devices" -> {
                when (index?.toInt()) {
                    1 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_all_devices_1))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            0,
                            params.rightMargin,
                            params.bottomMargin + 100
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text =
                            getString(R.string.tour_all_devices_1)

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    2 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_all_devices_2))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.INVISIBLE


                        btnDone.visibility = View.VISIBLE
                    }
                }

                btnDone.setOnClickListener {
                    SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                        putBoolean("allDevicesTourDone", true)
                    }

                    activity?.finish()
                }
            }
            "group" -> {
                when (index?.toInt()) {
                    1 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_1))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    2 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_2))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    3 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_3))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.VISIBLE

                        btnDone.visibility = View.GONE
                    }
                    4 -> {
                        imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.tour_group_4))
                        val params = imageView.layoutParams as? ConstraintLayout.LayoutParams
                        params?.setMargins(
                            params.leftMargin,
                            80,
                            params.rightMargin,
                            params.bottomMargin
                        )
                        params?.let { p ->
                            imageView.layoutParams = p
                        }

                        textView.text = ""

                        activity?.tabLayout?.visibility = View.INVISIBLE


                        btnDone.visibility = View.VISIBLE
                    }
                }

                btnDone.setOnClickListener {
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
}