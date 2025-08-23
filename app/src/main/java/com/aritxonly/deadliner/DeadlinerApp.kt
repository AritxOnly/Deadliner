package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.DividerAttributes
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.web.DeepSeekUtils

class DeadlinerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalUtils.init(this)
        KeystorePreferenceManager.createKeyIfNeeded()
        DeepSeekUtils.init(this)
        AppSingletons.init(this)

        if (GlobalUtils.embeddedActivities) {
            if (GlobalUtils.dynamicSplit) {
                if (installSplitAttributesTest()) return
            }

            GlobalUtils.dynamicSplit = false

            val rulesRes = if (GlobalUtils.splitPlaceholderEnable) {
                R.xml.tablet_split_rules_placeholder
            } else {
                R.xml.tablet_split_rules_standard
            }
            val rules = RuleController.parseRules(this, rulesRes)
            RuleController.getInstance(this).setRules(rules)
        }
    }

    @SuppressLint("RequiresWindowSdk")
    private fun installSplitAttributesTest(): Boolean {
        val splitAttributesBuilder: SplitAttributes.Builder = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.45f))
            .setLayoutDirection(SplitAttributes.LayoutDirection.LEFT_TO_RIGHT)

        if (WindowSdkExtensions.getInstance().extensionVersion >= 6) {
            splitAttributesBuilder.setDividerAttributes(
                DividerAttributes.DraggableDividerAttributes.Builder()
                    .setWidthDp(0)
                    .setDragRange(DividerAttributes.DragRange.DRAG_RANGE_SYSTEM_DEFAULT)
                    .build()
            )
        } else {
            return false
        }

        val splitAttributes: SplitAttributes = splitAttributesBuilder.build()

        val pairRule = SplitPairRule.Builder(
            setOf(
                SplitPairFilter(
                    ComponentName(this, "com.aritxonly.deadliner.MainActivity"),
                    ComponentName(this, "com.aritxonly.deadliner.DeadlineDetailActivity"),
                    null
                ),
                SplitPairFilter(
                    ComponentName(this, "com.aritxonly.deadliner.MainActivity"),
                    ComponentName(this, "com.aritxonly.deadliner.SettingsActivity"),
                    null
                ),
                SplitPairFilter(
                    ComponentName(this, "com.aritxonly.deadliner.MainActivity"),
                    ComponentName(this, "com.aritxonly.deadliner.OverviewActivity"),
                    null
                ),
            )
        )
            .setDefaultSplitAttributes(splitAttributes) // 👈 在这里应用
            .setMinWidthDp(840)
            .build()

        RuleController.getInstance(this).setRules(setOf(pairRule))

        return true
    }
}
