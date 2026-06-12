package com.aurora.aonev3.ui.fragments.intro

import com.aurora.aonev3.synthetic.*
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.aurora.aonev3.databinding.FragmentIntroBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity

private const val ARG_INDEX = "index"
private const val ARG_TARGET = "target"

/**
 * A simple [Fragment] subclass.
 */
class IntroFragment : Fragment() {

    protected var _binding: FragmentIntroBinding? = null
    protected val binding get() = _binding!!

    private var index: String? = null
    private var target: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            index = it.getString(ARG_INDEX)
            target = it.getString(ARG_TARGET)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return if (index?.toInt() != 4) {
            inflater.inflate(R.layout.fragment_intro, container, false)
        } else {
            inflater.inflate(R.layout.fragment_intro_end, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

                when (index?.toInt()) {
                    1 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.intro_1))

                        binding.textView.text = getString(R.string.intro_1_text_1)
                        binding.textView2.text = getString(R.string.intro_1_text_2)

                        activity?.tabLayout?.visibility = View.VISIBLE
                    }
                    2 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.intro_2))

                        binding.textView.text = getString(R.string.intro_2_text_1)
                        binding.textView2.text = getString(R.string.intro_2_text_2)

                        activity?.tabLayout?.visibility = View.VISIBLE
                    }
                    3 -> {
                        binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.intro_3))

                        binding.textView.text = getString(R.string.intro_3_text_1)
                        binding.textView2.text = getString(R.string.intro_3_text_2)

                        activity?.tabLayout?.visibility = View.VISIBLE
                    }
                    4 -> {
                        activity?.tabLayout?.visibility = View.INVISIBLE

                        binding.btnDone.setOnClickListener {
                            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                                putBoolean("introDone", true)
                            }

                            activity?.let {
                                if (target == "login") {
                                    startActivity(Intent(it, LoginActivity::class.java))
                                } else {
                                    startActivity(Intent(it, MainActivity::class.java))
                                }
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
         * @param index Parameter 2.
         * @return A new instance of fragment TourFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(target: String, index: String) =
            IntroFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INDEX, index)
                    putString(ARG_TARGET, target)
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
