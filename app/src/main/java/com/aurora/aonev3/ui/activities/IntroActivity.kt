package com.aurora.aonev3.ui.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.fragments.intro.IntroFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tour.*

class IntroActivity : FragmentActivity() {

    private var target: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tour)

        target = intent.getStringExtra("target") ?: ""

        pager.adapter = ScreenSlidePagerAdapter(this)

        TabLayoutMediator(tabLayout, pager) { _, _ ->
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

}