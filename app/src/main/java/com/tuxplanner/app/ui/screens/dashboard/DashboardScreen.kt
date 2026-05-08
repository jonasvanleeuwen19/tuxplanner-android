package com.tuxplanner.app.ui.screens.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuxplanner.app.TuxPlannerApp
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.TodoResponse
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as TuxPlannerApp).container

    val viewModel: DashboardViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(
                    container.eventRepository,
                    container.todoRepository,
                    container.authRepository
                ) as T
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var selectedEventInfo by remember { mutableStateOf<EventResponse?>(null) }
    var selectedTodoInfo by remember { mutableStateOf<TodoResponse?>(null) }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000L)
        }
    }

    val greeting = when (currentTime.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val greetingText = if (uiState.userName.isNotBlank()) "$greeting, ${uiState.userName}!" else "$greeting!"
    val today = LocalDate.now()
    val dateText = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            ", " + today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    val timeText = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting + clock
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Events today
            DashboardCard(title = "Events Today (${uiState.todayEvents.size})") {
                if (uiState.todayEvents.isEmpty()) {
                    Text(
                        text = "No events today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    uiState.todayEvents.forEach { event ->
                        EventSummaryRow(event = event, onClick = { selectedEventInfo = event })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // Tasks today
            DashboardCard(title = "Tasks Today (${uiState.todayTodos.size})") {
                if (uiState.todayTodos.isEmpty()) {
                    Text(
                        text = "No tasks due today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    uiState.todayTodos.forEach { todo ->
                        TodoSummaryRow(
                            todo = todo,
                            onToggle = { viewModel.toggleTodoComplete(todo) },
                            onClick = { selectedTodoInfo = todo }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // Work sessions today
            DashboardCard(title = "Work Sessions Today (${uiState.workSessions.size})") {
                if (uiState.workSessions.isEmpty()) {
                    Text(
                        text = "No work sessions today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    uiState.workSessions.forEach { session ->
                        EventSummaryRow(event = session, onClick = { selectedEventInfo = session })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    selectedEventInfo?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedEventInfo = null },
            title = { Text(event.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Start: ${event.start}")
                    event.end?.let { Text("End: $it") }
                    if (!event.location.isNullOrBlank()) Text("Location: ${event.location}")
                    if (!event.description.isNullOrBlank()) Text(event.description)
                }
            },
            confirmButton = { TextButton(onClick = { selectedEventInfo = null }) { Text("Close") } }
        )
    }
    selectedTodoInfo?.let { todo ->
        AlertDialog(
            onDismissRequest = { selectedTodoInfo = null },
            title = { Text(todo.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Priority: ${todo.priority}")
                    if (!todo.dueDate.isNullOrBlank()) Text("Due: ${todo.dueDate}")
                    if (!todo.description.isNullOrBlank()) Text(todo.description)
                }
            },
            confirmButton = { TextButton(onClick = { selectedTodoInfo = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun DashboardCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EventSummaryRow(event: EventResponse, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDashboardTime(event.start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodoSummaryRow(todo: TodoResponse, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (todo.completed) Icons.Default.CheckCircle
                else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (todo.completed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
            color = if (todo.completed) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDashboardTime(isoStr: String): String =
    runCatching {
        val dt = java.time.LocalDateTime.parse(isoStr.take(19))
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(isoStr)
