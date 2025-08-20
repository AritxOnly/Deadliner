package com.aritxonly.deadliner

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aritxonly.deadliner.intro.IntroFragment0
import com.aritxonly.deadliner.intro.IntroFragment1
import com.aritxonly.deadliner.intro.IntroFragment2
import com.aritxonly.deadliner.intro.IntroFragment3
import com.aritxonly.deadliner.intro.IntroFragment4
import com.aritxonly.deadliner.intro.IntroFragment5
import com.aritxonly.deadliner.intro.IntroFragment6
import com.aritxonly.deadliner.intro.IntroFragmentWelcome

class IntroViewPagerAdapter(activity: IntroActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        IntroFragment0(),
        IntroFragment1(),
        IntroFragment2(),
        IntroFragment3(),
        IntroFragment4(),
        IntroFragment5(),
        IntroFragment6(),
        IntroFragmentWelcome()
    )

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}