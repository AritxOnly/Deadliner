package com.aritxonly.deadliner.intro

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.aritxonly.deadliner.R

class IntroFragment0 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro_page0, container, false)
        val pageView: ConstraintLayout = view.findViewById(R.id.pageView)

        val pageViewAnimator = ObjectAnimator.ofFloat(pageView, "alpha", 0f, 1f)
        pageViewAnimator.start()

        return view
    }
}