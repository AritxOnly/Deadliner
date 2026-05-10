package com.aritxonly.deadliner.ui.main.simplified

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.DeadlineDetailActivity
// 别名导入，防止 M3 和 MIUIX 冲突
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.SearchBar as M3SearchBar
import top.yukonga.miuix.kmp.basic.SearchBar as MiuixSearchBar
import top.yukonga.miuix.kmp.basic.InputField as MiuixInputField
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.base.TextButton
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.shared.MainSearchResultsContent
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    onQueryChanged: (String) -> Unit,
    searchResults: List<DDLItem>,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    onMoreAnchorChange: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    useAvatar: Boolean = false,
    avatarPainter: Painter? = null,
    activity: MainActivity? = null,
    habitViewModel: HabitViewModel,
    expanded: Boolean,
    onExpandedChangeExternal: (Boolean) -> Unit = {},
    selectedPage: DeadlineType,
    miuixMode: Boolean = LocalAppDesignSystem.current == AppDesignSystem.MIUIX,
    resultsHorizontalPadding: Dp = 16.dp,
    mixedResultTypes: Boolean = false,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val isEnabled = GlobalUtils.motivationalQuotes
    val excitementArray = stringArrayResource(id = R.array.excitement_array).toList()
    var idx by rememberSaveable {
        mutableIntStateOf(if (excitementArray.isNotEmpty()) (0 until excitementArray.size).random() else 0)
    }

    LaunchedEffect(expanded) { onExpandedChangeExternal(expanded) }
    LaunchedEffect(isEnabled, excitementArray) {
        if (!isEnabled || excitementArray.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(30_000)
            idx = (idx + 1) % excitementArray.size
        }
    }

    val handleClose = {
        textFieldState.edit { replace(0, length, "") }
        onExpandedChangeExternal(false)
        focusManager.clearFocus()
    }

    if (!miuixMode) {
        // ==========================================
        // M3 模式：维持原样，头像和更多按钮在搜索框内部
        // ==========================================
        val searchBarPadding by animateDpAsState(
            targetValue = if (expanded) 0.dp else 16.dp,
            label = "Search bar padding"
        )

        Box(modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
            M3SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = searchBarPadding)
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        query = textFieldState.text.toString(),
                        onQueryChange = {
                            onQueryChanged(it)
                            textFieldState.edit { replace(0, length, it) }
                        },
                        onSearch = { handleClose() },
                        expanded = expanded,
                        onExpandedChange = onExpandedChangeExternal,
                        placeholder = {
                            AnimatedHintPlaceholder(
                                expanded = expanded, isEnabled = isEnabled, excitement = excitementArray, idx = idx
                            )
                        },
                        leadingIcon = {
                            if (expanded) {
                                M3IconButton(onClick = handleClose) {
                                    M3Icon(ImageVector.vectorResource(R.drawable.ic_back), stringResource(R.string.back))
                                }
                            } else {
                                M3Icon(ImageVector.vectorResource(R.drawable.ic_search), stringResource(R.string.search_events))
                            }
                        },
                        trailingIcon = {
                            if (!expanded) {
                                val iconModifier = Modifier.clip(CircleShape).onGloballyPositioned { onMoreAnchorChange(it.boundsInWindow()) }
                                if (useAvatar && avatarPainter != null) {
                                    M3IconButton(onClick = onMoreClick, modifier = iconModifier.size(32.dp)) {
                                        Image(painter = avatarPainter, contentDescription = stringResource(R.string.user), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                } else {
                                    M3IconButton(onClick = onMoreClick, modifier = iconModifier) {
                                        M3Icon(ImageVector.vectorResource(R.drawable.ic_more), stringResource(R.string.settings_more))
                                    }
                                }
                            } else {
                                if (textFieldState.text.isNotEmpty()) {
                                    M3IconButton(onClick = { textFieldState.edit { replace(0, length, "") } }) {
                                        M3Icon(ImageVector.vectorResource(R.drawable.ic_close), stringResource(R.string.close))
                                    }
                                }
                            }
                        }
                    )
                },
                expanded = expanded,
                onExpandedChange = { exp ->
                    if (!exp) textFieldState.edit { replace(0, length, "") }
                    onExpandedChangeExternal(exp)
                }
            ) {
                SearchResultContent(
                    searchResults = searchResults,
                    selectedPage = selectedPage,
                    activity = activity,
                    habitViewModel = habitViewModel,
                    horizontalPadding = resultsHorizontalPadding,
                    mixedResultTypes = mixedResultTypes,
                )
            }
        }
    } else {
        val defaultHint = stringResource(R.string.search_hint)

        // 目标 hint
        val targetHint = when {
            expanded -> defaultHint
            isEnabled && excitementArray.isNotEmpty() -> excitementArray[idx % excitementArray.size]
            else -> defaultHint
        }

        MiuixSearchBar(
            modifier = modifier.fillMaxWidth().padding(8.dp, 8.dp, 8.dp, 0.dp),
            inputField = {
                MiuixInputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        onQueryChanged(it)
                        textFieldState.edit { replace(0, length, it) }
                    },
                    onSearch = { handleClose() },
                    expanded = expanded,
                    onExpandedChange = onExpandedChangeExternal,
                    label = targetHint
                )
            },
            expanded = expanded,
            onExpandedChange = { exp ->
                if (!exp) textFieldState.edit { replace(0, length, "") }
                onExpandedChangeExternal(exp)
            }
        ) {
            SearchResultContent(
                searchResults = searchResults,
                selectedPage = selectedPage,
                activity = activity,
                habitViewModel = habitViewModel,
                horizontalPadding = resultsHorizontalPadding,
                mixedResultTypes = mixedResultTypes,
            )
        }
    }
}

@Composable
private fun SearchResultContent(
    searchResults: List<DDLItem>,
    selectedPage: DeadlineType,
    activity: MainActivity?,
    habitViewModel: HabitViewModel,
    horizontalPadding: Dp,
    mixedResultTypes: Boolean,
) {
    val targetActivity = activity ?: return
    MainSearchResultsContent(
        searchResults = searchResults,
        selectedPage = selectedPage,
        habitViewModel = habitViewModel,
        activity = targetActivity,
        horizontalPadding = horizontalPadding,
        mixedResultTypes = mixedResultTypes,
    )
}
