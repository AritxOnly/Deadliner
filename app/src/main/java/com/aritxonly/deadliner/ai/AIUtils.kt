package com.aritxonly.deadliner.ai

import android.content.Context
import android.util.Log
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.ApiKeystore
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

    /** 初始化：在 Application 或首次使用时调用 */
    fun init(context: Context) {
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
    }

    fun setPreset(preset0: LlmPreset, context: Context) {
        val bp = toBackendPreset(preset0)
        model = bp.model

        val bearerKey = ApiKeystore.retrieveAndDecrypt(context).orEmpty()
        val appSecret = GlobalUtils.getDeadlinerAppSecret(context).orEmpty()
        val deviceId = GlobalUtils.getOrCreateDeviceId(context)

        transport = LlmTransportFactory.create(bp, bearerKey, appSecret, deviceId)
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
你是 Deadliner AI。仅输出**纯 JSON**，不允许多余文字/注释/代码块。
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

    suspend fun generateDeadline(context: Context, rawText: String): String =
        generateMixed(context, rawText, candidatePrimary = IntentType.ExtractTasks)

    suspend fun generateMixed(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        candidatePrimary: IntentType? = null
    ): String = withContext(Dispatchers.IO) {
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
        extractJsonFromMarkdown(raw)
    }

    /**
     * 自动意图识别 + 调用 generateByIntent()
     * @param preferLLM 当启发式置信度 < 0.75 时，自动用 LLM 复核；置 false 则只用本地启发式
     */
    suspend fun generateAuto(
        context: Context,
        rawText: String,
        profile: UserProfile? = null,
        preferLLM: Boolean = true
    ): Pair<IntentGuess, String> {
        // 1) 本地启发式
        val h = IntentClassifier.heuristicClassify(rawText)

        val guess = if (preferLLM && h.confidence < 0.75) {
            // 2) 低置信 → LLM 覆核
            try {
                val g = IntentClassifier.llmClassifyIntent(context, rawText)
                // 若 LLM 与启发式一致且置信度更高，采用 LLM；否则选择更可信的
                if (g.intent == h.intent && g.confidence >= h.confidence) g
                else if (g.confidence >= 0.8) g else h.copy(reason = h.reason + " | llm_disagree:$g")
            } catch (t: Throwable) {
                // 网络/权限失败：回退启发式
                h.copy(reason = h.reason + " | llm_fallback:${t::class.simpleName}")
            }
        } else h

        // 3) 进入既有管线
        val json = generateMixed(context, rawText, profile)
        return guess to json
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

    object IntentClassifier {
        fun heuristicClassify(raw: String): IntentGuess {
            val text = raw.lowercase().trim()

            // --- 关键词覆盖（中英混合） ---
            val planHit = listOf(
                "日程", "安排", "计划", "规划", "时间块", "番茄", "今天", "明天", "这周", "本周",
                "下午", "晚上", "早上", "上午", "晚上学习", "复习两小时",
                "schedule", "plan my day", "time block", "timeline", "today", "tomorrow"
            ).count { text.contains(it) }

            val splitHit = listOf(
                "拆解", "步骤", "清单", "子任务", "分解", "workflow", "checklist", "steps", "break down"
            ).count { text.contains(it) }

            val extractHit = listOf(
                "ddl", "截止", "截止时间", "提醒", "任务", "todo", "待办", "到期",
                "提交", "完成", "安排提交", "deadline", "due", "remind", "tag"
            ).count { text.contains(it) }

            // --- 强特征正则 ---
            val timeLike = Regex("""\b(\d{1,2}:\d{2})\b|今天|明天|后天|本周|下周|周[一二三四五六日天]""")
                .containsMatchIn(text)
            val imperative = Regex("""^(安排|规划|计划|请|帮我|plan|schedule|make|create)\b""")
                .containsMatchIn(text)

            // --- 简单打分 ---
            var planScore = planHit * 1.0 + (if (timeLike) 0.5 else 0.0) + (if (imperative) 0.2 else 0.0)
            var splitScore = splitHit * 1.0 + (if (text.contains("步骤") || text.contains("checklist")) 0.3 else 0.0)
            var extractScore = extractHit * 1.0 + (if (text.contains("截止") || text.contains("ddl")) 0.4 else 0.0)

            // 避免全零
            if (planScore == 0.0 && splitScore == 0.0 && extractScore == 0.0) {
                // 兜底倾向任务抽取
                extractScore = 0.2
            }

            val scores = mapOf(
                IntentType.PlanDay to planScore,
                IntentType.SplitToSteps to splitScore,
                IntentType.ExtractTasks to extractScore
            )
            val (bestIntent, bestScore) = scores.maxBy { it.value }

            // 计算相对置信度（与次优拉开）
            val sorted = scores.values.sortedDescending()
            val margin = if (sorted.size >= 2) (sorted[0] - sorted[1]).coerceAtLeast(0.0) else sorted[0]
            val confidence = (0.55 + margin / 5.0).coerceIn(0.0, 0.95) // 0.55 起步，留点空间给 LLM 覆核

            return IntentGuess(bestIntent, confidence, reason = "heuristic: $scores, margin=$margin")
        }

        suspend fun llmClassifyIntent(
            context: Context,
            rawText: String,
        ): IntentGuess {
            val langTag = currentLangTag(context)

            val prompt = """
你是一个分类器。仅输出**纯 JSON**，不要代码块，不要多余文字。
在以下三类中选择最合适的一类，输出 {"intent":"ExtractTasks|PlanDay|SplitToSteps"}。

- ExtractTasks：用户在描述待办/DDL/提醒，期望抽取一个或多个任务、截止时间、提醒等结构化数据。
- PlanDay：用户希望把当天/某段时间安排成多个日程块（含开始/结束时间），进行时间规划或学习/工作安排。
- SplitToSteps：用户给出一个目标任务，希望拆解成可执行的步骤清单（checklist）。

用户输入（$langTag）：
$rawText
""".trimIndent()

            val messages = listOf(
                Message("system", prompt),
                Message("user", """请仅输出：{"intent":"..."}""")
            )

            val raw = sendPrompt(messages)
            val json = extractJsonFromMarkdown(raw)

            // 极简解析（无需引入新 DTO）
            val m = Regex(""""intent"\s*:\s*"([A-Za-z]+)"""").find(json)
            val intentStr = m?.groupValues?.get(1) ?: "ExtractTasks"
            val intent = when (intentStr) {
                "PlanDay" -> IntentType.PlanDay
                "SplitToSteps" -> IntentType.SplitToSteps
                else -> IntentType.ExtractTasks
            }
            return IntentGuess(intent, confidence = 0.85, reason = "llm")
        }
    }
}