package com.aritxonly.deadliner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ListView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aritxonly.deadliner.ai.GeneratedDDL
import com.aritxonly.deadliner.calendar.CalendarHelper
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.localutils.DynamicColorsExtension
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.GlobalUtils.toDateTimeString
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.CalendarEvent
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.toJson
import com.aritxonly.deadliner.ui.base.Button
import com.aritxonly.deadliner.ui.base.OutlinedTextField
import com.aritxonly.deadliner.ui.base.Scaffold
import com.aritxonly.deadliner.ui.base.Switch
import com.aritxonly.deadliner.ui.base.TabRow
import com.aritxonly.deadliner.ui.expressiveTypeModifier
import com.aritxonly.deadliner.ui.navIconPaddingModifier
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.aritxonly.deadliner.ai.AIUtils
import com.aritxonly.deadliner.ui.iconResource

@SuppressLint("SimpleDateFormat")
class AddDDLActivity : DeadlinerAppCompatActivity() {

    private val repo = DDLRepository()
    private val habitRepo = HabitRepository()

    private var selectedPage by mutableIntStateOf(0)

    private var ddlName by mutableStateOf("")
    private var taskNote by mutableStateOf("")
    private var habitDescription by mutableStateOf("")
    private var isStarred by mutableStateOf(false)

    private var startTime by mutableStateOf(LocalDateTime.now())
    private var endTime by mutableStateOf(LocalDateTime.now().plusHours(1))

    private var frequencyInput by mutableStateOf("1")
    private var totalInput by mutableStateOf("")
    private var frequencyType by mutableStateOf(DeadlineFrequency.DAILY)

    private var habitReminderEnabled by mutableStateOf(false)
    private var habitReminderTime by mutableStateOf(LocalTime.of(20, 0))
    private var aiInputText by mutableStateOf("")
    private var isAiLoading by mutableStateOf(false)
    private var autoRunAiOnAppear by mutableStateOf(false)
    private var aiAutoTriggered by mutableStateOf(false)

    private var calendarEventId: Long? = null
    private var pendingCalendarAction: PendingCalendarAction? = null

    private enum class PendingCalendarAction {
        IMPORT,
        SAVE_TO_CALENDAR
    }

    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val action = pendingCalendarAction
            pendingCalendarAction = null

