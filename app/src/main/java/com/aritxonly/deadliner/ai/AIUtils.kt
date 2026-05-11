package com.aritxonly.deadliner.ai

import android.content.Context
import android.util.Log
import com.aritxonly.deadliner.localutils.ApiKeystore
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.ui.overview.MonthlyAnalysisResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AIUtils {
    private var model: String = "deepseek-chat"
    private var transport: LlmTransport? = null
    private var appContext: Context? = null
    private var coreBridge: DeadlinerCoreBridge? = null
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }

    /** 初始化：在 Application 或首次使用时调用 */
    fun init(context: Context) {
        appContext = context.applicationContext
        val config = GlobalUtils.getDeadlinerAIConfig()
        val preset0 = config.getCurrentPreset()?: defaultLlmPreset
        val deviceId = GlobalUtils.getOrCreateDeviceId(context)

        val bearerKey = try {
            ApiKeystore.retrieveAndDecrypt(context).orEmpty()
        } catch (t: Throwable) {
            Log.e("AIUtils", "decrypt failed, fallback empty", t)
            ApiKeystore.reset(context)
            ""
        }
        val appSecret = GlobalUtils.getDeadlinerAppSecret(context)

        val preset = toBackendPreset(preset0)

        model = preset.model
        transport = LlmTransportFactory.create(
            preset = preset,
            bearerKey = bearerKey,
            appSecret = appSecret,
            deviceId = deviceId
        )
        coreBridge = DeadlinerCoreBridge(context.applicationContext, gson)
    }

    fun setPreset(preset0: LlmPreset, context: Context) {
        val bp = toBackendPreset(preset0)
        model = bp.model

        val bearerKey = ApiKeystore.retrieveAndDecrypt(context).orEmpty()
        val appSecret = GlobalUtils.getDeadlinerAppSecret(context).orEmpty()
        val deviceId = GlobalUtils.getOrCreateDeviceId(context)

        transport = LlmTransportFactory.create(bp, bearerKey, appSecret, deviceId)
        coreBridge = DeadlinerCoreBridge(context.applicationContext, gson)
    }

    /**
     * 发送一次无状态的 Prompt 请求。
     */
    private suspend fun sendPrompt(messages: List<Message>): String = withContext(Dispatchers.IO) {
        val t = transport ?: error("AIUtils not initialized")
        val req = ChatRequest(model = model, messages = messages, stream = false)
        val resp = t.chat(req)
        resp.choices.firstOrNull()?.message?.content ?: error("API 没有返回消息")
    }

    private fun buildMixedPrompt(
        langTag: String,
        tzId: String,
        nowLocal: String,
        timeFormatSpec: String,
        profile: UserProfile?,
        candidatePrimary: IntentType?,
        withExample: Boolean = false
    ): String {
        val base = """
你是 Lifi AI。仅输出**纯 JSON**，不允许多余文字/注释/代码块。
所有时间必须使用 $timeFormatSpec（24小时制、零填充、不带时区），相对时间需基于 $tzId、当前 $nowLocal 解析为**具体时间**。
name/note 使用设备语言（当前：$langTag）。若出现“晚上”等模糊表达，默认 ${profile?.defaultEveningHour ?: 20}:00。
若无提醒偏好，默认 reminders=${profile?.defaultReminderMinutes ?: listOf(30)}（分钟）。
输出**固定键**：primaryIntent, tasks, planBlocks, steps。若某类为空，给空数组。
primaryIntent 只能为 "ExtractTasks" | "PlanDay" | "SplitToSteps"。${candidatePrimary?.let { "可优先考虑将 primaryIntent 设为 \"$it\"。" } ?: ""}
""".trimIndent()

        val schemaSkeleton = """
只允许如下 JSON 结构（Skeleton）：
{
  "primaryIntent": "ExtractTasks|PlanDay|SplitToSteps",
  "tasks": [
    {
      "name": "string(≤16)",
      "dueTime": "$timeFormatSpec",
      "note": "string"
    }
  ],
  "planBlocks": [
    {
      "title": "string",
      "start": "$timeFormatSpec",
      "end": "$timeFormatSpec",
      "location": "string",
      "energy": "low|med|high",
      "linkTask": "可关联任务名"
    }
  ],
  "steps": [
    {
      "title": "string",
      "checklist": ["步骤1","步骤2","步骤3"]
    }
  ]
}
""".trimIndent()

        val fewshotMinimal = """
示例（可同时包含三类）：
输入：明天晚8点前交系统论作业；今晚安排两小时复习；并把复习拆成步骤。
输出：
{
  "primaryIntent": "PlanDay",
  "tasks": [
    {"name":"提交系统论作业","dueTime":"${LocalDate.now().plusDays(1)} 20:00","note":""}
  ],
  "planBlocks": [
    {"title":"晚间复习","start":"${LocalDate.now()} 20:00","end":"${LocalDate.now()} 22:00","energy":"med"}
  ],
  "steps": [
    {"title":"复习流程","checklist":["过一遍讲义","做5题","整理错题"]}
  ]
}
""".trimIndent()

        return buildString {
            append(base).append("\n\n")
            append(schemaSkeleton)
            if (withExample) {
                append("\n\n")
                append(fewshotMinimal)
            }
        }.trim()
    }

    // Legacy chain is kept only for AddDDLActivity (structured extraction to local cards).
    suspend fun generateMixed(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        candidatePrimary: IntentType? = null
    ): String = withContext(Dispatchers.IO) {
        if (candidatePrimary != IntentType.PlanDay && candidatePrimary != IntentType.SplitToSteps) {
            val coreJson = runCatching {
                val mixed = processByCore(context, rawText)
                gson.toJson(withReservedCalendarToolCall(mixed))
            }.getOrNull()
            if (!coreJson.isNullOrBlank()) return@withContext coreJson
        }

        val langTag = profile?.preferredLang ?: currentLangTag(context)
        val timeFormatSpec = "yyyy-MM-dd HH:mm"
        val tzId = java.util.TimeZone.getDefault().id
        val nowLocal = LocalDateTime.now().toString()
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val systemPrompt = buildMixedPrompt(
            langTag = langTag,
            tzId = tzId,
            nowLocal = nowLocal,
            timeFormatSpec = timeFormatSpec,
            profile = profile,
            candidatePrimary = candidatePrimary
        )
        val localeHint = "当前日期：$today；设备语言：$langTag；时区：$tzId。严格按上述固定键输出 JSON。"

        val messages = listOfNotNull(
            Message("system", systemPrompt),
            Message("user", localeHint),
            GlobalUtils.customPrompt?.let { Message("user", it) },
            Message("user", rawText)
        )

        val raw = sendPrompt(messages)
        val mixed = parseMixedResult(extractJsonFromMarkdown(raw), gson)
        gson.toJson(withReservedCalendarToolCall(mixed))
    }

    /**
     * 自动意图识别 + 调用 generateByIntent()
     * @param preferLLM 当启发式置信度 < 0.75 时，自动用 LLM 复核；置 false 则只用本地启发式
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun generateAuto(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        preferLLM: Boolean = true
    ): Pair<IntentGuess, String> {
        val mixed = processByCore(context, rawText)
        val intent = mixed.primaryIntent.toIntentTypeOrDefault(IntentType.ExtractTasks)
        val guess = IntentGuess(
            intent = intent,
            confidence = 1.0,
            reason = "core_only"
        )
        return guess to gson.toJson(withReservedCalendarToolCall(mixed))
    }

    suspend fun generateMonthlyAnalysis(
        monthKey: String,
        monthName: String,
        metricsSummary: String,
        completedTaskNames: List<String>
    ): MonthlyAnalysisResult = withContext(Dispatchers.IO) {
        val systemPrompt = """
你是 Deadliner 的月度复盘助手。请基于用户上个月的任务统计输出简短、具体、有行动建议的总结。
仅输出纯 JSON，不允许 Markdown、代码块或额外解释。
JSON 结构固定为：
{
  "summary": "string",
  "keywords": ["string", "string"]
}

要求：
1. `summary` 使用当前输入语言，长度控制在 90-180 字。
2. 总结要覆盖完成情况、风险或拖延信号、一个积极亮点，以及一个下月建议。
3. `keywords` 返回 4-6 个简短关键词，每个关键词不超过 8 个汉字或 3 个英文单词。
4. 如果已完成任务名较少，也不要编造不存在的任务内容。
        """.trimIndent()

        val userPrompt = buildString {
            appendLine("月份：$monthName")
            appendLine("指标：")
            appendLine(metricsSummary.ifBlank { "暂无指标" })
            appendLine()
            appendLine("已完成任务：")
            if (completedTaskNames.isEmpty()) {
                append("无")
            } else {
                append(completedTaskNames.take(50).joinToString(separator = "、"))
            }
        }

        val raw = sendPrompt(
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt),
            )
        )
        data class MonthlyAnalysisPayload(
            val summary: String = "",
            val keywords: List<String> = emptyList(),
        )

        val payload = gson.fromJson(
            extractJsonFromMarkdown(raw),
            MonthlyAnalysisPayload::class.java
        )

        MonthlyAnalysisResult(
            month = monthKey,
            summary = payload.summary.trim(),
            keywords = payload.keywords
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(6),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    internal suspend fun generateAutoInteractive(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        preferLLM: Boolean = true,
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)? = null,
        onTextStream: (suspend (String) -> Unit)? = null,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)? = null,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)? = null,
    ): Pair<IntentGuess, String> {
        val mixed = processByCore(
            context = context,
            rawText = rawText,
            onToolRequest = onToolRequest,
            onTextStream = onTextStream,
            onThinking = onThinking,
            onLifecycle = onLifecycle,
        )
        val intent = mixed.primaryIntent.toIntentTypeOrDefault(IntentType.ExtractTasks)
        val guess = IntentGuess(
            intent = intent,
            confidence = 1.0,
            reason = "core_interactive_only"
        )
        return guess to gson.toJson(withReservedCalendarToolCall(mixed))
    }

    fun extractJsonFromMarkdown(raw: String): String {
        val idx = raw.indexOf('{')
        if (idx >= 0) {
            var depth = 0
            for (i in idx until raw.length) {
                val c = raw[i]
                if (c == '{') depth++
                if (c == '}') {
                    depth--
                    if (depth == 0) return raw.substring(idx, i + 1).trim()
                }
            }
        }

        val jsonFenceRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        jsonFenceRegex.find(raw)?.let { return it.groups[1]!!.value.trim() }
        val anyFenceRegex = Regex("```\\s*([\\s\\S]*?)```")
        anyFenceRegex.find(raw)?.let { return it.groups[1]!!.value.trim() }

        return raw.trim()
    }

    fun parseMixedResult(
        json: String,
        gson: Gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    ): MixedResult = gson.fromJson(json, MixedResult::class.java)

    fun parseAIResult(intent: IntentType, json: String, gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
    ): AIResult = when (intent) {
        IntentType.ExtractTasks -> {
            val r = gson.fromJson(json, ExtractTasksResult::class.java)
            AIResult.ExtractTasks(r)
        }
        IntentType.PlanDay -> {
            data class PlanResp(val blocks: List<PlanBlock>)
            val r = gson.fromJson(json, PlanResp::class.java)
            AIResult.PlanDay(r.blocks)
        }
        IntentType.SplitToSteps -> {
            val r = gson.fromJson(json, SplitStepsResult::class.java)
            AIResult.SplitToSteps(r)
        }
    }

    private fun currentLangTag(context: Context): String {
        val loc = context.resources.configuration.locales[0]
        return loc.toLanguageTag() // e.g., "zh-CN" / "en-US"
    }

    private fun toBackendPreset(p: LlmPreset): BackendPreset {
        // 约定：如果 endpoint 以 "/api" 结尾，认为是 Deadliner 代理；否则是直连
        return if (p.endpoint.contains("aritxonly.top/api")) {
            BackendPreset(
                type = BackendType.DeadlinerProxy,
                endpoint = p.endpoint.trimEnd('/'),
                model = p.model
            )
        } else {
            BackendPreset(
                type = BackendType.DirectBearer,
                endpoint = p.endpoint,
                model = p.model
            )
        }
    }

    private suspend fun processByCore(
        context: Context,
        rawText: String,
        onToolRequest: (suspend (AIToolRequest) -> ToolDecision)? = null,
        onTextStream: (suspend (String) -> Unit)? = null,
        onThinking: (suspend (agent: String, phase: String, message: String?) -> Unit)? = null,
        onLifecycle: (suspend (AgentLifecycleEvent) -> Unit)? = null,
    ): MixedResult {
        val bridge = coreBridge ?: DeadlinerCoreBridge(context.applicationContext, gson).also {
            coreBridge = it
        }
        val config = readCoreRuntimeConfig(context)
        val strictToolArgHint = """
            [Adapter Contract]
            Tool-call arguments must strictly match schema.
            Never use null for integer fields.
            If an optional integer is unknown, omit that field.
            For create_habit.totalTarget: provide an integer only when goalType indicates total target; otherwise omit totalTarget.
        """.trimIndent()
        val composedInput = "$rawText\n\n$strictToolArgHint"
        val output = bridge.processInput(
            text = composedInput,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            modelId = config.modelId,
            platform = "android",
            onToolRequest = onToolRequest,
            onTextStream = onTextStream,
            onThinking = onThinking,
            onLifecycle = onLifecycle,
        )
        GlobalUtils.incrementLifiModelUsageCount()
        return output.mixed
    }

    private fun withReservedCalendarToolCall(mixed: MixedResult): MixedResult {
        val firstPlan = mixed.planBlocks.firstOrNull() ?: return mixed
        val existing = mixed.toolCalls.any { it.tool == DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR }
        if (existing) return mixed

        val reserved = AIToolCall(
            tool = DeadlinerCoreBridge.TOOL_ADD_TO_CALENDAR,
            addToCalendarArgs = AddToCalendarArgs(
                title = firstPlan.title,
                start = firstPlan.start,
                end = firstPlan.end,
                location = firstPlan.location,
                description = firstPlan.linkTask?.let { "关联任务: $it" },
            ),
            reason = "保留为后续 Calendar Skill 执行入口",
        )

        return mixed.copy(toolCalls = mixed.toolCalls + reserved)
    }

    private fun readCoreRuntimeConfig(context: Context): CoreRuntimeConfig {
        val apiKey = ApiKeystore.retrieveAndDecrypt(context).orEmpty()
        val preset = GlobalUtils.getDeadlinerAIConfig().getCurrentPreset() ?: defaultLlmPreset
        val baseUrl = normalizeCoreBaseUrl(preset.endpoint)
        if (apiKey.isBlank()) {
            Log.w("AIUtils", "core runtime api key is empty")
        }
        Log.d(
            "AIUtils",
            "core runtime config: endpoint=${preset.endpoint}, normalizedBaseUrl=$baseUrl, model=${preset.model}"
        )
        return CoreRuntimeConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = preset.model,
        )
    }

    private fun normalizeCoreBaseUrl(endpoint: String): String {
        var e = endpoint.trim().trimEnd('/')
        // Core expects provider-compatible base URL (e.g. .../v1), not app proxy endpoints.
        if (e.contains("deadliner.aritxonly.top/api")) {
            return "https://api.deepseek.com/v1"
        }
        if (e.endsWith("/api")) {
            return "https://api.deepseek.com/v1"
        }
        if (e.endsWith("/chat/completions")) {
            e = e.removeSuffix("/chat/completions")
        }
        if (!e.startsWith("http://") && !e.startsWith("https://")) {
            e = "https://$e"
        }
        return e.trimEnd('/')
    }

    private data class CoreRuntimeConfig(
        val apiKey: String,
        val baseUrl: String,
        val modelId: String,
    )
}
