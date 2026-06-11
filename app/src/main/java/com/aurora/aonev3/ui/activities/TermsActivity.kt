package com.aurora.aonev3.ui.activities

import android.os.Bundle
import androidx.binding.fragment.app.FragmentActivity
import com.aurora.aonev3.databinding.ActivityTermsBinding
import com.aurora.aonev3.R

class TermsActivity : FragmentActivity() {

    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}