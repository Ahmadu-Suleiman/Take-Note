package com.meta4projects.takenote.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.meta4projects.takenote.fragments.FifthTutorialFragment
import com.meta4projects.takenote.fragments.FirstTutorialFragment
import com.meta4projects.takenote.fragments.FourthTutorialFragment
import com.meta4projects.takenote.fragments.SecondTutorialFragment
import com.meta4projects.takenote.fragments.ThirdTutorialFragment

class SliderAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return FirstTutorialFragment()
            1 -> return SecondTutorialFragment()
            2 -> return ThirdTutorialFragment()
            3 -> return FourthTutorialFragment()
            4 -> return FifthTutorialFragment()
        }
        return FirstTutorialFragment()
    }

    override fun getItemCount(): Int {
        return 5
    }
}