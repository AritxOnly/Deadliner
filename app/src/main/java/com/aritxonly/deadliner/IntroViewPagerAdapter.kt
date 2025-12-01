package com.aritxonly.deadliner

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aritxonly.deadliner.intro.IntroComposeFragment
import com.aritxonly.deadliner.intro.IntroFragment
import com.aritxonly.deadliner.intro.IntroFragmentWelcome
import com.aritxonly.deadliner.intro.IntroPageType
import com.aritxonly.deadliner.intro.IntroWizardFragment

// region OLD
//class IntroViewPagerAdapter(activity: IntroActivity) : FragmentStateAdapter(activity) {
//
//    private val titles = activity.resources.getStringArray(R.array.intro_titles)
//    private val descriptions = activity.resources.getStringArray(R.array.intro_descriptions)
//    private val images = activity.resources.obtainTypedArray(R.array.intro_drawables)
//
//    // 总页数 = 普通 intro 页 + 欢迎页
//    override fun getItemCount(): Int = titles.size + 1
//
//    override fun createFragment(position: Int): Fragment {
//        return if (position < titles.size) {
//            when (position) {
//                0 -> {
//                    // 第 1 页使用 Lottie
//                    IntroFragment.newLottieInstance(
//                        lottieRaw = R.raw.intro_welcome,
//                        title = titles[position],
//                        description = descriptions[position]
//                    )
//                }
//                else -> {
//                    // 其它页使用 drawable
//                    IntroFragment.newInstance(
//                        images.getResourceId(position, 0),
//                        titles[position],
//                        descriptions[position]
//                    )
//                }
//            }
//        } else {
//            // 最后一页固定 Welcome
//            IntroFragmentWelcome()
//        }
//    }
//}
// endregion


class IntroViewPagerAdapter(private val activity: IntroActivity) : FragmentStateAdapter(activity) {

    private val pages = listOf(
        IntroPageType.Cover,
        IntroPageType.Permissions,
        IntroPageType.Theme,
        IntroPageType.Wizard,
        IntroPageType.Final,
    )

    override fun getItemCount(): Int = pages.size

    fun getPageType(position: Int): IntroPageType = pages[position]

    override fun createFragment(position: Int): Fragment {
        return when (pages[position]) {
            IntroPageType.Cover -> {
                // 原来的第一页 Lottie 欢迎页
                IntroFragment.newLottieInstance(
                    lottieRaw = R.raw.intro_welcome,
                    title = activity.getString(R.string.intro_cover_title),
                    description = activity.getString(R.string.intro_cover_desc)
                )
            }

            IntroPageType.Permissions -> {
                IntroComposeFragment.newPermissions()
            }

            IntroPageType.Theme -> {
                IntroComposeFragment.newTheme()
            }

            IntroPageType.Wizard -> {
                IntroWizardFragment()
            }

            IntroPageType.Final -> {
                IntroFragmentWelcome()
            }
        }
    }
}