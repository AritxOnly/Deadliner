package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityFilter
import androidx.window.embedding.DividerAttributes
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.localutils.GlobalUtils
import com.aritxonly.deadliner.localutils.KeystorePreferenceManager
import com.aritxonly.deadliner.sync.SyncScheduler
import com.aritxonly.deadliner.web.AIUtils

class DeadlinerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalUtils.init(this)
        KeystorePreferenceManager.createKeyIfNeeded()
        AIUtils.init(this)
        AppSingletons.init(this)
        UserProfileRepository.init(this)

        if (GlobalUtils.cloudSyncEnable) {
            SyncScheduler.enqueuePeriodic(this)
        } else {
            SyncScheduler.cancelAll(this)
        }

        if (GlobalUtils.embeddedActivities) {
            if (GlobalUtils.dynamicSplit) {
                if (installSplitAttributes(GlobalUtils.splitPlaceholderEnable)) return
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
    private fun installSplitAttributes(placeholder: Boolean = false): Boolean {
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
            .setClearTop(true)
            .setDefaultSplitAttributes(splitAttributes)
            .setMinWidthDp(840)
            .build()

        if (placeholder) {
            val mainFilter = ActivityFilter(
                ComponentName(this, "com.aritxonly.deadliner.MainActivity"),
                /* intentAction = */ null
            )

            val placeholderRule = SplitPlaceholderRule.Builder(
                setOf(mainFilter),
                Intent().setComponent(
                    ComponentName(this, "com.aritxonly.deadliner.PlaceholderActivity")
                )
            )
                .setDefaultSplitAttributes(splitAttributes)
                .setMinWidthDp(840)
                .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ALWAYS)
                .build()

            RuleController.getInstance(this).setRules(setOf(placeholderRule))
        } else {
            RuleController.getInstance(this).setRules(setOf(pairRule))
        }

        return true
    }
}
