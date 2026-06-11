package com.aurora.aonev3.ui.fragments.groups.creategroups

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentCreateGroupBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.groups.Group
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CreateGroupFragment : Fragment() {

    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = CreateGroupFragment()
    }

    private val viewModel: CreateGroupViewModel by viewModels()
    private var existingGroups: List<Group>? = null
    private var isCreateNew: MutableLiveData<Boolean> = MutableLiveData(true)

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val args: CreateGroupFragmentArgs by navArgs()
    private val isVirtual: Boolean by lazy { args.isVirtual }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context ?: return

        val groupsAdapter = CreateGroupsViewAdapter(context)

        with(recyclerView) {
            adapter = groupsAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getGroups(gateway).observe(viewLifecycleOwner, {
                val groups = it?.toList()
                if (existingGroups == null) {
                    existingGroups = groups
                }
                groups?.let {
                    groupsAdapter.setGroups(groups.filter { group ->
                        !(existingGroups?.contains(
                            group
                        ) ?: false)
                    })
                }
            })
        }

        isCreateNew.observe(viewLifecycleOwner, {
            binding.createCardLayout.visibility = if (it) View.GONE else View.VISIBLE
            binding.createEditTextLayout.visibility = if (it) View.VISIBLE else View.GONE
            binding.virtualLayout.visibility = if (it) View.VISIBLE else View.GONE
            binding.btnSave.text = if (it) getString(R.string.save) else getString(R.string.done)
        })

        if (isVirtual) {
            binding.title.text = getString(R.string.create_some_custom_groups)
            binding.isVirtualSwitch.isChecked = true
            tvName.text = getString(R.string.add_custom_group)
        }

        binding.virtualInfoIv.setOnClickListener {
            val action = CreateGroupFragmentDirections.actionCreateGroupFragmentToGroupHelpFragment()
            findNavController().navigate(action)
        }

        cardView.setOnClickListener {
            isCreateNew.value = true
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack(R.id.groupsFragment, false)
        }

        binding.isVirtualSwitch.setOnTouchListener { v, event ->
            val activity = activity ?: return@setOnTouchListener false

            if (event.action == ACTION_UP) {
                if (isVirtual) {
                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            AlertDialog.Builder(activity)
                                .setMessage("Spaces can't be nested in a Group only another Group can")
                                .setPositiveButton(R.string.ok) { _, _ ->
                                }
                                .create()
                                .show()
                        }
                    }
                } else {
                    v.performClick()
                }
            }

            return@setOnTouchListener true
        }

        binding.btnSave.setOnClickListener {
            if (isCreateNew.value == true) {
                val name = etName.text?.toString() ?: return@setOnClickListener

                if (name.isBlank()) {
                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                isCreateNew.value = isCreateNew.value != true

                viewModel.createGroup(name, binding.isVirtualSwitch.isChecked)

                etName.setText("")
                try {
                    val v = etName
                    v?.let {
                        val imm =
                            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                    v?.clearFocus()
                } catch (ex: Exception) {
                    crashlytics.recordException(ex)
                }

            } else {
                if (findNavController().previousBackStackEntry?.destination?.label != "fragment_add_nested_groups") {
                    val action =
                        CreateGroupFragmentDirections.actionCreateGroupFragmentToGroupsFragment()
                    findNavController().navigate(action)
                } else {
                    findNavController().popBackStack()
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
