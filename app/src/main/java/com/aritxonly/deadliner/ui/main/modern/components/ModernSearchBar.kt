package com.aritxonly.deadliner.ui.main.modern.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.SearchBar as M3SearchBar
import top.yukonga.miuix.kmp.basic.InputField as MiuixInputField
import top.yukonga.miuix.kmp.basic.SearchBar as MiuixSearchBar
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.main.shared.MainSearchResultsContent
import com.aritxonly.deadliner.ui.theme.AppDesignSystem
import com.aritxonly.deadliner.ui.theme.LocalAppDesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSearchBar(
    textFieldState: androidx.compose.foundation.text.input.TextFieldState,
    searchResults: List<DDLItem>,
    selectedPage: DeadlineType,
    habitViewModel: HabitViewModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    val query = textFieldState.text.toString()

    val clearQueryAndCollapse = {
        textFieldState.edit { replace(0, length, "") }
        onExpandedChange(false)
    }

    M3SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = {
                    onQueryChanged(it)
                    textFieldState.edit { replace(0, length, it) }
                },
                onSearch = { clearQueryAndCollapse() },
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    if (expanded) {
                        IconButton(onClick = clearQueryAndCollapse) {
                            M3Icon(
                                ImageVector.vectorResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    } else {
                        M3Icon(
                            ImageVector.vectorResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_events),
                        )
                    }
                },
                trailingIcon = {
                    if (expanded && query.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.edit { replace(0, length, "") } }) {
                            M3Icon(
                                ImageVector.vectorResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        MainSearchResultsContent(
            searchResults = searchResults,
            selectedPage = selectedPage,
            habitViewModel = habitViewModel,
            activity = activity,
        )
    }
}
