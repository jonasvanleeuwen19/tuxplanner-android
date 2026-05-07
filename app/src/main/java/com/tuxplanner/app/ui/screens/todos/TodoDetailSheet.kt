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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
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

@Composable
private fun AddSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<SessionDraft>) -> Unit
) {
    var step by rememberSaveable { mutableStateOf(1) }
    var selectedDates by remember { mutableStateOf(setOf<String>()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var startHour by rememberSaveable { mutableStateOf("9") }
    var startMinute by rememberSaveable { mutableStateOf("0") }
    var defaultHours by rememberSaveable { mutableStateOf("1") }
    var defaultMinutes by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Work Sessions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                    ) { Text("2. Duration") }
                }

                if (step == 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { month = month.minusMonths(1) }) { Text("Prev") }
                        Text(
                            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { month = month.plusMonths(1) }) { Text("Next") }
                    }
                    val daysInMonth = month.lengthOfMonth()
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (day in 1..daysInMonth) {
                            val date = month.atDay(day).toString()
                            val selected = selectedDates.contains(date)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedDates = if (selected) selectedDates - date else selectedDates + date
                                },
                                label = { Text(day.toString()) }
                            )
                        }
                    }
                } else {
                    Text("${selectedDates.size} day(s) selected")
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { startHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Start hour (0-23)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = startMinute,
                        onValueChange = { startMinute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Start minute (0-59)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defaultHours,
                        onValueChange = { defaultHours = it.filter(Char::isDigit) },
                        label = { Text("Hours") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defaultMinutes,
                        onValueChange = { defaultMinutes = it.filter(Char::isDigit) },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 1) {
                        if (selectedDates.isNotEmpty()) step = 2
                        return@TextButton
                    }
                    val hours = defaultHours.toIntOrNull() ?: 0
                    val minutes = defaultMinutes.toIntOrNull() ?: 0
                    val startHourInt = startHour.toIntOrNull() ?: -1
                    val startMinuteInt = startMinute.toIntOrNull() ?: -1
                    val totalMinutes = hours * 60 + minutes
                    if (startHourInt !in 0..23 || startMinuteInt !in 0..59) {
                        error = "Choose a valid start time"
                        return@TextButton
                    }
                    if (totalMinutes <= 0) {
                        error = "Please set at least 1 minute of work time"
                        return@TextButton
                    }
                    val sessions = selectedDates.sorted().map { date ->
                        val start = LocalDateTime.parse(
                            "${date}T${startHourInt.toString().padStart(2, '0')}:${startMinuteInt.toString().padStart(2, '0')}:00"
                        )
                        val end = start.plusMinutes(totalMinutes.toLong())
                        SessionDraft(
                            start = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            end = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            note = note.ifBlank { null }
                        )
                    }
                    onConfirm(sessions)
                }
            ) { Text(if (step == 1) "Next" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (step == 2) step = 1 else onDismiss()
            }) { Text(if (step == 2) "Back" else "Cancel") }
        }
    )
}

// Internal helper used by the multi-step planner before persisting sessions.
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
