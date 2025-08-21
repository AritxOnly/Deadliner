package com.aritxonly.deadliner.intro

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.aritxonly.deadliner.R
import com.google.android.material.color.MaterialColors

class IntroFragment : Fragment() {

    companion object {
        private const val ARG_IMG = "arg_img"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_LOTTIE_RAW = "arg_lottie_raw"

        fun newInstance(@DrawableRes img: Int, title: String, description: String): IntroFragment {
            return IntroFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_IMG, img)
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, description)
                }
            }
        }

        fun newLottieInstance(@RawRes lottieRaw: Int, title: String, description: String): IntroFragment {
            return IntroFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_LOTTIE_RAW, lottieRaw)
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, description)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_intro_page, container, false)

        val imageView: ImageView = view.findViewById(R.id.imageView)
        val lottieView: com.airbnb.lottie.LottieAnimationView = view.findViewById(R.id.lottieView)
        val titleView: TextView = view.findViewById(R.id.mainDescription)
        val descView: TextView = view.findViewById(R.id.mainDescriptionContent)

        val imgRes = arguments?.getInt(ARG_IMG) ?: 0
        val lottieRaw = arguments?.getInt(ARG_LOTTIE_RAW) ?: 0
        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val desc = arguments?.getString(ARG_DESC).orEmpty()

        titleView.text = title
        descView.text = desc

        if (lottieRaw != 0) {
            // 显示 Lottie，隐藏静态图
            imageView.visibility = View.GONE
            lottieView.visibility = View.VISIBLE
            applyDynamicColorLottie(view)
            lottieView.setAnimation(lottieRaw)
            lottieView.playAnimation()
        } else {
            // 显示静态图
            lottieView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            if (imgRes != 0) imageView.setImageResource(imgRes)
        }

        // 整页淡入
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 600
            start()
        }
        return view
    }

    private fun applyDynamicColorLottie(view: View) {
        // 假设你已在布局里用 LottieAnimationView（id = lottieView）
        // 并已 setAnimation(R.raw.portal_time) / playAnimation()

        val lottieView: LottieAnimationView = view.findViewById(R.id.lottieView)

        val primary   = MaterialColors.getColor(lottieView, com.google.android.material.R.attr.colorPrimaryContainer)
        val secondary = MaterialColors.getColor(lottieView, com.google.android.material.R.attr.colorPrimaryVariant)
        val outline   = MaterialColors.getColor(lottieView, com.google.android.material.R.attr.colorPrimaryContainer)

        lottieView.addValueCallback(
            KeyPath("Ð¼Ð°ÑÐºÐ° 1", "**", "Stroke 1"),
            LottieProperty.STROKE_COLOR,
            LottieValueCallback(primary)
        )

        lottieView.addValueCallback(
            KeyPath("Ð¼Ð°ÑÐºÐ° 2", "**", "Stroke 1"),
            LottieProperty.STROKE_COLOR,
            LottieValueCallback(primary)
        )

        lottieView.addValueCallback(
            KeyPath("Ð¼Ð°ÑÐºÐ° 3", "**", "Stroke 1"),
            LottieProperty.STROKE_COLOR,
            LottieValueCallback(primary)
        )

        lottieView.addValueCallback(
            KeyPath("Ð¿ÑÑÐ¶Ð¸Ð½ÐºÐ°", "**", "Stroke 1"),
            LottieProperty.STROKE_COLOR,
            LottieValueCallback(outline)
        )

        lottieView.addValueCallback(
            KeyPath("ÑÐ°ÑÐ¸Ðº 2", "**", "Fill 1"),
            LottieProperty.COLOR,
            LottieValueCallback(secondary)
        )

        // （可选）如果要整体降低某些层透明度（0–100）
        lottieView.addValueCallback(
            KeyPath("ÑÐ°ÑÐ¸Ðº 2", "**"),
            LottieProperty.OPACITY,
            LottieValueCallback(85)
        )

        // 主题切换后需要刷新时，可调用：
        lottieView.invalidate()
    }
}