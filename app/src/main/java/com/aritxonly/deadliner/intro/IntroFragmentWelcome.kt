package com.aritxonly.deadliner.intro

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.aritxonly.deadliner.R
import com.google.android.material.button.MaterialButton

class IntroFragmentWelcome : Fragment() {

    private lateinit var circularButton: MaterialButton
    private lateinit var welcomeText: TextView
    private lateinit var welcomeTextAlt: TextView

    private var hasAnimated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_intro_welcome, container, false)

        circularButton = view.findViewById(R.id.circularButton)
        welcomeText = view.findViewById(R.id.welcomeText)
        welcomeTextAlt = view.findViewById(R.id.welcomeTextAlt)

        // 初始化状态：一开始看不到
        welcomeText.text = ""
        welcomeTextAlt.translationY = 500f
        welcomeTextAlt.alpha = 0f
        circularButton.alpha = 0f

        // 按钮点击监听可以提前绑好
        circularButton.setOnClickListener {
            if (hasAnimated) {
                setFragmentResult("buttonClick", Bundle())
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // 只在第一次真正显示这个 Fragment 时跑一遍动画
        if (!hasAnimated) {
            startAnimations()
            hasAnimated = true
        }
    }

    private fun startAnimations() {
        // TextAlt 动画
        welcomeTextAlt.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000L)
            .start()

        // Button 渐显
        circularButton.animate()
            .alpha(1f)
            .setDuration(1000L)
            .start()

        // 打字机效果
        val text = resources.getString(R.string.welcome)
        val textAnimator = ValueAnimator.ofInt(0, text.length).apply {
            duration = 2000L
            addUpdateListener { animator ->
                val len = animator.animatedValue as Int
                welcomeText.text = text.substring(0, len)
            }
        }
        textAnimator.start()
    }
}