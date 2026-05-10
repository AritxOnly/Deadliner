package com.aritxonly.deadliner.ai

import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.data.HabitRepository
import com.aritxonly.deadliner.model.DeadlineFrequency
import com.aritxonly.deadliner.model.DDLState
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.HabitGoalType
import com.aritxonly.deadliner.model.HabitMetaData
import com.aritxonly.deadliner.model.HabitPeriod
import com.aritxonly.deadliner.model.toJson
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal data class ToolCallExecution(
    val id: String,
    val toolName: String,
    val argsJson: String,
)

internal data class ToolExecutionResult(
    val resultJson: String,
)

internal interface ToolCallAdapter {
    suspend fun execute(call: ToolCallExecution): ToolExecutionResult
}

internal interface TaskCreator {
    fun createTask(name: String, due: LocalDateTime, note: String): Long
}

internal interface HabitCreator {
    fun createHabit(input: CreateHabitInput, now: LocalDateTime): CreatedHabit
}

internal data class CreatedHabit(
    val id: Long,
    val ddlId: Long,
    val name: String,
    val period: HabitPeriod,
    val timesPerPeriod: Int,
    val goalType: HabitGoalType,
    val totalTarget: Int?,
)

internal data class CreateTaskInput(
    val name: String,
    val due: LocalDateTime,
    val note: String,
)

internal data class CreateHabitInput(
    val name: String,
    val period: HabitPeriod,
    val timesPerPeriod: Int,
    val goalType: HabitGoalType,
    val totalTarget: Int?,
    val description: String?,
)

