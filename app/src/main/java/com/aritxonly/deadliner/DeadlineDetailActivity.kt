package com.aritxonly.deadliner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aritxonly.deadliner.data.DDLRepository
import com.aritxonly.deadliner.localutils.enableEdgeToEdgeForAllDevices
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.ui.detail.DeadlineDetailScreen
import com.aritxonly.deadliner.ui.theme.DeadlinerTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
class DeadlineDetailActivity : DeadlinerAppCompatActivity() {

    companion object {
        const val EXTRA_DEADLINE = "com.aritxonly.deadliner.deadline"

        fun newIntent(context: Context, deadline: DDLItem): Intent {
            return Intent(context, DeadlineDetailActivity::class.java).apply {
                putExtra(EXTRA_DEADLINE, deadline)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeForAllDevices()
        super.onCreate(savedInstanceState)

        val initialDeadline = intent.getParcelableExtra<DDLItem>(EXTRA_DEADLINE)
            ?: throw IllegalArgumentException("Missing Deadline parameter")

        val repo = DDLRepository()
        val latestDeadline = repo.getDDLById(initialDeadline.id) ?: initialDeadline

        setContent {
            DeadlinerTheme {
                var currentDeadline by remember { mutableStateOf(latestDeadline) }

                DeadlineDetailScreen(
                    applicationContext = applicationContext,
                    activityContext = this,
                    deadline = currentDeadline,
                    onClose = { finish() },
                    onEdit = {
                        val editDialog = EditDDLFragment(currentDeadline) { updatedDDL ->
                            repo.updateDDL(updatedDDL)
                            currentDeadline = repo.getDDLById(updatedDDL.id) ?: updatedDDL
                        }
                        editDialog.show(supportFragmentManager, "EditDDLFragment")
                    },
                    onPersistDeadline = { updated ->
                        repo.updateDDL(updated)
                        currentDeadline = repo.getDDLById(updated.id) ?: updated
                    },
                    onToggleStar = { isStarred ->
                        val updated = currentDeadline.copy(isStared = isStarred)
                        repo.updateDDL(updated)
                        currentDeadline = repo.getDDLById(updated.id) ?: updated
                    },
                    onApplyTaskAction = { action, confirmed ->
                        val updated = repo.applyTaskAction(
                            itemId = currentDeadline.id,
                            action = action,
                            confirmed = confirmed
                        )
                        currentDeadline = repo.getDDLById(updated.id) ?: updated
                        currentDeadline
                    },
                    onDelete = { id ->
                        repo.deleteDDL(id)
                    },
                    finishActivity = { finishAfterTransition() }
                )
            }
        }
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
