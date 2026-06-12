package com.aurora.aonev3.ui.activities

import com.aurora.aonev3.synthetic.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.aonev3.databinding.ActivityTourBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.fragments.intro.IntroFragment
import com.google.android.material.tabs.TabLayoutMediator

class IntroActivity : FragmentActivity() {

    protected var _binding: ActivityTourBinding? = null
    protected val binding get() = _binding!!


    private var target: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTourBinding.inflate(layoutInflater)
        setContentView(binding.root)

        target = intent.getStringExtra("target") ?: ""

        binding.pager.adapter = ScreenSlidePagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.pager) { _, _ ->
            //Some implementation
        }.attach()

    }

    override fun onResume() {
        SyncHandler.restartCoroutineScope()
        NabtoHandler.cancelClosing()

        super.onResume()
    }

    override fun onPause() {
        NabtoHandler.closeNabtoDelayed()

        super.onPause()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment = IntroFragment.newInstance(target, (position + 1).toString())
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
