package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.databinding.FragmentGroupSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.fragments.groups.GroupsViewModel
import com.aurora.aonev3.ui.fragments.groups.groupselector.GroupSelectorViewAdapter
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MotionSensorGroupSelectorFragment : Fragment() {

    private var _binding: FragmentGroupSelectorBinding? = null
    private val binding get() = _binding!!


    companion object {
        private const val TAG = "GroupSelectorFragment"
        fun newInstance() = MotionSensorGroupSelectorFragment()
    }

    private val groupsViewModel: GroupsViewModel by activityViewModels()
    private val motionSensorEventViewModel: MotionSensorEventViewModel by activityViewModels()

    private var mGroup: Group? = null
    private var mGroups: List<Group> = emptyList()

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentGroupSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")

        val listAdapter = GroupSelectorViewAdapter(activity, false)

        motionSensorEventViewModel.targetGroup.observe(viewLifecycleOwner, {
            mGroup = it
            listAdapter.selectedGroup = mGroup
            listAdapter.notifyDataSetChanged()
        })

        with(rvGroups) {
            adapter = listAdapter
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widgbinding.et.GridLayoutManager(
                context,
                1,
                androidx.recyclerview.widgbinding.et.RecyclerView.VERTICAL,
                false
            )

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin * 2, margin * 2))
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            groupsViewModel.getGroups(gateway).observe(viewLifecycleOwner, Observer { groups ->
                if (groups == null) return@Observer
                mGroups = groups
                listAdapter.setGroups(mGroups)
            })
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
            try {
                val v = activity.currentFocus
                v?.let {
                    val imm =
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                }
            } catch (ex: Exception) {
                crashlytics.recordException(ex)
            }
        }

        btnSave.setOnClickListener {
            motionSensorEventViewModel.targetGroup.postValue(listAdapter.selectedGroup)

            findNavController().popBackStack()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
