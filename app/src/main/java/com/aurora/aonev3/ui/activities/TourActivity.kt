package com.aurora.aonev3.ui.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.fragments.tour.TourFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_tour.*

class TourActivity : FragmentActivity() {

    private var tour: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tour)

        tour = intent.getStringExtra("tour") ?: ""

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
        override fun getItemCount(): Int {
            return when (tour) {
                "home" -> 4
                "all_devices" -> 2
                "group" -> 4
                else -> 3
            }
        }

        override fun createFragment(position: Int): Fragment {
            return when (tour) {
                "home" -> TourFragment.newInstance("home", (position + 1).toString())
                "all_devices" -> TourFragment.newInstance("all_devices", (position + 1).toString())
                "group" -> TourFragment.newInstance("group", (position + 1).toString())
                else -> TourFragment.newInstance("home", (position + 1).toString())
            }
        }
    }

}