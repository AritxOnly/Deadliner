package com.aritxonly.deadliner.intro

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IntroWizardViewModel : ViewModel() {

    private val _state = MutableStateFlow(IntroWizardState())
    val state = _state.asStateFlow()

    fun onAddEntryClicked() {
        _state.update { it.copy(currentStep = WizardStep.AddEntryInfo) }
    }

    fun onAddEntryInfoNext() {
        _state.update { it.copy(currentStep = WizardStep.SwipeRightComplete) }
    }

    fun onSwipeRightComplete() {
        _state.update { it.copy(currentStep = WizardStep.SwipeLeftDelete) }
    }

    fun onSwipeLeftDelete() {
        _state.update { it.copy(currentStep = WizardStep.AiEntry) }
    }

    fun onAiEntryTriggered() {
        _state.update { it.copy(currentStep = WizardStep.AiInfo) }
    }

    fun onAiInfoNext() {
        _state.update { it.copy(currentStep = WizardStep.Done) }
    }

    /**
     * next(): 自动前进
     */
    fun next() {
        val nextStep = when (state.value.currentStep) {
            WizardStep.AddEntry -> WizardStep.AddEntryInfo
            WizardStep.AddEntryInfo -> WizardStep.SwipeRightComplete
            WizardStep.SwipeRightComplete -> WizardStep.SwipeLeftDelete
            WizardStep.SwipeLeftDelete -> WizardStep.AiEntry
            WizardStep.AiEntry -> WizardStep.AiInfo
            WizardStep.AiInfo -> WizardStep.Done
            WizardStep.Done -> WizardStep.Done  // 已结束，不再前进
        }
        _state.update { it.copy(currentStep = nextStep) }
    }
}