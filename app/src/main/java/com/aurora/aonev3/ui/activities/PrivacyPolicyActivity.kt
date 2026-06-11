package com.aurora.aonev3.ui.activities

import android.os.Bundle
import androidx.binding.fragment.app.FragmentActivity
import com.aurora.aonev3.databinding.ActivityPrivacyPolicyBinding
import com.aurora.aonev3.R

class PrivacyPolicyActivity : FragmentActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}