internal class AndroidToolCallAdapter(
    private val gson: Gson,
    private val taskCreatorFactory: () -> TaskCreator = {
        val repo = DDLRepository()
        object : TaskCreator {
            override fun createTask(name: String, due: LocalDateTime, note: String): Long {
                val start = LocalDateTime.now()
                return repo.insertDDL(
                    name = name,
                    startTime = start.toString(),
                    endTime = due.toString(),
                    note = note,
                    type = DeadlineType.TASK,
                )
            }
        }
    },
    private val habitCreatorFactory: () -> HabitCreator = {
        val ddlRepo = DDLRepository()
        val habitRepo = HabitRepository()
        object : HabitCreator {
            override fun createHabit(input: CreateHabitInput, now: LocalDateTime): CreatedHabit {
                val habitFrequencyType = when {
                    input.goalType == HabitGoalType.TOTAL -> DeadlineFrequency.TOTAL
                    input.period == HabitPeriod.WEEKLY -> DeadlineFrequency.WEEKLY
                    input.period == HabitPeriod.MONTHLY -> DeadlineFrequency.MONTHLY
                    else -> DeadlineFrequency.DAILY
                }
                val meta = HabitMetaData(
                    completedDates = emptySet(),
                    frequencyType = habitFrequencyType,
                    frequency = if (input.goalType == HabitGoalType.TOTAL) 1 else input.timesPerPeriod,
                    total = input.totalTarget ?: 0,
                    refreshDate = LocalDate.now().toString(),
                )
                val ddlId = ddlRepo.insertDDL(
                    name = input.name,
                    startTime = now.toString(),
                    endTime = now.toString(),
                    note = meta.toJson(),
                    type = DeadlineType.HABIT,
                )
                val habitId = habitRepo.createHabitForDdl(
                    ddlId = ddlId,
                    name = input.name,
                    period = input.period,
                    timesPerPeriod = if (input.goalType == HabitGoalType.TOTAL) 1 else input.timesPerPeriod,
                    goalType = input.goalType,
                    totalTarget = if (input.goalType == HabitGoalType.TOTAL) input.totalTarget else null,
                    description = input.description,
                )
                val created = habitRepo.getHabitById(habitId)
                return CreatedHabit(
                    id = habitId,
                    ddlId = ddlId,
                    name = created?.name ?: input.name,
                    period = created?.period ?: input.period,
                    timesPerPeriod = created?.timesPerPeriod ?: input.timesPerPeriod,
                    goalType = created?.goalType ?: input.goalType,
                    totalTarget = created?.totalTarget ?: input.totalTarget,
                )
            }
        }
    },
) : ToolCallAdapter {
    override suspend fun execute(call: ToolCallExecution): ToolExecutionResult {
        val resultJson = when (call.toolName) {
            DeadlinerCoreBridge.TOOL_READ_TASKS -> {
                val args = parseReadTasksArgs(call.argsJson) ?: ReadTasksArgs()
                gson.toJson(executeReadTasks(args))
            }
            DeadlinerCoreBridge.TOOL_CREATE_TASK -> {
                gson.toJson(executeCreateTask(call.argsJson))
            }
            DeadlinerCoreBridge.TOOL_READ_HABITS -> {
                gson.toJson(executeReadHabits(call.argsJson))
            }
            DeadlinerCoreBridge.TOOL_CREATE_HABIT -> {
                gson.toJson(executeCreateHabit(call.argsJson))
            }

            DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR -> gson.toJson(
                mapOf(
                    "ok" to true,
                    "reserved" to true,
                    "status" to "pending_skill",
                    "message" to "addToCalendar will be handled by app skill later",
                )
            )

            else -> gson.toJson(
                mapOf(
                    "ok" to false,
                    "errorCode" to "UNSUPPORTED_TOOL",
                    "message" to "unsupported tool: ${call.toolName}",
                )
            )
        }
        return ToolExecutionResult(resultJson)
    }

    private fun executeCreateTask(argsJson: String): Map<String, Any?> {
        val argsObj = extractArgsObject(argsJson)
        val (itemsNode, isBatchMode) = extractBatchOrSingleItems(argsObj, "tasks")
            ?: return mapOf(
                "ok" to false,
                "errorCode" to "INVALID_ARGUMENT",
                "message" to "create_task requires either non-empty tasks[] or single fields: name, dueTime",
            )
        if (itemsNode.isEmpty()) {
            return mapOf(
                "ok" to false,
                "errorCode" to "INVALID_ARGUMENT",
                "message" to "create_task.tasks cannot be empty",
            )
        }
        val creator = taskCreatorFactory()
        val itemResults = itemsNode.mapIndexed { index, node ->
            val parsed = parseCreateTaskItem(node)
            if (parsed == null) {
                mapOf(
                    "index" to index,
                    "ok" to false,
                    "errorCode" to "INVALID_ARGUMENT",
                    "message" to "task item must be an object",
                )
            } else {
                val (name, dueText, note) = parsed
                when {
                    name.isBlank() -> mapOf(
                        "index" to index,
                        "ok" to false,
                        "errorCode" to "INVALID_ARGUMENT",
                        "message" to "create_task.name is required",
                    )
                    dueText.isBlank() -> mapOf(
                        "index" to index,
                        "ok" to false,
                        "errorCode" to "INVALID_ARGUMENT",
                        "message" to "create_task.dueTime is required",
                    )
                    else -> {
                        val due = parseFlexibleDue(dueText)
                        if (due == null) {
                            mapOf(
                                "index" to index,
                                "ok" to false,
                                "errorCode" to "INVALID_ARGUMENT",
                                "message" to "create_task.dueTime format is invalid",
                            )
                        } else {
                            runCatching {
                                val id = creator.createTask(name = name, due = due, note = note)
                                mapOf(
                                    "index" to index,
                                    "ok" to true,
                                    "task" to mapOf(
                                        "id" to id,
                                        "name" to name,
                                        "dueTime" to due.format(DUE_FMT),
                                        "note" to note,
                                    )
                                )
                            }.getOrElse { e ->
                                mapOf(
                                    "index" to index,
                                    "ok" to false,
                                    "errorCode" to "CREATE_FAILED",
                                    "message" to (e.message ?: "create task failed"),
                                )
                            }
                        }
                    }
                }
            }
        }
        val successItems = itemResults.mapNotNull { if (it["ok"] == true) it["task"] else null }
        val failCount = itemResults.size - successItems.size
        if (!isBatchMode && successItems.isEmpty()) {
            val firstFailure = itemResults.firstOrNull()
            return mapOf(
                "ok" to false,
                "errorCode" to (firstFailure?.get("errorCode") ?: "INVALID_ARGUMENT"),
                "message" to (firstFailure?.get("message") ?: "create_task failed"),
                "items" to itemResults,
                "summary" to mapOf("total" to itemResults.size, "successCount" to 0, "failureCount" to itemResults.size),
            )
        }
        return mapOf(
            "ok" to (failCount == 0),
            "partialSuccess" to (successItems.isNotEmpty() && failCount > 0),
            "task" to successItems.firstOrNull(),
            "createdTasks" to successItems,
            "items" to itemResults,
            "summary" to mapOf(
                "total" to itemResults.size,
                "successCount" to successItems.size,
                "failureCount" to failCount,
            ),
        )
    }

    private fun executeReadHabits(argsJson: String): Map<String, Any> {
        val habitRepo = HabitRepository()
        val obj = runCatching { gson.fromJson(argsJson, JsonObject::class.java) }.getOrNull()
        val limit = runCatching {
            if (obj?.has("limit") == true) obj.get("limit").asInt
            else if (obj?.has("args") == true && obj.getAsJsonObject("args").has("limit")) obj.getAsJsonObject("args").get("limit").asInt
            else 20
        }.getOrDefault(20).coerceIn(1, 100)
        val keyword = runCatching {
            if (obj?.has("keyword") == true) obj.get("keyword").asString
            else if (obj?.has("args") == true && obj.getAsJsonObject("args").has("keyword")) obj.getAsJsonObject("args").get("keyword").asString
            else ""
        }.getOrDefault("").trim()

        val habits = habitRepo.getAllHabits()
            .asSequence()
            .filter { it.status.name == "ACTIVE" }
            .filter { keyword.isBlank() || it.name.contains(keyword, ignoreCase = true) }
            .take(limit)
            .map {
                mapOf(
                    "id" to it.id,
                    "ddlId" to it.ddlId,
                    "name" to it.name,
                    "period" to it.period.name.lowercase(),
                    "timesPerPeriod" to it.timesPerPeriod,
                    "goalType" to if (it.goalType == HabitGoalType.TOTAL) "total" else "frequency",
                    "totalTarget" to it.totalTarget,
                )
            }.toList()

        return mapOf(
            "habits" to habits,
            "summary" to mapOf("count" to habits.size),
        )
    }

    private fun executeCreateHabit(argsJson: String): Map<String, Any?> {
        val argsObj = extractArgsObject(argsJson)
        val (itemsNode, isBatchMode) = extractBatchOrSingleItems(argsObj, "habits")
            ?: return mapOf(
                "ok" to false,
                "errorCode" to "INVALID_ARGUMENT",
                "message" to "create_habit requires either non-empty habits[] or single fields: name, period, timesPerPeriod, goalType",
            )
        if (itemsNode.isEmpty()) {
            return mapOf(
                "ok" to false,
                "errorCode" to "INVALID_ARGUMENT",
                "message" to "create_habit.habits cannot be empty",
            )
        }
        val creator = habitCreatorFactory()
        val now = LocalDateTime.now()
        val itemResults = itemsNode.mapIndexed { index, node ->
            val parsed = parseCreateHabitItem(node)
            if (parsed == null) {
                mapOf(
                    "index" to index,
                    "ok" to false,
                    "errorCode" to "INVALID_ARGUMENT",
                    "message" to "habit item must be an object",
                )
            } else {
                val validationErr = validateCreateHabitInput(parsed)
                if (validationErr != null) {
                    mapOf(
                        "index" to index,
                        "ok" to false,
                        "errorCode" to "INVALID_ARGUMENT",
                        "message" to validationErr,
                    )
                } else {
                    runCatching {
                        val created = creator.createHabit(parsed, now)
                        mapOf(
                            "index" to index,
                            "ok" to true,
                            "habit" to mapOf(
                                "id" to created.id,
                                "ddlId" to created.ddlId,
                                "name" to created.name,
                                "period" to created.period.name.lowercase(),
                                "timesPerPeriod" to created.timesPerPeriod,
                                "goalType" to if (created.goalType == HabitGoalType.TOTAL) "total" else "frequency",
                                "totalTarget" to created.totalTarget,
                            ),
                        )
                    }.getOrElse { e ->
                        mapOf(
                            "index" to index,
                            "ok" to false,
                            "errorCode" to "CREATE_FAILED",
                            "message" to (e.message ?: "create habit failed"),
                        )
                    }
                }
            }
        }
        val successItems = itemResults.mapNotNull { if (it["ok"] == true) it["habit"] else null }
        val failCount = itemResults.size - successItems.size
        if (!isBatchMode && successItems.isEmpty()) {
            val firstFailure = itemResults.firstOrNull()
            return mapOf(
                "ok" to false,
                "errorCode" to (firstFailure?.get("errorCode") ?: "INVALID_ARGUMENT"),
                "message" to (firstFailure?.get("message") ?: "create_habit failed"),
                "items" to itemResults,
                "summary" to mapOf("total" to itemResults.size, "successCount" to 0, "failureCount" to itemResults.size),
            )
        }
        return mapOf(
            "ok" to (failCount == 0),
            "partialSuccess" to (successItems.isNotEmpty() && failCount > 0),
            "habit" to successItems.firstOrNull(),
            "createdHabits" to successItems,
            "items" to itemResults,
            "summary" to mapOf(
                "total" to itemResults.size,
                "successCount" to successItems.size,
                "failureCount" to failCount,
            ),
        )
    }

    private fun extractArgsObject(argsJson: String): JsonObject? {
        val obj = runCatching { gson.fromJson(argsJson, JsonObject::class.java) }.getOrNull() ?: return null
        if (obj.has("args") && obj.get("args").isJsonObject) {
            return obj.getAsJsonObject("args")
        }
        return obj
    }

    private fun toHabitPeriod(raw: String): HabitPeriod {
        return when (raw.trim().lowercase()) {
            "day", "daily" -> HabitPeriod.DAILY
            "week", "weekly" -> HabitPeriod.WEEKLY
            "month", "monthly" -> HabitPeriod.MONTHLY
            else -> HabitPeriod.DAILY
        }
    }

    private fun toHabitGoalType(raw: String): HabitGoalType {
        return when (raw.trim().lowercase()) {
            "total" -> HabitGoalType.TOTAL
            "frequency", "completion", "per_period", "perperiod" -> HabitGoalType.PER_PERIOD
            else -> HabitGoalType.PER_PERIOD
        }
    }

    private fun parseReadTasksArgs(argsJson: String): ReadTasksArgs? {
        val obj = runCatching { gson.fromJson(argsJson, JsonObject::class.java) }.getOrNull()
        return runCatching { gson.fromJson(argsJson, ReadTasksArgs::class.java) }.getOrNull()
            ?: runCatching {
                if (obj?.has("args") == true && obj.get("args").isJsonObject) {
                    gson.fromJson(obj.getAsJsonObject("args"), ReadTasksArgs::class.java)
                } else null
            }.getOrNull()
    }

    private fun executeReadTasks(rawArgs: ReadTasksArgs): ReadTasksResultPayload {
        val args = normalizeReadTasksArgs(rawArgs)
        val now = LocalDateTime.now()
        val dueEnd = now.plusDays((args.timeRangeDays ?: DEFAULT_RANGE_DAYS).toLong())

        val items = DDLRepository()
            .getAllDDLs()
            .asSequence()
            .filter { it.type == DeadlineType.TASK }
            .filter { !it.isArchived && it.state != DDLState.ARCHIVED && it.state != DDLState.ABANDONED_ARCHIVED }
            .filter { item ->
                when ((args.status ?: "OPEN").uppercase()) {
                    "DONE" -> item.state == DDLState.COMPLETED
                    "ALL" -> true
                    else -> item.state == DDLState.ACTIVE || item.state == DDLState.ABANDONED
                }
            }
            .filter { item ->
                val kws = args.keywords.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
                if (kws.isEmpty()) return@filter true
                val hay = (item.name + "\n" + item.note).lowercase()
                kws.any { hay.contains(it.lowercase()) }
            }
            .map { item ->
                val due = parseDue(item.endTime)
                item to due
            }
            .filter { (_, due) ->
                val days = args.timeRangeDays ?: DEFAULT_RANGE_DAYS
                if (days <= 0) return@filter true
                if (due == null) return@filter true
                due <= dueEnd
            }
            .sortedWith(
                when ((args.sort ?: "DUE_ASC").uppercase()) {
                    "UPDATED_DESC" -> compareByDescending<Pair<com.aritxonly.deadliner.model.DDLItem, LocalDateTime?>> { it.first.timeStamp }
                    else -> compareBy<Pair<com.aritxonly.deadliner.model.DDLItem, LocalDateTime?>> { it.second ?: LocalDateTime.MAX }
                }
            )
            .take((args.limit ?: DEFAULT_LIMIT).coerceAtMost(MAX_LIMIT))
            .toList()

        val digests = items.map { (item, due) ->
            TaskDigestItem(
                id = item.id,
                name = item.name,
                due = due?.format(DUE_FMT) ?: "",
                status = if (item.state == DDLState.COMPLETED) "DONE" else "OPEN",
                notePreview = item.note.trim().replace("\n", " ").take(40)
            )
        }

        val overdue = items.count { (item, due) ->
            due != null && due.isBefore(now) && item.state != DDLState.COMPLETED
        }
        val dueSoon24h = items.count { (item, due) ->
            due != null && !due.isBefore(now) && due.isBefore(now.plusHours(24)) && item.state != DDLState.COMPLETED
        }

        return ReadTasksResultPayload(
            tasks = digests,
            summary = TaskSummary(
                count = digests.size,
                overdue = overdue,
                dueSoon24h = dueSoon24h,
            )
        )
    }

    private fun normalizeReadTasksArgs(args: ReadTasksArgs): ReadTasksArgs {
        val limit = (args.limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        val days = (args.timeRangeDays ?: DEFAULT_RANGE_DAYS).coerceIn(0, 365)
        val status = (args.status ?: "OPEN").uppercase().let {
            if (it in setOf("OPEN", "DONE", "ALL")) it else "OPEN"
        }
        val sort = (args.sort ?: "DUE_ASC").uppercase().let {
            if (it in setOf("DUE_ASC", "UPDATED_DESC")) it else "DUE_ASC"
        }
        val keywords = args.keywords.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.take(3)

        return ReadTasksArgs(
            timeRangeDays = days,
            status = status,
            keywords = keywords,
            limit = limit,
            sort = sort,
        )
    }

    private fun parseDue(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        return runCatching { LocalDateTime.parse(text) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(text, DUE_FMT) }.getOrNull()
    }

    private fun parseFlexibleDue(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        return runCatching { LocalDateTime.parse(text) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(text, DUE_FMT) }.getOrNull()
            ?: runCatching { LocalDate.parse(text).atTime(20, 0) }.getOrNull()
    }

    private fun extractBatchOrSingleItems(
        argsObj: JsonObject?,
        batchField: String,
    ): Pair<List<JsonElement>, Boolean>? {
        if (argsObj == null) return null
        if (argsObj.has(batchField)) {
            val array = runCatching { argsObj.getAsJsonArray(batchField) }.getOrNull() ?: JsonArray()
            return array.toList() to true
        }
        val hasSingleFields = argsObj.entrySet().any { it.key != "args" }
        return if (hasSingleFields) listOf(argsObj) to false else null
    }

    private fun parseCreateTaskItem(node: JsonElement): Triple<String, String, String>? {
        if (!node.isJsonObject) return null
        val obj = node.asJsonObject
        val name = obj.getAsStringOrEmpty("name").trim()
        val dueText = obj.getAsStringOrEmpty("dueTime").trim()
        val note = obj.getAsStringOrEmpty("note")
        return Triple(name, dueText, note)
    }

    private fun parseCreateHabitItem(node: JsonElement): CreateHabitInput? {
        if (!node.isJsonObject) return null
        val obj = node.asJsonObject
        val name = obj.getAsStringOrEmpty("name").trim()
        val period = toHabitPeriod(obj.getAsStringOrEmpty("period"))
        val timesPerPeriod = runCatching { obj.get("timesPerPeriod")?.asInt ?: 1 }.getOrDefault(1).coerceAtLeast(1)
        val goalType = toHabitGoalType(obj.getAsStringOrEmpty("goalType"))
        val totalTarget = runCatching {
            if (obj.has("totalTarget") && !obj.get("totalTarget").isJsonNull) obj.get("totalTarget").asInt else null
        }.getOrNull()
        val description = runCatching {
            if (obj.has("description") && !obj.get("description").isJsonNull) obj.get("description").asString else null
        }.getOrNull()
        return CreateHabitInput(
            name = name,
            period = period,
            timesPerPeriod = timesPerPeriod,
            goalType = goalType,
            totalTarget = totalTarget,
            description = description,
        )
    }

    private fun validateCreateHabitInput(input: CreateHabitInput): String? {
        if (input.name.isBlank()) return "create_habit.name is required"
        if (input.timesPerPeriod <= 0) return "create_habit.timesPerPeriod must be > 0"
        if (input.goalType == HabitGoalType.TOTAL && (input.totalTarget == null || input.totalTarget <= 0)) {
            return "create_habit.totalTarget is required when goalType is TOTAL"
        }
        return null
    }

    private fun JsonObject.getAsStringOrEmpty(field: String): String {
        return runCatching {
            if (has(field) && !get(field).isJsonNull) get(field).asString else ""
        }.getOrDefault("")
    }

    private companion object {
        private const val DEFAULT_RANGE_DAYS = 7
        private const val DEFAULT_LIMIT = 20
        private const val MAX_LIMIT = 50
        private val DUE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
