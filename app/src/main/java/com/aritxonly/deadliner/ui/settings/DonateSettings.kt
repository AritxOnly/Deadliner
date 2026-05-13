package com.aritxonly.deadliner.ui.settings

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier

@Composable
fun DonateScreen(
    navigateUp: () -> Unit
) {
    val context = LocalContext.current
    var orderId by remember { mutableStateOf(GlobalUtils.getDeadlinerDonateOrderId()) }
    val submitted = GlobalUtils.isDeadlinerDonateOrderSubmitted()

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_donate),
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
                modifier = navIconPaddingModifier
            ) {
                Icon(
                    painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { padding ->
        SettingsScrollColumn(
            contentPadding = padding,
            modifier = Modifier,
        ) {
            SettingsSection(mainContent = true, enabled = true) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.settings_donate_emotional), style = MaterialTheme.typography.titleMedium)

                    Text(stringResource(R.string.settings_donate_thanks), style = MaterialTheme.typography.bodyLarge)

                    OutlinedTextField(
                        value = orderId,
                        onValueChange = { orderId = it },
                        label = { Text(stringResource(R.string.deadliner_donate_order_id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        enabled = !submitted
                    )

                    Button(
                        onClick = {
                            val trimmed = orderId.trim()
                            if (trimmed.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.deadliner_donate_order_id_empty), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            GlobalUtils.submitDeadlinerDonateOrder(trimmed)
                            Toast.makeText(context, context.getString(R.string.deadliner_donate_order_saved), Toast.LENGTH_SHORT).show()
                        },
                        enabled = !submitted,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(stringResource(R.string.deadliner_donate_submit_order))
                    }

                    if (submitted) {
                        Text(
                            text = stringResource(R.string.deadliner_donate_no_more_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
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
