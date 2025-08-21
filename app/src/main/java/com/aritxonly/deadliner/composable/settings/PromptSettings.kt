package com.aritxonly.deadliner.composable.settings

import android.widget.Space
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.localutils.GlobalUtils

@Composable
fun PromptSettingsScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    var customPrompt by remember { mutableStateOf(GlobalUtils.customPrompt) }
    val onPromptChange: (String) -> Unit = {
        customPrompt = it
    }

    val successText = stringResource(R.string.settings_deepseek_custom_prompt_success)
    val onSaveButtonClick: () -> Unit = {
        GlobalUtils.customPrompt = customPrompt
        Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
    }

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_deepseek_custom_prompt),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SvgCard(R.drawable.svg_prompt, modifier = Modifier.padding(16.dp))

            Text(
                stringResource(R.string.settings_deepseek_custom_prompt_description),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall
            )

            SettingsSection(customColor = MaterialTheme.colorScheme.surface) {
                RoundedTextField(
                    value = customPrompt?:"",
                    onValueChange = onPromptChange,
                    hint = stringResource(R.string.settings_deepseek_custom_prompt_title),
                    metrics = RoundedTextFieldMetricsDefaults.copy(singleLine = false, minHeight = 196.dp)
                )

                Button(
                    onClick = onSaveButtonClick,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}