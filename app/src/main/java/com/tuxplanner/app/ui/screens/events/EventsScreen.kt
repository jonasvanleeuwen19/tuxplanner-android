package com.tuxplanner.app.ui.screens.events

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuxplanner.app.TuxPlannerApp
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.EventUpdate
import com.tuxplanner.app.ui.common.DateTimePickerField
import com.tuxplanner.app.ui.common.MarkdownEditorField
import com.tuxplanner.app.ui.common.OsmLocationService
import com.tuxplanner.app.ui.common.OsmSuggestion
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(onNavigateToCalendarLists: () -> Unit = {}) {
    val context = LocalContext.current
    val container = (context.applicationContext as TuxPlannerApp).container

    val viewModel: EventsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EventsViewModel(container.eventRepository, container.calendarListRepository, container.todoRepository) as T
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<EventResponse?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventResponse?>(null) }

    val filteredEvents = remember(uiState.events, uiState.filterCalendarListId, uiState.calendarLists) {
        val visibleListIds = uiState.calendarLists.filter { it.isVisible }.map { it.id }.toSet()
        uiState.events.filter { event ->
            val visibleMatch = event.calendarListId == null || visibleListIds.contains(event.calendarListId)
            val filterMatch = uiState.filterCalendarListId == null || event.calendarListId == uiState.filterCalendarListId
            visibleMatch && filterMatch
        }
    }

    LaunchedEffect(selectedEvent?.id) {
        selectedEvent?.id?.let { viewModel.loadTasksForEvent(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // View type toggles
                    IconButton(
                        onClick = { viewModel.setViewType(CalendarViewType.Month) }
                    ) {
                        Icon(
                            Icons.Default.CalendarViewMonth,
                            contentDescription = "Month view",
                            tint = if (uiState.viewType == CalendarViewType.Month)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setViewType(CalendarViewType.List) }
                    ) {
                        Icon(
                            Icons.Default.ViewAgenda,
                            contentDescription = "List view",
                            tint = if (uiState.viewType == CalendarViewType.List)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterCalendarListId == null,
                        onClick = { viewModel.setCalendarListFilter(null) },
                        label = { Text("All") }
                    )
                    uiState.calendarLists.filter { it.isVisible }.forEach { cal ->
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

            // Main content area
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    uiState.error != null -> ErrorView(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadEvents() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> when (uiState.viewType) {
                        CalendarViewType.Month -> MonthCalendarView(
                            events = filteredEvents,
                            calendarLists = uiState.calendarLists,
                            displayedYearMonth = uiState.displayedYearMonth,
                            selectedDate = uiState.selectedDate,
                            onPrevMonth = { viewModel.prevMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onSelectDate = { viewModel.selectDate(it) },
                            onTapEvent = { selectedEvent = it },
                            onDeleteEvent = { viewModel.deleteEvent(it) }
                        )
                        CalendarViewType.List -> AgendaCalendarView(
                            events = filteredEvents,
                            calendarLists = uiState.calendarLists,
                            onTapEvent = { selectedEvent = it },
                            onDeleteEvent = { viewModel.deleteEvent(it) }
                        )
                    }
                }
            }
        }
    }

    // Event detail bottom sheet
    selectedEvent?.let { event ->
        EventDetailSheet(
            event = event,
            calendarLists = uiState.calendarLists,
            linkedTodos = uiState.linkedTasks,
            allTodos = uiState.allTodos,
            onDismiss = { selectedEvent = null },
            onEdit = {
                editingEvent = event
                selectedEvent = null
                showEditDialog = true
            },
            onDelete = {
                viewModel.deleteEvent(event.id)
                selectedEvent = null
            },
            onLinkTodo = { todoId -> viewModel.linkTaskToEvent(todoId, event.id) },
            onUnlinkTodo = { todoId -> viewModel.unlinkTask(todoId, event.id) }
        )
    }

    // Add event dialog
    if (showAddDialog) {
        EventFormDialog(
            title = "New Event",
            calendarLists = uiState.calendarLists,
            initialStart = uiState.selectedDate.atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
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
                    val start = runCatching {
                        LocalDateTime.parse(startStr, userFormatter).format(isoFormatter)
                    }.getOrNull()
                    val end = runCatching {
                        LocalDateTime.parse(endStr, userFormatter).format(isoFormatter)
                    }.getOrNull()
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            if (eventTitle.isBlank()) { titleError = true; return@TextButton }
                            onConfirm(
                                eventTitle, description, location,
                                startStr, endStr, allDay, colorStr, selectedListId
                            )
                        }) { Text("Save") }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
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
                MarkdownEditorField(
                    label = "Description",
                    value = description,
                    onValueChange = { description = it }
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                var showLocationSearch by remember { mutableStateOf(false) }
                TextButton(onClick = { showLocationSearch = true }) { Text("Search location (OpenStreetMap)") }
                DateTimePickerField(
                    label = "Start",
                    value = startStr,
                    onValueChange = { startStr = it }
                )
                DateTimePickerField(
                    label = "End (optional)",
                    value = endStr,
                    onValueChange = { endStr = it },
                    allowClear = true
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
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = calendarExpanded)
                            },
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
                if (showLocationSearch) {
                    LocationSearchDialog(
                        onDismiss = { showLocationSearch = false },
                        onSelect = {
                            location = it.displayName
                            showLocationSearch = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationSearchDialog(
    onDismiss: () -> Unit,
    onSelect: (OsmSuggestion) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<OsmSuggestion>()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Search location") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Address or place") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        loading = true
                        scope.launch {
                            results = OsmLocationService.search(query)
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Search") }

                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        results.forEach { item ->
                            TextButton(onClick = { onSelect(item) }, modifier = Modifier.fillMaxWidth()) {
                                Text(item.displayName)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
