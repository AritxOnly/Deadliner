@file:OptIn(ExperimentalMaterial3Api::class)
package com.aritxonly.deadliner.ui.main.simplified

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.DeadlineAlarmScheduler
import com.aritxonly.deadliner.DeadlineDetailActivity
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.SettingsActivity
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.SearchFilter
import com.aritxonly.deadliner.data.MainViewModel
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLStatus
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.PartyPresets
import com.aritxonly.deadliner.ui.main.DDLItemCardSimplified
import com.aritxonly.deadliner.ui.main.DDLItemCardSwipeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainDisplay(
    ddlList: List<DDLItem>,
    dueSoonCounts: Map<DeadlineType, Int>,
    refreshState: MainViewModel.RefreshState,
    selectedPage: DeadlineType,
    onSearch: (String) -> Unit,
    onReload: () -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
    vm: MainViewModel,
    onCelebrate: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<DDLItem?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing =
        refreshState is MainViewModel.RefreshState.Loading &&
                !refreshState.silent

    Column(modifier) {
        val textFieldState = rememberTextFieldState()

        MainSearchBar(
            textFieldState = textFieldState,
            onSearch = onSearch,
            searchResults = emptyList(),
            onMoreClick = { activity.startActivity(Intent(context, SettingsActivity::class.java)) }
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = {
                vm.refreshFromPull(selectedPage)
            },
            modifier = modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 96.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = ddlList,
                    key = { it.id }
                ) { item ->
                    when (selectedPage) {
                        DeadlineType.TASK -> {
                            val startTime = GlobalUtils.parseDateTime(item.startTime)
                            val endTime = GlobalUtils.parseDateTime(item.endTime)
                            val now = LocalDateTime.now()

                            val remainingTimeText =
                                if (!item.isCompleted)
                                    GlobalUtils.buildRemainingTime(
                                        context,
                                        startTime,
                                        endTime,
                                        true,
                                        now
                                    )
                                else stringResource(R.string.completed)

                            val progress = computeProgress(startTime, endTime, now)
                            val status =
                                DDLStatus.calculateStatus(startTime, endTime, now, item.isCompleted)

                            DDLItemCardSwipeable(
                                title = item.name,
                                remainingTimeAlt = remainingTimeText,
                                note = item.note,
                                progress = progress,
                                isStarred = item.isStared,
                                status = status,
                                onClick = {
                                    val intent = DeadlineDetailActivity.newIntent(context, item)
                                    activity.startActivity(intent)
                                },
                                onComplete = {
                                    GlobalUtils.triggerVibration(activity, 100)

                                    val realItem = DDLRepository().getDDLById(item.id)
                                        ?: return@DDLItemCardSwipeable
                                    val newItem = realItem.copy(
                                        isCompleted = !realItem.isCompleted,
                                        completeTime = if (!realItem.isCompleted) LocalDateTime.now()
                                            .toString() else ""
                                    )
                                    DDLRepository().updateDDL(newItem)
                                    vm.loadData(selectedPage)
                                    if (newItem.isCompleted) {
                                        if (GlobalUtils.fireworksOnFinish) onCelebrate?.invoke()
                                        Toast.makeText(
                                            activity,
                                            R.string.toast_finished,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            activity,
                                            R.string.toast_definished,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onDelete = {
                                    GlobalUtils.triggerVibration(activity, 200)
                                    pendingDelete = item
                                }
                            )
                        }

                        DeadlineType.HABIT -> {
                            Text(item.name)
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    DDLRepository().deleteDDL(target.id)
                    DeadlineAlarmScheduler.cancelAlarm(activity.applicationContext, target.id)
                    pendingDelete = null
                    vm.loadData(selectedPage)
                    Toast.makeText(context, R.string.toast_deletion, Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

fun computeProgress(
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    now: LocalDateTime = LocalDateTime.now(),
    progressDir: Boolean = GlobalUtils.progressDir
): Float {
    if (startTime == null || endTime == null) return 0f
    val total = Duration.between(startTime, endTime).toMinutes().toFloat().coerceAtLeast(1f)
    val elapsed = Duration.between(startTime, now).toMinutes().toFloat().coerceIn(0f, total)
    val remaining = total - elapsed

    return if (progressDir) {
        // 已经过的时间占比
        elapsed / total
    } else {
        // 剩余的时间占比
        remaining / total
    }
}

@Composable
fun MainSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isEnabled = GlobalUtils.motivationalQuotes
    val excitementArray = stringArrayResource(id = R.array.excitement_array).toList()

    var idx by rememberSaveable {
        mutableIntStateOf(
            if (excitementArray.isNotEmpty()) (0 until excitementArray.size).random() else 0
        )
    }

    LaunchedEffect(isEnabled, excitementArray) {
        if (!isEnabled || excitementArray.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(30_000)
            idx = (idx + 1) % excitementArray.size
        }
    }

    val searchBarPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else 16.dp,
        label = "Search bar padding"
    )

    Box(
        modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = searchBarPadding)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = { textFieldState.edit { replace(0, length, it) } },
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        expanded = false
                        focusManager.clearFocus()
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },

                    placeholder = {
                        val hint = if (expanded) {
                            stringResource(R.string.search_hint)
                        } else {
                            if (isEnabled && excitementArray.isNotEmpty())
                                excitementArray[idx]
                            else stringResource(R.string.search_hint)
                        }
                        val style = if (expanded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium

                        Text(hint, style = style, maxLines = 1)
                    },

                    leadingIcon = {
                        if (expanded) {
                            IconButton(
                                onClick = {
                                    expanded = false
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                                    contentDescription = "返回"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                contentDescription = "搜索"
                            )
                        }
                    },

                    trailingIcon = {
                        if (!expanded) {
                            IconButton(onClick = onMoreClick) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_more),
                                    contentDescription = "更多选项"
                                )
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                searchResults.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                textFieldState.edit { replace(0, length, result) }
                                expanded = false
                                focusManager.clearFocus()
                            }
                    )
                }
            }
        }
    }
}