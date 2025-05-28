package com.aritxonly.deadliner.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.GlobalUtils
import com.aritxonly.deadliner.model.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

/**
 * Helper for creating, updating, and deleting calendar events from DDLItem instances.
 */
class CalendarHelper(private val context: Context) {

    companion object {
        private const val CALENDAR_ACCOUNT_NAME = "Deadliner"
        private const val CALENDAR_DISPLAY_NAME = "Deadliner Calendar"
        private const val CALENDAR_ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL
        private val ICS_DATE_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Ensures a calendar exists and returns its ID.
     */
    private fun getOrCreateCalendarId(): Long {
        val resolver = context.contentResolver
        // Query primary calendar
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(CALENDAR_ACCOUNT_NAME)

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }

        // Create a new local calendar
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }

        val insertUri = uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            .build()

        val result: Uri = resolver.insert(insertUri, values)
            ?: throw IllegalStateException("Failed to create calendar account")
        return ContentUris.parseId(result)
    }

    /**
     * Inserts a DDLItem as a calendar event and returns the event ID.
     */
    suspend fun insertEvent(item: DDLItem): Long = withContext(Dispatchers.IO) {
        val calendarId = getOrCreateCalendarId()
        val resolver = context.contentResolver

        val endMillis = parseToMillis(item.endTime)

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, item.name)
            put(CalendarContract.Events.DTSTART, endMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.DESCRIPTION, item.note)
            if (item.type == DeadlineType.HABIT && item.habitCount > 1) {
                put(CalendarContract.Events.RRULE, "FREQ=DAILY;COUNT=${item.habitCount}")
            }
        }

        val uri: Uri = if (item.calendarEventId != null) {
            // 如果已有 calendarEventId，则更新事件
            val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, item.calendarEventId!!)
            resolver.update(updateUri, values, null, null)
            return@withContext item.calendarEventId!!
        } else {
            // 否则插入新事件
            resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: throw IllegalStateException("Failed to insert calendar event")
        }

        val eventId = ContentUris.parseId(uri)

        // 更新 DDLItem 的 calendarEventId 字段
        item.calendarEventId = eventId

        // 添加默认提醒
        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 10)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)

        eventId
    }

    fun queryCalendarEvents(): List<CalendarEvent> {
        val resolver = context.contentResolver
        val calendarId = getOrCreateCalendarId()

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.RRULE
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val events = mutableListOf<CalendarEvent>()
        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                val dtStart = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val dtEnd = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
                val rrule = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE))

                val event = CalendarEvent(
                    id = id,
                    title = title,
                    startMillis = dtStart,
                    endMillis = dtEnd,
                    description = description,
                    rrule = rrule
                )
                events.add(event)
            }
        }

        return events
    }

    /**
     * Parses a date-time string ("yyyy-MM-dd HH:mm") into milliseconds.
     */
    private fun parseToMillis(dateTime: String): Long {
        val ldt = GlobalUtils.parseDateTime(dateTime)
            ?: throw IllegalArgumentException("Invalid date format: $dateTime")

        return ldt.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}