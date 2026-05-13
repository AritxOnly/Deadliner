package com.aritxonly.deadliner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PolicyScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    val policyText by remember {
        mutableStateOf(
            context.resources
                .openRawResource(R.raw.policy)
                .bufferedReader()
                .use { it.readText() }
        )
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_privacy_policy),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = navIconPaddingModifier) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        val outerPadding = rememberSettingsScaffoldOuterPaddingValues(innerPadding)
        val topOverlayPadding = rememberSettingsScaffoldTopOverlayPadding(innerPadding)
        Column(
            Modifier
                .padding(outerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(topOverlayPadding))
            MarkdownText(
                markdown = policyText,
                syntaxHighlightColor = MaterialTheme.colorScheme.surfaceContainer
            )

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
