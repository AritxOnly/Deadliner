package com.aritxonly.deadliner.localutils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.aritxonly.deadliner.model.LlmPreset
import com.google.gson.Gson
import androidx.core.content.edit
import com.aritxonly.deadliner.R

class DeadlinerAIConfig(private val sp: SharedPreferences) {

    private val gson = Gson()
    private val KEY_PRESETS = "llm_presets_v1"
    private val KEY_CURRENT_ID = "llm_current_id"

    private val KEY_CURRENT_LOGO = "llm_current_logo"

    /** 读取全部预设（无则给默认） */
    fun getPresets(): List<LlmPreset> {
        val json = sp.getString(KEY_PRESETS, null)
        if (json.isNullOrBlank()) return defaultPresets().also { savePresets(it) }
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, LlmPreset::class.java).type
            gson.fromJson<List<LlmPreset>>(json, type) ?: defaultPresets().also { savePresets(it) }
        } catch (_: Exception) {
            defaultPresets().also { savePresets(it) }
        }
    }

    /** 覆盖保存预设列表 */
    fun savePresets(list: List<LlmPreset>) {
        val json = gson.toJson(list)
        sp.edit { putString(KEY_PRESETS, json) }
        // 确保当前选中仍然存在；否则重置为第一个
        val cur = getCurrentPresetId()
        if (cur == null || list.none { it.id == cur }) {
            setCurrentPresetId(list.firstOrNull()?.id)
        }
    }

    /** 获取当前选中预设 id */
    fun getCurrentPresetId(): String? =
        sp.getString(KEY_CURRENT_ID, null)

    /** 设置当前选中预设 id（可传 null 清空） */
    fun setCurrentPresetId(id: String?) {
        sp.edit { putString(KEY_CURRENT_ID, id) }
    }

    /** 获取当前选中的完整预设 */
    fun getCurrentPreset(): LlmPreset? {
        val id = getCurrentPresetId() ?: return getPresets().firstOrNull()
        return getPresets().firstOrNull { it.id == id } ?: getPresets().firstOrNull()
    }

    /** 新增或更新一个预设（按 id upsert） */
    fun upsertPreset(p: LlmPreset) {
        val list = getPresets().toMutableList()
        val idx = list.indexOfFirst { it.id == p.id }
        if (idx >= 0) list[idx] = p else list += p
        savePresets(list)
    }

    /** 删除一个预设 */
    fun deletePreset(id: String) {
        val list = getPresets().filterNot { it.id == id }
        savePresets(list)
    }

    /** 给 UI 用的默认预设（可按需增减） */
    private fun defaultPresets(): List<LlmPreset> {
        return listOf(
            LlmPreset(
                id = "deepseek_official",
                name = "DeepSeek",
                model = "deepseek-chat",
                endpoint = "https://api.deepseek.com/v1/chat/completions"
            ),
            LlmPreset(
                id = "gpt_official",
                name = "ChatGPT",
                model = "gpt-5",
                endpoint = "https://api.openai.com/v1/chat/completions"
            ),
            LlmPreset(
                id = "qwen_flash_official",
                name = "Qwen Flash",
                model = "qwen-flash",
                endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
            ),
            LlmPreset(
                id = "qwen_plus_official",
                name = "Qwen Plus",
                model = "qwen-plus",
                endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
            )
        )
    }

    fun getCurrentLogo(): Int {
        return sp.getInt(KEY_CURRENT_LOGO, R.drawable.ic_orbit)
    }

    fun setCurrentLogo(res: Int) {
        sp.edit { putInt(KEY_CURRENT_LOGO, res) }
    }

    fun getCurrentLogoDrawable(context: Context): Drawable? {
        return AppCompatResources.getDrawable(context, getCurrentLogo())
    }

    fun getLogoList(): List<Int> {
        return listOf(
            R.drawable.ic_orbit,
            R.drawable.ic_wand_stars,
            R.drawable.ic_psycho,
            R.drawable.ic_stars_2,
            R.drawable.ic_deepseek,
            R.drawable.ic_qwen,
            R.drawable.ic_gemini_color,
            R.drawable.ic_openai,
        )
    }
}