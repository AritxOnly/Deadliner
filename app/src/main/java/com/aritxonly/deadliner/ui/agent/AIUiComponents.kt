package com.aritxonly.deadliner.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.iconResource

@Composable
fun TaskCardView(card: UiCard.TaskCard, onAdd: () -> Unit, onCopy: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.due.tryFormatAsLocalDateTime(), style = MaterialTheme.typography.bodyMedium)
            if (card.note.isNotBlank()) {
                Text(card.note, style = MaterialTheme.typography.bodyMedium)
            }
            // actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_task)) }
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.copy)) }
            }
        }
    }
}

@Composable
fun PlanBlockCardView(card: UiCard.PlanBlockCard, onAdd: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text("${card.start.tryFormatAsLocalDateTime()} — ${card.end.tryFormatAsLocalDateTime()}",
                style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                card.location?.let { Pill(it) }
                card.energy?.let { Pill(it.uppercase()) }
                card.linkTask?.let { Pill(stringResource(R.string.linked_task, it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save_and_add_to_calendar)) }
            }
        }
    }
}

@Composable
fun StepsCardView(card: UiCard.StepsCard, onCreateSubtasks: (String, List<String>) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                card.checklist.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconResource(R.drawable.ic_check), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = { /* TODO Copy list */ }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.copy_list))
                }
            }
        }
    }
}

@Composable
fun EmptyHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            iconResource(GlobalUtils.getDeadlinerAIConfig().getCurrentLogo()),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.ai_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ErrorBlock(message: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                iconResource(R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.parse_failed), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}