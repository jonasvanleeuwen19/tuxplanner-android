package com.tuxplanner.app.data.model

// ── Auth ─────────────────────────────────────────────────────

data class SetupStatusResponse(
    val setupRequired: Boolean = false
)

data class UserInfo(
    val username: String = "",
    val isAdmin: Boolean = false
)

data class MessageResponse(
    val message: String = ""
)

data class SetupRequest(
    val username: String,
    val password: String
)

// ── Calendar List ─────────────────────────────────────────────

data class CalendarListResponse(
    val id: Int = 0,
    val name: String = "",
    val color: String = "#3b82f6",
    val isVisible: Boolean = true,
    val isAuto: Boolean = false,
    val icalFeedId: Int? = null,
    val createdAt: String = ""
)

// ── Event ─────────────────────────────────────────────────────

data class EventResponse(
    val id: Int = 0,
    val title: String = "",
    val description: String? = null,
    val location: String? = null,
    val start: String = "",
    val end: String? = null,
    val allDay: Boolean = false,
    val color: String? = null,
    val calendarListId: Int? = null,
    val source: String? = null,
    val taskCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String? = null
)

data class EventCreate(
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val start: String,
    val end: String? = null,
    val allDay: Boolean = false,
    val color: String? = null,
    val calendarListId: Int? = null
)

data class EventUpdate(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val start: String? = null,
    val end: String? = null,
    val allDay: Boolean? = null,
    val color: String? = null,
    val calendarListId: Int? = null
)

// ── Todo ──────────────────────────────────────────────────────

data class TodoResponse(
    val id: Int = 0,
    val title: String = "",
    val description: String? = null,
    val completed: Boolean = false,
    val priority: String = "medium",
    val dueDate: String? = null,
    val todoListId: Int? = null,
    val category: String = "Default",
    val eventId: Int? = null,
    val sessionCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String? = null
)

data class TodoCreate(
    val title: String,
    val description: String? = null,
    val priority: String = "medium",
    val dueDate: String? = null,
    val todoListId: Int? = null,
    val category: String = "Default",
    val eventId: Int? = null
)

data class TodoUpdate(
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean? = null,
    val priority: String? = null,
    val dueDate: String? = null,
    val todoListId: Int? = null,
    val category: String? = null
)

// ── Todo List ─────────────────────────────────────────────────

data class TodoListResponse(
    val id: Int = 0,
    val name: String = "",
    val color: String = "#3b82f6",
    val createdAt: String = ""
)

data class TodoListCreate(
    val name: String,
    val color: String = "#3b82f6"
)

data class TodoListUpdate(
    val name: String? = null,
    val color: String? = null
)

// ── Calendar List CRUD ────────────────────────────────────────

data class CalendarListCreate(
    val name: String,
    val color: String = "#3b82f6",
    val isVisible: Boolean = true
)

data class CalendarListUpdate(
    val name: String? = null,
    val color: String? = null,
    val isVisible: Boolean? = null
)

// ── Task Session ──────────────────────────────────────────────

data class TaskSessionResponse(
    val id: Int = 0,
    val todoId: Int = 0,
    val eventId: Int? = null,
    val start: String = "",
    val end: String? = null,
    val note: String? = null,
    val createdAt: String = ""
)

data class TaskSessionCreate(
    val start: String,
    val end: String? = null,
    val note: String? = null
)

data class TaskSessionUpdate(
    val start: String? = null,
    val end: String? = null,
    val note: String? = null
)
