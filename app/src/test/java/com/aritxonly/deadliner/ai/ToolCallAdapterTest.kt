package com.aritxonly.deadliner.ai

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class ToolCallAdapterTest {
    private val gson = Gson()

    private fun newAdapter(): AndroidToolCallAdapter {
        val taskId = AtomicLong(100)
        val habitId = AtomicLong(200)
        return AndroidToolCallAdapter(
            gson = gson,
            taskCreatorFactory = {
                object : TaskCreator {
                    override fun createTask(name: String, due: LocalDateTime, note: String): Long {
                        return taskId.incrementAndGet()
                    }
                }
            },
            habitCreatorFactory = {
                object : HabitCreator {
                    override fun createHabit(input: CreateHabitInput, now: LocalDateTime): CreatedHabit {
                        val id = habitId.incrementAndGet()
                        return CreatedHabit(
                            id = id,
                            ddlId = id + 1000,
                            name = input.name,
                            period = input.period,
                            timesPerPeriod = if (input.goalType.name == "TOTAL") 1 else input.timesPerPeriod,
                            goalType = input.goalType,
                            totalTarget = input.totalTarget,
                        )
                    }
                }
            },
        )
    }

    private fun exec(tool: String, argsJson: String): Map<String, Any?> = runBlocking {
        val json = newAdapter().execute(
            ToolCallExecution(
                id = "t1",
                toolName = tool,
                argsJson = argsJson,
            )
        ).resultJson
        @Suppress("UNCHECKED_CAST")
        gson.fromJson(json, Map::class.java) as Map<String, Any?>
    }

    @Test
    fun createTask_singleLegacy_success() {
        val res = exec("create_task", """{"name":"task1","dueTime":"2026-05-02 20:00","note":"n"}""")
        assertTrue(res["ok"] as Boolean)
        assertNotNull(res["task"])
    }

    @Test
    fun createTask_batchNew_success() {
        val res = exec(
            "create_task",
            """{"tasks":[{"name":"a","dueTime":"2026-05-02 20:00"},{"name":"b","dueTime":"2026-05-03 21:00"}]}"""
        )
        assertTrue(res["ok"] as Boolean)
        val created = res["createdTasks"] as List<*>
        assertEquals(2, created.size)
    }

    @Test
    fun createHabit_singleLegacy_success() {
        val res = exec(
            "create_habit",
            """{"name":"h1","period":"daily","timesPerPeriod":1,"goalType":"frequency"}"""
        )
        assertTrue(res["ok"] as Boolean)
        assertNotNull(res["habit"])
    }

    @Test
    fun createHabit_batchNew_success() {
        val res = exec(
            "create_habit",
            """{"habits":[{"name":"h1","period":"daily","timesPerPeriod":1,"goalType":"frequency"},{"name":"h2","period":"weekly","timesPerPeriod":2,"goalType":"frequency"}]}"""
        )
        assertTrue(res["ok"] as Boolean)
        val created = res["createdHabits"] as List<*>
        assertEquals(2, created.size)
    }

    @Test
    fun createTask_batch_partialFailure_invalidItem() {
        val res = exec(
            "create_task",
            """{"tasks":[{"name":"ok","dueTime":"2026-05-02 20:00"},{"name":"","dueTime":"2026-05-03 20:00"}]}"""
        )
        assertFalse(res["ok"] as Boolean)
        assertTrue(res["partialSuccess"] as Boolean)
        val summary = gson.toJsonTree(res["summary"]).asJsonObject
        assertEquals(1, summary.get("successCount").asInt)
        assertEquals(1, summary.get("failureCount").asInt)
    }

    @Test
    fun createTask_emptyArray_orMissingRequired_shouldFail() {
        val emptyRes = exec("create_task", """{"tasks":[]}""")
        assertFalse(emptyRes["ok"] as Boolean)
        val missingFieldRes = exec("create_task", """{"name":"x"}""")
        assertFalse(missingFieldRes["ok"] as Boolean)
    }

    @Test
    fun createTask_dueTimeFormatValidation_shouldFail() {
        val res = exec("create_task", """{"name":"x","dueTime":"bad-time"}""")
        assertFalse(res["ok"] as Boolean)
        val msg = res["message"] as String
        assertTrue(msg.contains("dueTime", ignoreCase = true))
    }

    @Test
    fun createHabit_goalTypeTotal_withoutTotalTarget_shouldFail() {
        val res = exec(
            "create_habit",
            """{"name":"h","period":"daily","timesPerPeriod":1,"goalType":"total"}"""
        )
        assertFalse(res["ok"] as Boolean)
        val msg = res["message"] as String
        assertTrue(msg.contains("totalTarget"))
    }
}
