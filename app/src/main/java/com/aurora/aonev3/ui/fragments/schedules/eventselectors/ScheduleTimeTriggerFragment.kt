package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.window.SplashScreen
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentTimeTriggerBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.TimeConditionFragment
import com.aurora.aonev3.ui.fragments.schedules.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.security.auth.login.LoginException

const val LOCATION_PERMISSION_RC = 0

class ScheduleTimeTriggerFragment : Fragment() {

    private var _binding: FragmentTimeTriggerBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventTimeViewModel: IScheduleTimeViewModel
    private val viewModel: IScheduleTimeViewModel by viewModels<ScheduleTimeViewModel>()
    private val sunriseSunsetViewModel: SunriseSunsetViewModel by activityViewModels()
    private val navArgs: ScheduleTimeTriggerFragmentArgs by navArgs()
    private val sender: String by lazy { navArgs.sender }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            requestLocation()
        } else {
            AlertDialog.Builder(activity)
                .setMessage("Without permission to use location we're unable to set up sunrise / sunset")
                .setPositiveButton(getString(R.string.ok), null)
                .create()
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")

        when (sender) {
            ScheduleEventFragment::class.simpleName -> {
                eventTimeViewModel = ViewModelProvider(activity).get(ScheduleEventViewModel::class.java)
            }
            "${TimeConditionFragment::class.simpleName}_start" -> {
                eventTimeViewModel = ViewModelProvider(activity).get(ConditionStartTimeViewModel::class.java)
            }
            "${TimeConditionFragment::class.simpleName}_end" -> {
                eventTimeViewModel = ViewModelProvider(activity).get(ConditionEndTimeViewModel::class.java)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentTimeTriggerBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")
        val gateway = NabtoHandler.selectedGateway ?: return

        sunriseSunsetViewModel.getCollections(gateway).observe(viewLifecycleOwner) { collections ->
            sunriseSunsetViewModel.sunriseSunsetLogicCollection = collections?.toList()?.firstOrNull { collection ->
                val metadata = collection.metadata

                metadata.collectionType == CollectionType.SUNRISE_SUNSET
            }
            sunriseSunsetViewModel.sunriseSunsetRules = sunriseSunsetViewModel.rules.toList().filter {
                it.logicCollectionId == sunriseSunsetViewModel.sunriseSunsetLogicCollection?.id
            }
            sunriseSunsetViewModel.sunriseSunsetTimers = sunriseSunsetViewModel.timers.toList().filter {
                it.logicCollectionId == sunriseSunsetViewModel.sunriseSunsetLogicCollection?.id
            }
        }

        sunriseSunsetViewModel.getRules(gateway).observe(viewLifecycleOwner) { rules ->
            sunriseSunsetViewModel.rules = rules
            sunriseSunsetViewModel.sunriseSunsetRules = rules.filter {
                it.logicCollectionId == sunriseSunsetViewModel.sunriseSunsetLogicCollection?.id
            }
        }

        sunriseSunsetViewModel.getTimers(gateway).observe(viewLifecycleOwner) { timers ->
            sunriseSunsetViewModel.timers = timers
            sunriseSunsetViewModel.sunriseSunsetTimers = timers.filter {
                it.logicCollectionId == sunriseSunsetViewModel.sunriseSunsetLogicCollection?.id
            }
        }

        viewModel.trigger.observe(viewLifecycleOwner) { trigger ->
            trigger?.let {
                val string = String.format("%02d:%02d", trigger.hour, trigger.minute)
                binding.btnTime.text = string

                when (it.trigger) {
                    SunriseSunsetType.SUNRISE -> {
                        binding.btnSunrise.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileActive)
                        binding.btnSunsbinding.et.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileInactive)
                        binding.btnTime.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileInactive)
                    }
                    SunriseSunsetType.SUNSET -> {
                        binding.btnSunrise.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileInactive)
                        binding.btnSunsbinding.et.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileActive)
                        binding.btnTime.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileInactive)
                    }
                    SunriseSunsetType.TIME -> {
                        binding.btnSunrise.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileInactive)
                        binding.btnSunsbinding.et.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileInactive)
                        binding.btnTime.backgroundTintList =
                            activity.getColorStateList(R.color.colorTileActive)
                    }
                }
            }
        }

        eventTimeViewModel.trigger.observe(viewLifecycleOwner) {
            viewModel.updateTrigger(hour = it.hour, minute = it.minute, triggerType = it.trigger, offset = it.offset)
        }

        binding.btnSunrise.setOnClickListener {
            viewModel.updateTrigger(triggerType = SunriseSunsetType.SUNRISE)
        }

        binding.btnSunsbinding.et.setOnClickListener {
            viewModel.updateTrigger(triggerType = SunriseSunsetType.SUNSET)
        }

        binding.btnTime.setOnClickListener {
            val trigger = viewModel.trigger.value
            val triggerHour = trigger?.hour ?: 0
            val triggerMinute = trigger?.minute ?: 0
            TimePickerDialog(
                activity,
                { _, hour, minute ->
                    viewModel.updateTrigger(hour = hour, minute = minute, triggerType = SunriseSunsetType.TIME)
                },
                triggerHour,
                triggerMinute,
                true
            ).show()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            if (viewModel.trigger.value?.trigger == SunriseSunsetType.SUNRISE ||
                    viewModel.trigger.value?.trigger == SunriseSunsetType.SUNSET) {
                if (sunriseSunsetViewModel.sunriseSunsetLogicCollection == null) {
                    try {
                        when {
                            ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // You can use the API that requires the permission.
                                requestLocation()
                            }
                            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                                AlertDialog.Builder(activity)
                                    .setMessage("Location is needed to retrieve local sunrise / sunset time")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                    .setNegativeButton(getString(R.string.cancel), null)
                                    .create()
                                    .show()
                            }
                            else -> {
                                requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return@setOnClickListener
                }
            }
            eventTimeViewModel.updateTrigger(hour = viewModel.trigger.value?.hour ?: 0, minute = viewModel.trigger.value?.minute ?: 0, triggerType = viewModel.trigger.value?.trigger ?: SunriseSunsetType.TIME)

            findNavController().popBackStack()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        activity ?: return
        requireActivity().runOnUiThread {
            requireActivity().findViewById<android.view.View>(R.id.layoutGreyOut).visibility = View.VISIBLE
        }
        LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                requestLocation()
                return@addOnSuccessListener
            }
            sunriseSunsetViewModel.viewModelScope.launch(Dispatchers.IO) {
                val gateway = NabtoHandler.selectedGateway ?: return@launch
                try {
                    sunriseSunsetViewModel.createSunriseSunsetCollection(location)
                    SyncHandler.syncLogicCollectionsCached(gateway, true)
                    SyncHandler.syncLogicRulesAndTimersCached(gateway, true)

                } catch (err: VolleyError) {
                    activity ?: return@launch
                    App.actionFailed()
                    err.printStackTrace()
                    gateway.isConnected = false
                    val credentials = CloudHandler.getCredentials()
                    if (credentials.first.isEmpty()) {
                        activity?.finishAffinity()
                        startActivity(Intent(context, SplashscreenActivity::class.java))
                    }
                    NabtoHandler.openTunnel(gateway, credentials.first)
                    delay(1000)
                    return@launch requestLocation()
                }

                eventTimeViewModel.updateTrigger(triggerType = viewModel.trigger.value?.trigger ?: SunriseSunsetType.TIME)

                activity ?: return@launch
                requireActivity().runOnUiThread {
                    activity ?: return@runOnUiThread
                    requireActivity().findViewById<android.view.View>(R.id.layoutGreyOut).visibility = View.GONE

                    findNavController().popBackStack()
                }
            }
        }
    }

    companion object {
        fun newInstance() =
            ScheduleTimeTriggerFragment()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_RC -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestLocation()
                } else {
                    AlertDialog.Builder(activity)
                        .setMessage("Without permission to use location we're unable to set up sunrise / sunset")
                        .setPositiveButton(getString(R.string.ok), null)
                        .create()
                        .show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
