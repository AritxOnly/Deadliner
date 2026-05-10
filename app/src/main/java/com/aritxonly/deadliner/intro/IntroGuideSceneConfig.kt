package com.aritxonly.deadliner.intro

import androidx.annotation.RawRes
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.UiStyle

data class IntroGuideSceneConfig(
    val id: String,
    val fileName: String,
    val title: String,
    val subtitle: String,
    val detail: String
)

object IntroGuideScenes {

    private val allScenes = listOf(
        IntroGuideSceneConfig(
            id = "guide_scene_1",
            fileName = "Scene-1-Harmony.json",
            title = "滑动任务，快捷处理",
            subtitle = "在任务卡片上左右滑动，就能快速完成常用操作。",
            detail = "像完成、归档这类高频动作，不必点进详情页，在列表里就能顺手处理。"
        ),
        IntroGuideSceneConfig(
            id = "guide_scene_2",
            fileName = "Scene-2-Harmony.json",
            title = "点一下 Lifi AI，唤起智能体",
            subtitle = "遇到临时想法、碎片信息或模糊需求，都可以先交给它整理。",
            detail = "Lifi AI 能帮你提炼任务意图、补齐结构，再决定是否加入清单。"
        ),
        IntroGuideSceneConfig(
            id = "guide_scene_3",
            fileName = "Scene-3.json",
            title = "上滑 + 号展开 Lifi AI",
            subtitle = "在简洁模式里，沿着底部的 + 号向上滑动，就能直接展开智能体入口。",
            detail = "想到什么就先记什么，让 Lifi AI 帮你整理，再决定要不要补充成正式任务。"
        ),
        IntroGuideSceneConfig(
            id = "guide_scene_4",
            fileName = "Scene-4-Harmony.json",
            title = "长按任务，进入多选",
            subtitle = "想批量完成、归档或删除时，长按任意任务即可开始选择。",
            detail = "连续处理一组任务会更高效，适合做每日清单收尾或集中整理。"
        ),
        IntroGuideSceneConfig(
            id = "guide_scene_5",
            fileName = "Scene-5-Android.json",
            title = "轻点任务，查看详情与子任务",
            subtitle = "短按任务可以进入详情页，继续补充信息或拆分子任务。",
            detail = "当一件事需要更多上下文时，详情页就是你继续展开和整理的地方。"
        ),
        IntroGuideSceneConfig(
            id = "guide_scene_6",
            fileName = "Scene-6-Android.json",
            title = "点击 Tab 顶部按钮，快速导航",
            subtitle = "每个 Tab 上方的按钮都可以带你进入对应功能入口。",
            detail = "把常用入口放在手边，切页之外也能更快抵达你要用的能力。"
        )
    )

    fun forStyle(style: UiStyle): List<IntroGuideSceneConfig> {
        val ids = when (style) {
            UiStyle.Classic -> listOf(
                "guide_scene_1",
                "guide_scene_2",
                "guide_scene_4",
                "guide_scene_5"
            )

            UiStyle.Simplified -> listOf(
                "guide_scene_1",
                "guide_scene_3",
                "guide_scene_4",
                "guide_scene_5"
            )

            UiStyle.Miuix -> listOf(
                "guide_scene_1",
                "guide_scene_2",
                "guide_scene_4",
                "guide_scene_5",
                "guide_scene_6"
            )
        }

        return ids.mapNotNull { id -> allScenes.find { it.id == id } }
    }

    @RawRes
    fun resolveRawRes(fileName: String): Int = when (fileName) {
        "Scene-1-Harmony.json" -> R.raw.scene_1_harmony
        "Scene-2-Harmony.json" -> R.raw.scene_2_harmony
        "Scene-3.json" -> R.raw.scene_3
        "Scene-4-Harmony.json" -> R.raw.scene_4_harmony
        "Scene-5-Android.json" -> R.raw.scene_5_android
        "Scene-6-Android.json" -> R.raw.scene_6_android
        else -> error("Unknown intro guide scene file: $fileName")
    }
}
