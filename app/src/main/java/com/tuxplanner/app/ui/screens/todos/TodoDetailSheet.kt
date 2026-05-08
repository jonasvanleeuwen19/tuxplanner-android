package com.tuxplanner.app.ui.screens.todos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tuxplanner.app.ui.common.TimePickerField
import com.tuxplanner.app.data.model.TaskSessionResponse
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.ui.theme.PriorityHigh
import com.tuxplanner.app.ui.theme.PriorityLow
import com.tuxplanner.app.ui.theme.PriorityMedium
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailSheet(
    todo: TodoResponse,
    sessions: List<TaskSessionResponse>,
    isLoadingSessions: Boolean,
    isEditable: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    onAddSession: (start: String, end: String?, note: String?) -> Unit,
    onDeleteSession: (sessionId: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showAddSessionDialog by remember { mutableStateOf(false) }

    val priorityColor = when (todo.priority) {
        "high" -> PriorityHigh
        "low" -> PriorityLow
        else -> PriorityMedium
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title + complete toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleComplete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (todo.completed) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (todo.completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Priority badge
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = todo.priority.replaceFirstChar { it.uppercase() },
                        color = priorityColor
                    )
                }
            )

            HorizontalDivider()

            // Due date
            if (todo.dueDate != null) {
                Row {
                    Text(
                        text = "Due: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTodoDate(todo.dueDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Description
            if (!todo.description.isNullOrBlank()) {
                Text(
                    text = todo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Linked event
            if (todo.eventId != null) {
                Row {
                    Text(
                        text = "Linked event ID: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${todo.eventId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            // Work sessions section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Work Sessions (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { showAddSessionDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add session",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isLoadingSessions) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (sessions.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No sessions yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sessions.forEach { session ->
                    SessionRow(
                        session = session,
                        onDelete = { onDeleteSession(session.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            HorizontalDivider()

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditable) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                } else {
                    Text(
                        text = "External calendar task: editing is disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAddSessionDialog) {
        AddSessionDialog(
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { sessions ->
                sessions.forEach { session ->
                    onAddSession(session.start, session.end, session.note)
                }
                showAddSessionDialog = false
            }
        )
    }
}

@Composable
private fun SessionRow(session: TaskSessionResponse, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatSessionTime(session.start),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (session.end != null) {
                Text(
                    text = "→ ${formatSessionTime(session.end)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!session.note.isNullOrBlank()) {
                Text(
                    text = session.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete session",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<SessionDraft>) -> Unit
) {
    var step by rememberSaveable { mutableStateOf(1) }
    var selectedDates by remember { mutableStateOf(setOf<java.time.LocalDate>()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var dayConfigs by remember { mutableStateOf<Map<java.time.LocalDate, SessionDayConfig>>(emptyMap()) }

    Dialog(onDismissRequest = onDismiss) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Plan Work Sessions") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    navigationIcon = {
                        TextButton(onClick = { if (step == 2) step = 1 else onDismiss() }) {
                            Text(if (step == 2) "Back" else "Cancel")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            if (step == 1) {
                                if (selectedDates.isNotEmpty()) {
                                    dayConfigs = selectedDates.sorted().associateWith { date ->
                                        dayConfigs[date] ?: SessionDayConfig()
                                    }
                                    step = 2
                                }
                                return@TextButton
                            }

                            val sessions = selectedDates.sorted().mapNotNull { date ->
                                val cfg = dayConfigs[date] ?: SessionDayConfig()
                                val startTime = runCatching { java.time.LocalTime.parse(cfg.startTime) }.getOrNull() ?: return@mapNotNull null
                                val endTime = runCatching { java.time.LocalTime.parse(cfg.endTime) }.getOrNull() ?: return@mapNotNull null
                                val start = date.atTime(startTime)
                                val end = date.atTime(endTime)
                                if (!end.isAfter(start)) return@mapNotNull null
                                SessionDraft(
                                    start = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                    end = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                    note = cfg.note.ifBlank { null }
                                )
                            }
                            if (sessions.isNotEmpty()) onConfirm(sessions)
                        }) { Text(if (step == 1) "Next" else "Add") }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { step = 1 },
                        selected = step == 1
                    ) { Text("1. Days") }
                    SegmentedButton(
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { if (selectedDates.isNotEmpty()) step = 2 },
                        selected = step == 2,
                        enabled = selectedDates.isNotEmpty()
                    ) { Text("2. Per day") }
                }

                if (step == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { month = month.minusMonths(1) }) { Text("Prev") }
                        Text(
                            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { month = month.plusMonths(1) }) { Text("Next") }
                    }
                    CalendarSelectionGrid(
                        month = month,
                        selectedDates = selectedDates,
                        onToggleDate = { date ->
                            selectedDates = if (selectedDates.contains(date)) selectedDates - date else selectedDates + date
                        }
                    )
                } else {
                    selectedDates.sorted().forEach { date ->
                        val cfg = dayConfigs[date] ?: SessionDayConfig()
                        androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(date.toString(), style = MaterialTheme.typography.titleSmall)
                                TimePickerField(
                                    label = "Start time",
                                    value = cfg.startTime,
                                    onValueChange = { value -> dayConfigs = dayConfigs + (date to cfg.copy(startTime = value)) }
                                )
                                TimePickerField(
                                    label = "End time",
                                    value = cfg.endTime,
                                    onValueChange = { value -> dayConfigs = dayConfigs + (date to cfg.copy(endTime = value)) }
                                )
                                OutlinedTextField(
                                    value = cfg.note,
                                    onValueChange = { value -> dayConfigs = dayConfigs + (date to cfg.copy(note = value)) },
                                    label = { Text("Note (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSelectionGrid(
    month: YearMonth,
    selectedDates: Set<java.time.LocalDate>,
    onToggleDate: (java.time.LocalDate) -> Unit
) {
    val calendarEmptyCellHeight = 40.dp
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1
    val totalDays = month.lengthOfMonth()
    val totalCells = startOffset + totalDays
    val rows = (totalCells + 6) / 7

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
    repeat(rows) { row ->
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(7) { col ->
                val idx = row * 7 + col
                val day = idx - startOffset + 1
                if (day !in 1..totalDays) {
                    Spacer(modifier = Modifier.weight(1f).height(calendarEmptyCellHeight))
                } else {
                    val date = month.atDay(day)
                    FilterChip(
                        selected = selectedDates.contains(date),
                        onClick = { onToggleDate(date) },
                        label = { Text(day.toString()) },
                        modifier = Modifier.weight(1f).padding(2.dp)
                    )
                }
            }
        }
    }
}

private data class SessionDayConfig(
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val note: String = ""
)

private data class SessionDraft(
    val start: String,
    val end: String?,
    val note: String?
)

private fun formatTodoDate(isoStr: String): String =
    runCatching {
        val dt = LocalDateTime.parse(isoStr.take(19))
        dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }.getOrElse {
        runCatching {
            java.time.LocalDate.parse(isoStr.take(10))
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }.getOrDefault(isoStr)
    }

private fun formatSessionTime(isoStr: String): String =
    runCatching {
        val dt = LocalDateTime.parse(isoStr.take(19))
        dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }.getOrDefault(isoStr)
