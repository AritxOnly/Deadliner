package com.aritxonly.deadliner

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aritxonly.deadliner.intro.IntroFragment
import com.aritxonly.deadliner.intro.IntroFragmentWelcome

class IntroViewPagerAdapter(activity: IntroActivity) : FragmentStateAdapter(activity) {

    private val titles = activity.resources.getStringArray(R.array.intro_titles)
    private val descriptions = activity.resources.getStringArray(R.array.intro_descriptions)
    private val images = activity.resources.obtainTypedArray(R.array.intro_drawables)

    // 总页数 = 普通 intro 页 + 欢迎页
    override fun getItemCount(): Int = titles.size + 1

    override fun createFragment(position: Int): Fragment {
        return if (position < titles.size) {
            when (position) {
                0 -> {
                    // 第 1 页使用 Lottie
                    IntroFragment.newLottieInstance(
                        lottieRaw = R.raw.intro_welcome,
                        title = titles[position],
                        description = descriptions[position]
                    )
                }
                else -> {
                    // 其它页使用 drawable
                    IntroFragment.newInstance(
                        images.getResourceId(position, 0),
                        titles[position],
                        descriptions[position]
                    )
                }
            }
        } else {
            // 最后一页固定 Welcome
            IntroFragmentWelcome()
        }
    }
}