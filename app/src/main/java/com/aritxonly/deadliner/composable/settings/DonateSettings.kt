package com.aritxonly.deadliner.composable.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R

@Composable
fun DonateScreen(
    navigateUp: () -> Unit
) {
    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_donate),
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
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(mainContent = true, enabled = true) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("穷学生在线求投喂 🥹", style = MaterialTheme.typography.titleMedium)

                    Text("你的支持是对我最大的鼓励~", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius)))) {
                Image(
                    painterResource(R.drawable.alipay),
                    contentDescription = null
                )
            }
        }
    }
}