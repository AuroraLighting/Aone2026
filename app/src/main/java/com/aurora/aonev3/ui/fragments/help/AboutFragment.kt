package com.aurora.aonev3.ui.fragments.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.aurora.aonev3.databinding.FragmentAboutBinding
import com.aurora.aonev3.BuildConfig
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentAboutBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAppVersionValue.text = BuildConfig.VERSION_NAME

        binding.btnPrivacy.setOnClickListener(
            Navigation
                .createNavigateOnClickListener(
                    AboutFragmentDirections
                        .actionAboutFragmentToPrivacyFragment()
                )
        )

        binding.btnTerms.setOnClickListener(
            Navigation
                .createNavigateOnClickListener(
                    AboutFragmentDirections
                        .actionAboutFragmentToTermsFragment()
                )
        )

        binding.btnLicences.setOnClickListener {
            startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
        }

        binding.btnBeta.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroralightinghelp.zendesk.com/hc/en-gb/articles/4405190146065-BETA-Testing-new-features-before-general-release"))
            startActivity(browserIntent)
        }

        binding.btnFeature.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroralightinghelp.zendesk.com/hc/en-gb/articles/4405190035473-How-can-I-request-for-a-feature-to-be-added-to-the-App-"))
            startActivity(browserIntent)
        }

        binding.btnUpdate.setOnClickListener {
            val activity = requireActivity()

            val appUpdateManager =  AppUpdateManagerFactory.create(activity)

            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener {
                if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    it.isUpdateTypeAllowed(IMMEDIATE)) {
                    appUpdateManager.startUpdateFlowForResult(
                        it,
                        IMMEDIATE,
                        activity,
                        0
                    )
                } else {
                    if (!activity.isFinishing) {
                        AlertDialog.Builder(activity)
                            .setMessage("No update available")
                            .setPositiveButton(R.string.ok) { _, _ ->
                            }
                            .create()
                            .show()
                    }
                }
            }

            appUpdateInfoTask.addOnFailureListener {
                if (!activity.isFinishing) {
                    AlertDialog.Builder(activity)
                        .setMessage("No update available")
                        .setPositiveButton(R.string.ok) { _, _ ->
                        }
                        .create()
                        .show()
                }
            }

            appUpdateInfoTask.addOnCompleteListener {
                debug(it.toString())
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
