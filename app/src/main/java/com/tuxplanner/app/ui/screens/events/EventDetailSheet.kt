package com.tuxplanner.app.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.ui.common.OsmMiniMap
import androidx.compose.material3.Checkbox
import androidx.compose.ui.window.Dialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: EventResponse,
    calendarLists: List<CalendarListResponse>,
    linkedTodos: List<TodoResponse>,
    allTodos: List<TodoResponse>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLinkTodo: (Int) -> Unit,
    onUnlinkTodo: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showTaskManager by remember { mutableStateOf(false) }

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
            // Title row with color indicator
            val calendarList = calendarLists.find { it.id == event.calendarListId }
            val colorHex = event.color ?: calendarList?.color ?: "#1565C0"
            val color = runCatching {
                Color(android.graphics.Color.parseColor(colorHex))
            }.getOrDefault(Color(0xFF1565C0))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Date/time
            InfoRow(label = "Start", value = formatDetailDateTime(event.start))
            if (event.end != null) {
                InfoRow(label = "End", value = formatDetailDateTime(event.end))
            }
            if (event.allDay) {
                InfoRow(label = "All Day", value = "Yes")
            }

            // Calendar list
            if (calendarList != null) {
                InfoRow(label = "Calendar", value = calendarList.name)
            }

            // Location
            if (!event.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OsmMiniMap(locationText = event.location, modifier = Modifier.fillMaxWidth())
            }

            // Description
            if (!event.description.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Linked todos count
            InfoRow(label = "Linked Tasks", value = "${linkedTodos.size}")
            if (event.source != "ical") {
                OutlinedButton(onClick = { showTaskManager = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage linked tasks")
                }
            }
            linkedTodos.forEach { todo ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(todo.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    if (event.source != "ical") {
                        TextButton(onClick = { onUnlinkTodo(todo.id) }) { Text("Unlink") }
                    }
                }
            }

            HorizontalDivider()

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (event.source != "ical") {
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
                }
            }
        }
    }

    if (showTaskManager) {
        LinkedTasksManagerDialog(
            todos = allTodos,
            initiallyLinkedIds = linkedTodos.map { it.id }.toSet(),
            onDismiss = { showTaskManager = false },
            onSave = { selectedIds ->
                val linkedIds = linkedTodos.map { it.id }.toSet()
                (selectedIds - linkedIds).forEach { onLinkTodo(it) }
                (linkedIds - selectedIds).forEach { onUnlinkTodo(it) }
                showTaskManager = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkedTasksManagerDialog(
    todos: List<TodoResponse>,
    initiallyLinkedIds: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit
) {
    var selectedIds by rememberSaveable { mutableStateOf(initiallyLinkedIds) }

    Dialog(onDismissRequest = onDismiss) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Linked tasks") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = { TextButton(onClick = { onSave(selectedIds) }) { Text("Save") } }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                todos.forEach { todo ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = selectedIds.contains(todo.id),
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + todo.id else selectedIds - todo.id
                            }
                        )
                        Text(todo.title, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatDetailDateTime(isoStr: String): String =
    runCatching {
        val dt = LocalDateTime.parse(isoStr.take(19))
        dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }.getOrDefault(isoStr)
