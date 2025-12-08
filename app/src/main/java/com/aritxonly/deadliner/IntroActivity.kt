package com.aritxonly.deadliner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.aritxonly.deadliner.intro.IntroPageType
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.PartyPresets
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import nl.dionsegijn.konfetti.xml.KonfettiView

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var buttonNext: MaterialButton
    private lateinit var tabLayout: TabLayout
    private lateinit var pageIndicator: LinearProgressIndicator
    private lateinit var adapter: IntroViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeForAllDevices()
        setContentView(R.layout.activity_intro)

        normalizeRootInsets()
        DynamicColors.applyToActivityIfAvailable(this)

        pageIndicator = findViewById(R.id.pageIndicator)
        viewPager = findViewById(R.id.viewPager_intro)
        tabLayout = findViewById(R.id.tabLayout)
        buttonNext = findViewById(R.id.buttonNext)

        adapter = IntroViewPagerAdapter(this)
        viewPager.adapter = adapter

        // 进度条最大值 = 最后一页的 index
        pageIndicator.max = adapter.itemCount - 1
        pageIndicator.progress = 0

        // 将 TabLayout 和 ViewPager2 绑定（Tab 仅做指示，不可点击）
        TabLayoutMediator(tabLayout, viewPager) { tab, _ ->
            tab.view.isClickable = false
        }.attach()

        buttonNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                goToMainActivity()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pageIndicator.progress = position

                val pageType = adapter.getPageType(position)

                when (pageType) {
                    IntroPageType.Wizard -> {
                        viewPager.isUserInputEnabled = false
                        buttonNext.visibility = View.GONE
                    }

                    IntroPageType.Final -> {
                        viewPager.isUserInputEnabled = false
                        buttonNext.visibility = View.GONE
                        pageIndicator.animate()
                            .setDuration(1000)
                            .alpha(0f)
                            .start()
                    }

                    else -> {
                        // 其他页：正常显示 Next，允许滑动
                        viewPager.isUserInputEnabled = true
                        buttonNext.visibility = View.VISIBLE
                        if (pageIndicator.alpha < 1f) {
                            pageIndicator.alpha = 1f
                        }
                    }
                }
            }
        })

        // Welcome Fragment “开始使用”按钮
        supportFragmentManager.setFragmentResultListener("buttonClick", this) { _, _ ->
            goToMainActivity()
        }

        // Wizard 完成：直接跳到最终欢迎页
        supportFragmentManager.setFragmentResultListener("wizardFinished", this) { _, _ ->
            viewPager.currentItem = IntroPageType.Final.index
        }

        // Wizard 跳过：同样跳到最终欢迎页
        supportFragmentManager.setFragmentResultListener("wizardSkipped", this) { _, _ ->
            viewPager.currentItem = IntroPageType.Final.index
        }
    }

    private fun goToMainActivity() {
        val konfettiView: KonfettiView = findViewById(R.id.konfetti_view)
        konfettiView.start(PartyPresets.explode())

        GlobalUtils.firstRun = false
        GlobalUtils.showIntroPage = false

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000L)
    }

    private fun normalizeRootInsets() {
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root, null)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, status.top, v.paddingRight, navigation.bottom)
            insets
        }
    }
}