package com.aritxonly.deadliner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        DynamicColors.applyToActivityIfAvailable(this)

        // 获取主题中的 colorSurface 值
        val colorSurface = getThemeColor(com.google.android.material.R.attr.colorSurface)

        // 设置状态栏和导航栏颜色
        setSystemBarColors(colorSurface, isLightColor(colorSurface))

        pageIndicator = findViewById(R.id.pageIndicator)

        viewPager = findViewById(R.id.viewPager_intro)

        val adapter = IntroViewPagerAdapter(this)
        viewPager.adapter = adapter

        tabLayout = findViewById(R.id.tabLayout)
        // 将 TabLayout 和 ViewPager2 绑定
        TabLayoutMediator(tabLayout, viewPager) { tab, _ ->
            // 设置 Tab 不可点击
            tab.view.isClickable = false
        }.attach()

        buttonNext = findViewById(R.id.buttonNext)
        buttonNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            // 如果还没到最后一页，就让 ViewPager 跳到下一页
            if (currentItem < adapter.itemCount - 1) {
                viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                // 如果已经是最后一页，跳转到主界面
                goToMainActivity()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                pageIndicator.progress = position

                // 如果滑到最后一页，你可以把箭头图标/文字换成“完成”
                if (position == adapter.itemCount - 1) {
                    viewPager.isUserInputEnabled = false
                    buttonNext.visibility = View.GONE
                    pageIndicator.animate().setDuration(1000).alpha(0f).start()
                } else {
                    viewPager.isUserInputEnabled = true
                    buttonNext.visibility = View.VISIBLE
                }
            }
        })

        // 监听 Fragment 按钮事件
        supportFragmentManager.setFragmentResultListener("buttonClick", this) { _, _ ->
            goToMainActivity() // 调用跳转逻辑
        }
    }

    private fun goToMainActivity() {
        // 开始播放烟花动画
        val konfettiView: KonfettiView = findViewById(R.id.konfetti_view)
        konfettiView.start(PartyPresets.explode())

        GlobalUtils.firstRun = false
        GlobalUtils.showIntroPage = false

        // 延迟关闭当前 Activity，与动画同步
        Handler(Looper.getMainLooper()).postDelayed({
            // 启动 MainActivity 并设置过渡动画
            val intent = Intent(this, MainActivity::class.java)

            startActivity(intent)
            finish()
        }, 1000)
    }

    /**
     * 设置状态栏和导航栏颜色及图标颜色
     */
    private fun setSystemBarColors(color: Int, lightIcons: Boolean) {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = color
            navigationBarColor = color

            // 设置状态栏图标颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.setSystemBarsAppearance(
                    if (lightIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                insetsController?.setSystemBarsAppearance(
                    if (lightIcons) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = if (lightIcons) {
                    decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }

    /**
     * 获取主题颜色
     * @param attributeId 主题属性 ID
     * @return 颜色值
     */
    private fun getThemeColor(attributeId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attributeId, typedValue, true)
        return typedValue.data
    }

    /**
     * 判断颜色是否为浅色
     */
    private fun isLightColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * ((color shr 16 and 0xFF) / 255.0) +
                0.587 * ((color shr 8 and 0xFF) / 255.0) +
                0.114 * ((color and 0xFF) / 255.0))
        return darkness < 0.5
    }
}