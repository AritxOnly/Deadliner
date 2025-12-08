package com.aritxonly.deadliner.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.aritxonly.deadliner.ui.intro.IntroWizardRoot
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

class IntroWizardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DeadlinerTheme {
                    IntroWizardRoot(
                        onWizardFinished = {
                            setFragmentResult("wizardFinished", bundleOf())
                        },
                        onWizardSkipped = {
                            setFragmentResult("wizardSkipped", bundleOf())
                        }
                    )
                }
            }
        }
    }
}

// -------- State & VM --------

data class IntroWizardState(
    val currentStep: WizardStep = WizardStep.AddEntry
)

sealed class WizardStep {
    data object AddEntry : WizardStep()           // 引导点击添加入口
    data object AddEntryInfo : WizardStep()       // AddDDL 说明页
    data object SwipeRightComplete : WizardStep() // 右滑完成
    data object SwipeLeftDelete : WizardStep()    // 左滑删除
    data object AiEntry : WizardStep()            // AI 唤醒（上滑 / 点击图标）
    data object AiInfo : WizardStep()             // AI 说明页
    data object Done : WizardStep()               // 已全部完成
}