            when (action) {
                PendingCalendarAction.IMPORT -> {
                    if (hasCalendarReadPermission()) {
                        loadCalendarEventsAndShowDialog()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.permission_calendar_error, "Missing calendar permission"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                PendingCalendarAction.SAVE_TO_CALENDAR -> {
                    if (hasCalendarWritePermission()) {
                        save(toCalendar = true)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.permission_calendar_error, "Missing calendar permission"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                null -> Unit
            }
        }

    companion object {
        const val EXTRA_PREFILL_TEXT = "EXTRA_PREFILL_TEXT"
        const val EXTRA_AUTO_RUN_AI = "EXTRA_AUTO_RUN_AI"
        private val REMINDER_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val AI_DUE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        Log.d("AddDDLActivity", "available: ${com.google.android.material.color.DynamicColors.isDynamicColorAvailable()}")

        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)

        DynamicColorsExtension.applyApp(this.application, GlobalUtils.seedColor)
        DynamicColorsExtension.apply(this, GlobalUtils.seedColor)
        GlobalUtils.decideHideFromRecent(this, this)

        selectedPage = intent.getIntExtra("EXTRA_CURRENT_TYPE", 0).coerceIn(0, 1)

        val generatedDDL = intent.getParcelableExtra<GeneratedDDL>("EXTRA_GENERATE_DDL")
        val fullDDL = intent.getParcelableExtra<DDLItem>("EXTRA_FULL_DDL")
        val prefillText = intent.getStringExtra(EXTRA_PREFILL_TEXT)
        val autoRunAi = intent.getBooleanExtra(EXTRA_AUTO_RUN_AI, false)

        applyInitialData(generatedDDL, fullDDL, prefillText, autoRunAi)
        promptDeadlinerDonateIfNeeded()

        setContent {
            DeadlinerTheme {
                AddDDLScreen()
            }
        }
    }

    private fun applyInitialData(
        generatedDDL: GeneratedDDL?,
        fullDDL: DDLItem?,
        prefillText: String?,
        autoRunAi: Boolean
    ) {
        generatedDDL?.let {
            ddlName = it.name
            endTime = it.dueTime
            taskNote = it.note
        }

        fullDDL?.let { item ->
            ddlName = item.name
            isStarred = item.isStared
            calendarEventId = item.calendarEventId

            GlobalUtils.parseDateTime(item.startTime)?.let { startTime = it }
            GlobalUtils.parseDateTime(item.endTime)?.let { endTime = it }

            when (item.type) {
                DeadlineType.TASK -> {
                    selectedPage = 0
                    taskNote = item.note
                }
                DeadlineType.HABIT -> {
                    selectedPage = 1

                    val meta = GlobalUtils.parseHabitMetaData(item.note)
                    frequencyInput = meta.frequency.toString()
                    totalInput = if (meta.total > 0) meta.total.toString() else ""
                    frequencyType = meta.frequencyType

                    val habit = habitRepo.getHabitByDdlId(item.id)
                    habitDescription = habit?.description.orEmpty()
                    val alarm = habit?.alarmTime.orEmpty()
                    if (alarm.isNotBlank()) {
                        runCatching { LocalTime.parse(alarm, REMINDER_TIME_FORMATTER) }
                            .onSuccess {
                                habitReminderEnabled = true
                                habitReminderTime = it
                            }
                    }
                }
            }
        }

        if (fullDDL == null) {
            val safePrefill = prefillText?.trim().orEmpty()
            if (safePrefill.isNotEmpty()) {
                aiInputText = safePrefill
            }
            if (autoRunAi && safePrefill.isNotEmpty()) {
                autoRunAiOnAppear = true
            }
        }
    }

    private fun promptDeadlinerDonateIfNeeded() {
        if (!GlobalUtils.shouldShowDeadlinerDonatePrompt()) return
        GlobalUtils.markDeadlinerDonatePromptShown()
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.deadliner_donate_plan_title))
            .setMessage(getString(R.string.deadliner_donate_plan_message))
            .setPositiveButton(getString(R.string.deadliner_donate_go)) { _, _ ->
                startActivity(
                    Intent(this, SettingsActivity::class.java).apply {
                        putExtra(SettingsActivity.EXTRA_INITIAL_ROUTE, SettingsRoute.Donate.route)
                    }
                )
            }
            .setNegativeButton(getString(R.string.later), null)
            .show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddDDLScreen() {
        val tabs = listOf(stringResource(R.string.task), stringResource(R.string.habit))
        val textFieldShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
        LaunchedEffect(autoRunAiOnAppear, aiInputText, aiAutoTriggered) {
            if (autoRunAiOnAppear && !aiAutoTriggered && aiInputText.isNotBlank()) {
                aiAutoTriggered = true
                parseAiInput()
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = contentColorFor(Color.Transparent),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.add_task),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finishAfterTransition() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = expressiveTypeModifier
                            )
                        }
                    },
                    actions = {
                        if (selectedPage == 0) {
                            IconButton(onClick = { onImportFromCalendarClick() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_event),
                                    contentDescription = stringResource(R.string.select_calendar_to_import),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = expressiveTypeModifier
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp)
                )
            },
            bottomBar = {
                BottomActions(
                    onSave = { save(false) },
                    onSaveToCalendar = { save(true) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(padding)
            ) {
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedPage,
                    onTabSelected = { selectedPage = it },
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surface) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AiQuickAddCard()
                    }

                    item {
                        OutlinedTextField(
                            value = ddlName,
                            onValueChange = { ddlName = it },
                            label = { Text(stringResource(R.string.add_ddl_name)) },
                            miuixLabel = stringResource(R.string.add_ddl_name),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = textFieldShape
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.star),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(
                                    checked = isStarred,
                                    onCheckedChange = { isStarred = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    if (selectedPage == 0) {
                        item {
                            DateTimeCard(
                                title = stringResource(R.string.start_time),
                                value = formatLocalDateTime(startTime),
                                onClick = {
                                    GlobalUtils.showDateTimePicker(supportFragmentManager) { selected ->
                                        startTime = selected
                                        if (endTime.isBefore(selected)) {
                                            endTime = selected.plusHours(1)
                                        }
                                    }
                                }
                            )
                        }

                        item {
                            DateTimeCard(
                                title = stringResource(R.string.end_time),
                                value = formatLocalDateTime(endTime),
                                onClick = {
                                    GlobalUtils.showDateTimePicker(
                                        supportFragmentManager,
                                        startTime,
                                        { chosen -> Toast.makeText(this@AddDDLActivity, getString(R.string.please_choose_the_time_after, chosen), Toast.LENGTH_SHORT).show() }
                                    ) { selected ->
                                        endTime = selected
                                    }
                                }
                            )
                        }
                    }

                    if (selectedPage == 0) {
                        item {
                            OutlinedTextField(
                                value = taskNote,
                                onValueChange = { taskNote = it },
                                label = { Text(stringResource(R.string.add_ddl_note)) },
                                miuixLabel = stringResource(R.string.add_ddl_note),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = textFieldShape
                            )
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = habitDescription,
                                onValueChange = { habitDescription = it },
                                label = { Text(stringResource(R.string.add_ddl_note)) },
                                miuixLabel = stringResource(R.string.add_ddl_note),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 5,
                                shape = textFieldShape
                            )
                        }

                        item {
                            Text(
                                text = stringResource(R.string.add_ddl_frequency_type),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FrequencyTypeRow(
                                selected = frequencyType,
                                onSelect = { frequencyType = it }
                            )
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = frequencyInput,
                                    onValueChange = { frequencyInput = it.filter(Char::isDigit) },
                                    label = { Text(stringResource(R.string.add_ddl_frequency)) },
                                    miuixLabel = stringResource(R.string.add_ddl_frequency),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = textFieldShape
                                )
                                OutlinedTextField(
                                    value = totalInput,
                                    onValueChange = { totalInput = it.filter(Char::isDigit) },
                                    label = { Text(stringResource(R.string.add_ddl_total)) },
                                    miuixLabel = stringResource(R.string.add_ddl_total),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = textFieldShape
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = GlobalUtils.generateHabitNote(
                                    this@AddDDLActivity,
                                    frequencyInput.toIntOrNull(),
                                    totalInput.toIntOrNull(),
                                    frequencyType
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            HabitReminderSection(
                                enabled = habitReminderEnabled,
                                onEnabledChange = { habitReminderEnabled = it },
                                time = habitReminderTime,
                                onPickTime = {
                                    GlobalUtils.showDateTimePicker(supportFragmentManager) { selected ->
                                        habitReminderTime = selected.toLocalTime().withSecond(0).withNano(0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AiQuickAddCard() {
        val textFieldShape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.ai_quick_add),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = aiInputText,
                    onValueChange = { aiInputText = it },
                    label = { Text(stringResource(R.string.ai_quick_add_placeholder)) },
                    miuixLabel = stringResource(R.string.ai_quick_add_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    shape = textFieldShape
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { parseAiInput() },
                        enabled = !isAiLoading && aiInputText.trim().isNotEmpty()
                    ) {
                        Text(
                            if (isAiLoading) stringResource(R.string.ai_parsing)
                            else stringResource(R.string.ai_parse)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BottomActions(
        onSave: () -> Unit,
        onSaveToCalendar: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedPage == 0) {
                Button(
                    onClick = onSaveToCalendar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save_and_add_to_calendar),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    @Composable
    private fun DateTimeCard(
        title: String,
        value: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            ),
            shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @Composable
    private fun HabitReminderSection(
        enabled: Boolean,
        onEnabledChange: (Boolean) -> Unit,
        time: LocalTime,
        onPickTime: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.habit_enable_notification),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                if (enabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPickTime),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.habit_notify_at, time.format(REMINDER_TIME_FORMATTER)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = iconResource(R.drawable.ic_event),
                            contentDescription = stringResource(R.string.set_time),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FrequencyTypeRow(
        selected: DeadlineFrequency,
        onSelect: (DeadlineFrequency) -> Unit
    ) {
        val options = listOf(
            DeadlineFrequency.TOTAL to stringResource(R.string.frequency_none),
            DeadlineFrequency.DAILY to stringResource(R.string.frequency_daily),
            DeadlineFrequency.WEEKLY to stringResource(R.string.frequency_weekly),
            DeadlineFrequency.MONTHLY to stringResource(R.string.frequency_monthly)
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (type, label) ->
                SegmentedButton(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label) },
                    colors = androidx.compose.material3.SegmentedButtonDefaults.colors()
                )
            }
        }
    }

    private fun parseAiInput() {
        val text = aiInputText.trim()
        if (text.isEmpty() || isAiLoading) return

        isAiLoading = true
        lifecycleScope.launch {
            try {
                val json = AIUtils.generateMixed(this@AddDDLActivity, text)
                val mixed = AIUtils.parseMixedResult(json)
                val firstTask = mixed.tasks.firstOrNull()

                when (selectedPage) {
                    0 -> {
                        if (firstTask != null) {
                            if (ddlName.isBlank()) ddlName = firstTask.name
                            else ddlName = firstTask.name

                            runCatching {
                                LocalDateTime.parse(firstTask.dueTime, AI_DUE_TIME_FORMATTER)
                            }.onSuccess { parsedDue ->
                                endTime = parsedDue
                                if (startTime.isAfter(endTime)) {
                                    startTime = endTime.minusHours(1)
                                }
                            }

                            taskNote = firstTask.note.orEmpty()
                        } else {
                            if (taskNote.isBlank()) taskNote = text
                        }
                    }

                    1 -> {
                        val suggestedName = firstTask?.name?.trim().orEmpty()
                        val suggestedDesc = firstTask?.note?.trim().orEmpty()
                        if (suggestedName.isNotEmpty()) {
                            ddlName = suggestedName
                        } else if (ddlName.isBlank()) {
                            ddlName = text.take(20)
                        }

                        habitDescription = if (suggestedDesc.isNotEmpty()) {
                            suggestedDesc
                        } else {
                            text
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AddDDLActivity", "AI parse failed", e)
                Toast.makeText(
                    this@AddDDLActivity,
                    getString(R.string.ai_parse_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isAiLoading = false
                autoRunAiOnAppear = false
            }
        }
    }

    private fun save(toCalendar: Boolean) {
        val name = ddlName.trim()
        if (name.isBlank()) {
            Toast.makeText(this, getString(R.string.add_ddl_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (toCalendar && !hasCalendarWritePermission()) {
            pendingCalendarAction = PendingCalendarAction.SAVE_TO_CALENDAR
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
            return
        }

        if (selectedPage == 0) {
            saveTask(name = name, toCalendar = toCalendar)
        } else {
            saveHabit(name = name)
        }
    }

    private fun saveTask(name: String, toCalendar: Boolean) {
        val ddlId = repo.insertDDL(
            name = name,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            note = taskNote,
            type = DeadlineType.TASK,
            calendarEventId = calendarEventId
        )

        repo.getDDLById(ddlId)?.let { inserted ->
            val item = if (inserted.isStared != isStarred) inserted.copy(isStared = isStarred) else inserted
            if (item != inserted) {
                repo.updateDDL(item)
            }

            if (GlobalUtils.deadlineNotification) {
                DeadlineAlarmScheduler.scheduleExactAlarm(applicationContext, item)
                DeadlineAlarmScheduler.scheduleUpcomingDDLAlarm(applicationContext, item)
            }

            if (toCalendar) {
                val calendarHelper = CalendarHelper(this)
                lifecycleScope.launch {
                    try {
                        val eventId = calendarHelper.insertEvent(item)
                        item.calendarEventId = eventId
                        repo.updateDDL(item)
                        Toast.makeText(
                            this@AddDDLActivity,
                            getString(R.string.add_calendar_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("Calendar", e.toString())
                        Toast.makeText(
                            this@AddDDLActivity,
                            getString(R.string.add_calendar_failed, e.toString()),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        setResult(RESULT_OK)
        finishAfterTransition()
    }

    private fun saveHabit(name: String) {
        val frequency = frequencyInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val total = totalInput.toIntOrNull()

        val meta = HabitMetaData(
            completedDates = emptySet(),
            frequencyType = frequencyType,
            frequency = frequency,
            total = total ?: 0,
            refreshDate = LocalDate.now().toString()
        )

        val ddlId = repo.insertDDL(
            name = name,
            startTime = "",
            endTime = "",
            note = meta.toJson(),
            type = DeadlineType.HABIT
        )

        repo.getDDLById(ddlId)?.takeIf { it.isStared != isStarred }?.let {
            repo.updateDDL(it.copy(isStared = isStarred))
        }

        val habitPeriod = when (frequencyType) {
            DeadlineFrequency.DAILY -> HabitPeriod.DAILY
            DeadlineFrequency.WEEKLY -> HabitPeriod.WEEKLY
            DeadlineFrequency.MONTHLY -> HabitPeriod.MONTHLY
            DeadlineFrequency.TOTAL -> HabitPeriod.DAILY
        }

        val habitGoalType =
            if (frequencyType == DeadlineFrequency.TOTAL) HabitGoalType.TOTAL
            else HabitGoalType.PER_PERIOD

        val habitTimesPerPeriod =
            if (frequencyType == DeadlineFrequency.TOTAL) 1
            else frequency

        val habitTotalTarget =
            if (frequencyType == DeadlineFrequency.TOTAL) total
            else null

        habitRepo.createHabitForDdl(
            ddlId = ddlId,
            name = name,
            period = habitPeriod,
            timesPerPeriod = habitTimesPerPeriod,
            goalType = habitGoalType,
            totalTarget = habitTotalTarget,
            description = habitDescription.ifBlank { null }
        )

        habitRepo.getHabitByDdlId(ddlId)?.let { habit ->
            val updated = habit.copy(
                alarmTime = if (habitReminderEnabled) habitReminderTime.format(REMINDER_TIME_FORMATTER) else null
            )
            habitRepo.updateHabit(updated)

            if (habitReminderEnabled) {
                DeadlineAlarmScheduler.scheduleHabitNotifyAlarm(applicationContext, ddlId)
            } else {
                DeadlineAlarmScheduler.cancelHabitNotifyAlarm(applicationContext, ddlId)
            }
        }

        setResult(RESULT_OK)
        finishAfterTransition()
    }

    private fun onImportFromCalendarClick() {
        if (!hasCalendarReadPermission()) {
            pendingCalendarAction = PendingCalendarAction.IMPORT
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
            return
        }
        loadCalendarEventsAndShowDialog()
    }

    private fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCalendarWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadCalendarEventsAndShowDialog() {
        if (!hasCalendarReadPermission()) {
            Toast.makeText(
                this,
                getString(R.string.permission_calendar_error, "Missing calendar permission"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val calendarHelper = CalendarHelper(applicationContext)
        val calendarEvents = try {
            calendarHelper.queryAllCalendarEvents()
        } catch (e: SecurityException) {
            Log.e("Calendar", "No calendar permission", e)
            Toast.makeText(
                this,
                getString(R.string.permission_calendar_error, e.message ?: "SecurityException"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (calendarEvents.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_task_add_calendar), Toast.LENGTH_SHORT).show()
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_calendar_to_import)
                .setMessage(getString(R.string.no_task_add_calendar))
                .setNeutralButton(R.string.filter_calendar_account) { _, _ -> showCalendarFilterDialog() }
                .setNegativeButton(R.string.close, null)
                .show()
            return
        }

        data class EventItem(val event: CalendarEvent) {
            val display: String
                get() {
                    val time = GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())
                    return "${event.title} - ${time?.let(::formatLocalDateTime) ?: getString(R.string.parse_failed)}"
                }

            override fun toString() = display
        }

        val items = calendarEvents.map { EventItem(it) }
        val dialogView = layoutInflater.inflate(R.layout.dialog_calendar_events, null, false)
        val etSearch = dialogView.findViewById<TextInputEditText>(R.id.searchEditText)
        val lvEvents = dialogView.findViewById<ListView>(R.id.eventListView)

        open class EventAdapter(
            ctx: Context,
            items: List<EventItem>
        ) : ArrayAdapter<EventItem>(ctx, R.layout.dialog_single_choice_layout, android.R.id.text1, items) {
            private val original = items.toList()
            private val filtered = items.toMutableList()

            override fun getFilter(): Filter = object : Filter() {
                override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                    filtered.clear()
                    if (constraint.isNullOrBlank()) {
                        filtered.addAll(original)
                    } else {
                        val kw = constraint.toString().lowercase()
                        filtered.addAll(original.filter { it.display.lowercase().contains(kw) })
                    }
                    values = filtered.toList()
                    count = filtered.size
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(c: CharSequence?, results: FilterResults) {
                    clear()
                    addAll(results.values as List<EventItem>)
                    notifyDataSetChanged()
                }
            }

            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val rb = view.findViewById<RadioButton>(R.id.radio)
                rb.isChecked = (parent as ListView).isItemChecked(position)
                return view
            }
        }

        val adapter = EventAdapter(this, items)
        lvEvents.adapter = adapter
        lvEvents.choiceMode = ListView.CHOICE_MODE_SINGLE

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        var selectedPosition = -1
        lvEvents.setOnItemClickListener { _, _, pos, _ ->
            selectedPosition = pos
            lvEvents.setItemChecked(pos, true)
            adapter.notifyDataSetChanged()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_calendar_to_import)
            .setView(dialogView)
            .setNeutralButton(R.string.filter_calendar_account) { _, _ -> showCalendarFilterDialog() }
            .setPositiveButton(R.string.settings_import) { dialog, _ ->
                if (selectedPosition >= 0) {
                    val event = adapter.getItem(selectedPosition)?.event
                    if (event != null) {
                        ddlName = event.title.orEmpty()
                        taskNote = event.description.orEmpty()
                        GlobalUtils.parseDateTime(event.startMillis.toDateTimeString())?.let { endTime = it }
                        calendarEventId = event.id
                    }
                } else {
                    Toast.makeText(this, getString(R.string.no_event_select), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCalendarFilterDialog() {
        if (!hasCalendarReadPermission()) {
            Toast.makeText(
                this,
                getString(R.string.permission_calendar_error, "Missing calendar permission"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val helper = CalendarHelper(applicationContext)
        val accounts = try {
            helper.getAllCalendarAccounts()
        } catch (e: SecurityException) {
            Log.e("Calendar", "No calendar permission", e)
            Toast.makeText(
                this,
                getString(R.string.permission_calendar_error, e.message ?: "SecurityException"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.no_valid_calendar_account, Toast.LENGTH_SHORT).show()
            return
        }

        val names = accounts.map { it.accountName.ifEmpty { it.accountName } }.toTypedArray()
        val savedSet = GlobalUtils.filteredCalendars ?: setOf()
        val checked = names.map { savedSet.contains(it) }.toBooleanArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_calendar_account_to_hide)
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.accept) { _, _ ->
                val newFiltered = names.zip(checked.toList())
                    .filter { it.second }
                    .map { it.first }
                    .toSet()

                GlobalUtils.filteredCalendars = newFiltered
                Toast.makeText(this, R.string.calendar_filter_saved, Toast.LENGTH_SHORT).show()
                loadCalendarEventsAndShowDialog()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun formatLocalDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
        return dateTime.format(formatter)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdgeForAllDevices()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        enableEdgeToEdgeForAllDevices()
    }
}
