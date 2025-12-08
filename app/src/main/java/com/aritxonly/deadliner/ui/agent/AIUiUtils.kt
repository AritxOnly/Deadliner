package com.aritxonly.deadliner.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aritxonly.deadliner.ai.AIResult
import com.aritxonly.deadliner.ai.AIUtils.parseAIResult
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.ai.IntentType
import com.aritxonly.deadliner.ai.MixedResult
import com.aritxonly.deadliner.ai.toIntentTypeOrDefault
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun mapToUiCards(intent: IntentType, json: String): List<UiCard> {
    return when (intent) {
        IntentType.ExtractTasks -> {
            val r = parseAIResult(intent, json) as AIResult.ExtractTasks
            r.data.tasks.map { t ->
                UiCard.TaskCard(
                    title = t.name,
                    due = t.dueTime,
                    note = t.note.orEmpty()
                )
            }
        }
        IntentType.PlanDay -> {
            val r = parseAIResult(intent, json) as AIResult.PlanDay
            r.blocks.map { b ->
                UiCard.PlanBlockCard(
                    title = b.title,
                    start = b.start,
                    end = b.end,
                    location = b.location,
                    energy = b.energy,
                    linkTask = b.linkTask
                )
            }
        }
        IntentType.SplitToSteps -> {
            val r = parseAIResult(intent, json) as AIResult.SplitToSteps
            listOf(UiCard.StepsCard(title = r.data.title, checklist = r.data.checklist))
        }
    }
}

fun mapMixedToUiCards(m: MixedResult): Pair<IntentType, List<UiCard>> {
    val primary = m.primaryIntent.toIntentTypeOrDefault()
    val tasks = m.tasks.map { t ->
        UiCard.TaskCard(
            title = t.name, due = t.dueTime, note = t.note.orEmpty()
        )
    }
    val blocks = m.planBlocks.map { b ->
        UiCard.PlanBlockCard(
            title = b.title, start = b.start, end = b.end,
            location = b.location, energy = b.energy, linkTask = b.linkTask
        )
    }
    val steps = m.steps.flatMap { s -> // 一个 steps 对象也可能含多 checklist
        listOf(UiCard.StepsCard(title = s.title, checklist = s.checklist))
    }

    val ordered = buildList {
        when (primary) {
            IntentType.ExtractTasks -> { addAll(tasks); addAll(blocks); addAll(steps) }
            IntentType.PlanDay     -> { addAll(blocks); addAll(tasks); addAll(steps) }
            IntentType.SplitToSteps-> { addAll(steps); addAll(tasks); addAll(blocks) }
        }
    }
    return primary to ordered
}

fun String.tryFormatAsLocalDateTime(): String {
    return try {
        val dt = LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        dt.format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
        )
    } catch (_: Throwable) {
        this
    }
}

@Composable
fun Pill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

fun UiCard.TaskCard.toGeneratedDDLOrNull(): GeneratedDDL? = runCatching {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    GeneratedDDL(name = title, dueTime = LocalDateTime.parse(due, fmt), note = note)
}.getOrNull()