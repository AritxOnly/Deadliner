package com.aritxonly.deadliner.intro

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.core.os.bundleOf
import com.aritxonly.deadliner.ui.intro.PermissionsScreen
import com.aritxonly.deadliner.ui.intro.UiModeScreen
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

private const val ARG_PAGE = "arg_intro_compose_page"

class IntroComposeFragment : Fragment() {

    companion object {
        fun newPermissions(): IntroComposeFragment =
            IntroComposeFragment().apply {
                arguments = bundleOf(ARG_PAGE to "permissions")
            }

        fun newTheme(): IntroComposeFragment =
            IntroComposeFragment().apply {
                arguments = bundleOf(ARG_PAGE to "theme")
            }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val page = arguments?.getString(ARG_PAGE) ?: "permissions"

        return ComposeView(requireContext()).apply {
            setContent {
                DeadlinerTheme {
                    when (page) {
                        "permissions" -> PermissionsScreen()
                        "theme" -> UiModeScreen()
                        else -> PermissionsScreen()
                    }
                }
            }
        }
    }
}