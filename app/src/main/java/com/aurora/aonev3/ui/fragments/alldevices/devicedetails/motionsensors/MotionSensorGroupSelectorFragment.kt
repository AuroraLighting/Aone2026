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
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.fragments.groups.GroupsViewModel
import com.aurora.aonev3.ui.fragments.groups.groupselector.GroupSelectorViewAdapter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.fragment_group_selector.*

class MotionSensorGroupSelectorFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_group_selector, container, false)
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
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                context,
                1,
                androidx.recyclerview.widget.RecyclerView.VERTICAL,
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
}
