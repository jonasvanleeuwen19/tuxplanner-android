package com.tuxplanner.app.ui.screens.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuxplanner.app.TuxPlannerApp
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.EventUpdate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(onNavigateToCalendarLists: () -> Unit = {}) {
    val context = LocalContext.current
    val container = (context.applicationContext as TuxPlannerApp).container

    val viewModel: EventsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EventsViewModel(container.eventRepository, container.calendarListRepository) as T
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventResponse?>(null) }

    val filteredEvents = remember(uiState.events, uiState.filterCalendarListId) {
        if (uiState.filterCalendarListId == null) uiState.events
        else uiState.events.filter { it.calendarListId == uiState.filterCalendarListId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadEvents() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToCalendarLists) {
                        Icon(Icons.Default.FilterList, contentDescription = "Manage Calendars")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add event")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Calendar list filter chips
            if (uiState.calendarLists.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterCalendarListId == null,
                        onClick = { viewModel.setCalendarListFilter(null) },
                        label = { Text("All") }
                    )
                    uiState.calendarLists.forEach { cal ->
                        FilterChip(
                            selected = uiState.filterCalendarListId == cal.id,
                            onClick = {
                                viewModel.setCalendarListFilter(
                                    if (uiState.filterCalendarListId == cal.id) null else cal.id
                                )
                            },
                            label = { Text(cal.name) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    uiState.error != null -> ErrorView(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadEvents() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    filteredEvents.isEmpty() -> EmptyView(modifier = Modifier.align(Alignment.Center))
                    else -> EventsList(
                        events = filteredEvents,
                        onTap = { selectedEvent = it },
                        onDelete = { viewModel.deleteEvent(it) }
                    )
                }
            }
        }
    }

    // Event detail bottom sheet
    selectedEvent?.let { event ->
        EventDetailSheet(
            event = event,
            calendarLists = uiState.calendarLists,
            onDismiss = { selectedEvent = null },
            onEdit = {
                editingEvent = event
                selectedEvent = null
                showEditDialog = true
            },
            onDelete = {
                viewModel.deleteEvent(event.id)
                selectedEvent = null
            }
        )
    }

    // Add event dialog
    if (showAddDialog) {
        EventFormDialog(
            title = "New Event",
            calendarLists = uiState.calendarLists,
            onDismiss = { showAddDialog = false },
            onConfirm = { eventTitle, description, location, startStr, endStr, allDay, color, calListId ->
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val start = runCatching { LocalDateTime.parse(startStr, fmt) }.getOrNull()
                    ?: LocalDateTime.now()
                val end = runCatching { LocalDateTime.parse(endStr, fmt) }.getOrNull()
                viewModel.createEvent(
                    EventCreate(
                        title = eventTitle,
                        description = description.ifBlank { null },
                        location = location.ifBlank { null },
                        start = start.format(isoFmt),
                        end = end?.format(isoFmt),
                        allDay = allDay,
                        color = color.ifBlank { null },
                        calendarListId = calListId
                    )
                )
                showAddDialog = false
            }
        )
    }

    // Edit event dialog
    if (showEditDialog) {
        editingEvent?.let { event ->
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            EventFormDialog(
                title = "Edit Event",
                calendarLists = uiState.calendarLists,
                initialTitle = event.title,
                initialDescription = event.description ?: "",
                initialLocation = event.location ?: "",
                initialStart = runCatching {
                    LocalDateTime.parse(event.start.take(19)).format(fmt)
                }.getOrDefault(event.start.take(16)),
                initialEnd = event.end?.let {
                    runCatching { LocalDateTime.parse(it.take(19)).format(fmt) }.getOrDefault(it.take(16))
                } ?: "",
                initialAllDay = event.allDay,
                initialColor = event.color ?: "",
                initialCalendarListId = event.calendarListId,
                onDismiss = { showEditDialog = false; editingEvent = null },
                onConfirm = { eventTitle, description, location, startStr, endStr, allDay, color, calListId ->
                    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val userFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    val start = runCatching { LocalDateTime.parse(startStr, userFormatter).format(isoFormatter) }.getOrNull()
                    val end = runCatching { LocalDateTime.parse(endStr, userFormatter).format(isoFormatter) }.getOrNull()
                    viewModel.updateEvent(
                        event.id,
                        EventUpdate(
                            title = eventTitle,
                            description = description.ifBlank { null },
                            location = location.ifBlank { null },
                            start = start,
                            end = end,
                            allDay = allDay,
                            color = color.ifBlank { null },
                            calendarListId = calListId
                        )
                    )
                    showEditDialog = false
                    editingEvent = null
                }
            )
        }
    }
}

@Composable
private fun EventsList(
    events: List<EventResponse>,
    onTap: (EventResponse) -> Unit,
    onDelete: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onTap = { onTap(event) },
                onDelete = { onDelete(event.id) }
            )
        }
    }
}

@Composable
private fun EventCard(event: EventResponse, onTap: () -> Unit, onDelete: () -> Unit) {
    val colorHex = event.color ?: "#1565C0"
    val color = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color(0xFF1565C0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onTap() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateTime(event.start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!event.location.isNullOrBlank()) {
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.taskCount > 0) {
                    Text(
                        text = "${event.taskCount} task(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            if (event.source != "ical") {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete event",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFormDialog(
    title: String,
    calendarLists: List<CalendarListResponse>,
    initialTitle: String = "",
    initialDescription: String = "",
    initialLocation: String = "",
    initialStart: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
    initialEnd: String = "",
    initialAllDay: Boolean = false,
    initialColor: String = "",
    initialCalendarListId: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Boolean, String, Int?) -> Unit
) {
    var eventTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var location by rememberSaveable { mutableStateOf(initialLocation) }
    var startStr by rememberSaveable { mutableStateOf(initialStart) }
    var endStr by rememberSaveable { mutableStateOf(initialEnd) }
    var allDay by rememberSaveable { mutableStateOf(initialAllDay) }
    var colorStr by rememberSaveable { mutableStateOf(initialColor) }
    var selectedListId by rememberSaveable { mutableStateOf(initialCalendarListId) }
    var titleError by remember { mutableStateOf(false) }
    var calendarExpanded by remember { mutableStateOf(false) }

    val selectedCalendarName = calendarLists.find { it.id == selectedListId }?.name ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it; titleError = false },
                    label = { Text("Title *") },
                    isError = titleError,
                    supportingText = if (titleError) ({ Text("Required") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startStr,
                    onValueChange = { startStr = it },
                    label = { Text("Start (yyyy-MM-dd HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endStr,
                    onValueChange = { endStr = it },
                    label = { Text("End (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text("All day", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = colorStr,
                    onValueChange = { colorStr = it },
                    label = { Text("Color (hex, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (calendarLists.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = calendarExpanded,
                        onExpandedChange = { calendarExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCalendarName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Calendar List") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calendarExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = calendarExpanded,
                            onDismissRequest = { calendarExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { selectedListId = null; calendarExpanded = false }
                            )
                            calendarLists.forEach { cal ->
                                DropdownMenuItem(
                                    text = { Text(cal.name) },
                                    onClick = { selectedListId = cal.id; calendarExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (eventTitle.isBlank()) { titleError = true; return@TextButton }
                    onConfirm(eventTitle, description, location, startStr, endStr, allDay, colorStr, selectedListId)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyView(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No events yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap + to add an event",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDateTime(isoStr: String): String =
    runCatching {
        val dt = LocalDateTime.parse(isoStr.take(19))
        dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }.getOrDefault(isoStr)
