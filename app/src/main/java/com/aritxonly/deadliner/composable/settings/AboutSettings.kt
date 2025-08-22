package com.aritxonly.deadliner.composable.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavHostController
import com.aritxonly.deadliner.BuildConfig
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.composable.PreviewCard
import com.aritxonly.deadliner.composable.SvgCard
import com.aritxonly.deadliner.composable.TintedGradientImage
import androidx.core.net.toUri
import com.aritxonly.deadliner.localutils.GlobalUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsScreen(
    nav: NavHostController,
    navigateUp: () -> Unit,
) {
    val context = LocalContext.current

    val expressiveTypeModifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .padding(8.dp)

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_about),
        navigationIcon = {
            IconButton(onClick = navigateUp, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = expressiveTypeModifier
                )
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            val isDark: Boolean = isSystemInDarkTheme()

            PreviewCard(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter            = painterResource(R.drawable.svg_moonlight),
                        contentDescription = null,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )
                    Text(
                        text      = stringResource(R.string.app_name),
                        color     = (if (isDark) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSecondary).copy(alpha = 0.9f),
                        fontFamily = FontFamily(Font(R.font.lexend_exa, weight = FontWeight.W600)),
                        style     = MaterialTheme.typography.headlineLargeEmphasized,
                        modifier  = Modifier.align(Alignment.Center).offset(y = 32.dp)
                    )
                }
            }

            // App version
            SettingsSection(
                topLabel = stringResource(R.string.settings_highlight)
            ) {
                SettingItem(
                    headlineText = stringResource(R.string.settings_version),
                    supportingText = "v" + context.getAppVersion(),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    trailingContent = null
                )

                SettingsSectionDivider()

                SettingItem(
                    headlineText = stringResource(R.string.settings_compile_date),
                    supportingText = BuildConfig.BUILD_TIME,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    trailingContent = null
                )
            }

            SettingsSection {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_check_for_updates,
                    supporting = R.string.settings_support_check_for_updates,
                    iconRes = R.drawable.ic_update
                ) {
                    nav.navigate("update")
                }
            }

            SettingsSection(topLabel = stringResource(R.string.settings_donate)) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_donate_author,
                    supporting = R.string.settings_support_donate
                ) {
                    nav.navigate("donate")
                }
            }

            // License / Legal
            SettingsSection(
                topLabel = stringResource(R.string.settings_legal)
            ) {
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_license,
                    supporting = R.string.settings_license_summary,
                    iconRes = R.drawable.ic_license
                ) {
                    nav.navigate("license")
                }
                SettingsSectionDivider()
                SettingsDetailTextButtonItem(
                    headline = R.string.settings_privacy_policy,
                    supporting = R.string.settings_privacy_summary,
                    iconRes = R.drawable.ic_privacy
                ) {
                    nav.navigate("policy")
                }
            }

            // Links
            SettingsSection(topLabel = stringResource(R.string.settings_more)) {
                SettingsTextButtonItem(
                    text = R.string.settings_homepage,
                    iconRes = R.drawable.ic_author
                ) {
                    val url = "https://github.com/AritxOnly"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                }
                SettingsSectionDivider()
                SettingsTextButtonItem(
                    text = R.string.settings_github,
                    iconRes = R.drawable.ic_github
                ) {
                    val url = "https://github.com/AritxOnly/Deadliner"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                }
                SettingsSectionDivider()
                SettingsTextButtonItem(
                    text = R.string.settings_playground,
                    iconRes = R.drawable.ic_android
                ) {
                    val url = "https://www.magicalapk.com/app/share/app?id=55830"
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}