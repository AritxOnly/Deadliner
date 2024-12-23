package com.aritxonly.deadliner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.button.MaterialButton

class IntroFragmentWelcome : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_welcome, container, false)

        // 获取按钮
        val circularButton: MaterialButton = view.findViewById(R.id.circular_button)

        // 设置按钮点击事件，使用 FragmentResult 传递事件
        circularButton.setOnClickListener {
            setFragmentResult("buttonClick", Bundle()) // 通知 Activity 按钮被点击
        }

        return view
    }
}