package com.aritxonly.deadliner.ui.main.modern.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.MainActivity
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.HabitViewModel
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.ui.main.shared.MainSearchResultsContent
import androidx.compose.ui.res.stringResource
import top.yukonga.miuix.kmp.basic.InputField as MiuixInputField
import top.yukonga.miuix.kmp.basic.SearchBar as MiuixSearchBar

@Composable
fun ModernSearchPageContent(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<DDLItem>,
    selectedPage: DeadlineType,
    habitViewModel: HabitViewModel,
    activity: MainActivity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        var expanded by rememberSaveable { mutableStateOf(false) }

        MiuixSearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            inputField = {
                MiuixInputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { expanded = true },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    label = stringResource(R.string.search_hint),
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            MainSearchResultsContent(
                searchResults = searchResults,
                selectedPage = selectedPage,
                habitViewModel = habitViewModel,
                activity = activity,
                horizontalPadding = 0.dp,
            )
        }
    }
}
