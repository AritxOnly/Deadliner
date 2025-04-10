package com.aritxonly.deadliner

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.button.MaterialButton
import org.w3c.dom.Text

class IntroFragmentWelcome : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_welcome, container, false)

        // 获取按钮
        val circularButton: MaterialButton = view.findViewById(R.id.circularButton)
        val welcomeText: TextView = view.findViewById(R.id.welcomeText)
        val welcomeTextAlt: TextView = view.findViewById(R.id.welcomeTextAlt)

        // TextAlt 动画效果（从下往上上浮）
        val textAltAnimator = ObjectAnimator.ofFloat(welcomeTextAlt, "translationY", 500f, 0f)
        val textAltAlphaAnimator = ObjectAnimator.ofFloat(welcomeTextAlt, "alpha", 0f, 1f)
        textAltAnimator.duration = 1000
        textAltAlphaAnimator.duration = 500
        textAltAnimator.start()
        textAltAlphaAnimator.start()

        // CircularButton 渐显动画
        val buttonAlphaAnimator = ObjectAnimator.ofFloat(circularButton, "alpha", 0f, 1f)
        buttonAlphaAnimator.duration = 1000
        buttonAlphaAnimator.start()

        // 打字机效果：逐字显示文本
        val text = resources.getString(R.string.welcome)
        val textAnimator = ValueAnimator.ofInt(0, text.length)
        textAnimator.duration = 2000 // 控制打字机速度
        textAnimator.addUpdateListener { animator ->
            val currentLength = animator.animatedValue as Int
            welcomeText.text = text[0].toString()
            if (welcomeText.text.isNotEmpty()) { // 保证有内容
                // 逐步显示文本
                welcomeText.text = text.substring(0, currentLength)
            }
        }
        textAnimator.start()

        // 设置按钮点击事件，使用 FragmentResult 传递事件
        circularButton.setOnClickListener {
            setFragmentResult("buttonClick", Bundle()) // 通知 Activity 按钮被点击
        }

        return view
    }
}