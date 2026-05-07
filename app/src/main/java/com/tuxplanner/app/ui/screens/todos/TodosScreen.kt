package com.tuxplanner.app.ui.screens.todos

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuxplanner.app.TuxPlannerApp
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.ui.common.MarkdownEditorField
import com.tuxplanner.app.ui.theme.PriorityHigh
import com.tuxplanner.app.ui.theme.PriorityLow
import com.tuxplanner.app.ui.theme.PriorityMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as TuxPlannerApp).container

    val viewModel: TodosViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TodosViewModel(
                    container.todoRepository,
                    container.todoListRepository,
                    container.taskSessionRepository,
                    container.eventRepository
                ) as T
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTodo by remember { mutableStateOf<TodoResponse?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoResponse?>(null) }

    // Load sessions when a todo is selected
    LaunchedEffect(selectedTodo) {
        selectedTodo?.let { viewModel.loadSessions(it.id) }
    }

    val filteredTodos = remember(uiState.todos, uiState.filter, uiState.selectedListId) {
        uiState.todos.filter { todo ->
            val listMatch = uiState.selectedListId == null || todo.todoListId == uiState.selectedListId
            val filterMatch = when (uiState.filter) {
                "pending" -> !todo.completed
                "completed" -> todo.completed
                else -> true
            }
            listMatch && filterMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadTodos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add todo")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status filter chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "pending" to "Pending", "completed" to "Completed").forEach { (value, label) ->
                    FilterChip(
                        selected = uiState.filter == value,
                        onClick = { viewModel.setFilter(value) },
                        label = { Text(label) }
                    )
                }
            }

            // Todo list filter chips
            if (uiState.todoLists.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedListId == null,
                        onClick = { viewModel.setListFilter(null) },
                        label = { Text("All Lists") }
                    )
                    uiState.todoLists.forEach { list ->
                        FilterChip(
                            selected = uiState.selectedListId == list.id,
                            onClick = {
                                viewModel.setListFilter(
                                    if (uiState.selectedListId == list.id) null else list.id
                                )
                            },
                            label = { Text(list.name) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    uiState.error != null -> ErrorView(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadTodos() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    filteredTodos.isEmpty() -> EmptyView(modifier = Modifier.align(Alignment.Center))
                    else -> TodosList(
                        todos = filteredTodos,
                        onTap = { selectedTodo = it },
                        onToggle = { viewModel.toggleComplete(it) },
                        onDelete = { viewModel.deleteTodo(it) }
                    )
                }
            }
        }
    }

    // Todo detail bottom sheet
    selectedTodo?.let { todo ->
        TodoDetailSheet(
            todo = todo,
            sessions = uiState.sessions,
            isLoadingSessions = uiState.isLoadingSessions,
            isEditable = todo.eventId == null || !uiState.externalEventIds.contains(todo.eventId),
            onDismiss = { selectedTodo = null },
            onEdit = {
                editingTodo = todo
                selectedTodo = null
                showEditDialog = true
            },
            onDelete = {
                viewModel.deleteTodo(todo.id)
                selectedTodo = null
            },
            onToggleComplete = { viewModel.toggleComplete(todo) },
            onAddSession = { start, end, note ->
                viewModel.addSession(todo.id, start, end, note)
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteSession(todo.id, sessionId)
            }
        )
    }

    // Add todo dialog
    if (showAddDialog) {
        TodoFormDialog(
            title = "New Todo",
            todoLists = uiState.todoLists,
            onDismiss = { showAddDialog = false },
            onConfirm = { todoTitle, description, priority, dueDate, todoListId ->
                viewModel.createTodo(todoTitle, description, priority, dueDate, todoListId, null)
                showAddDialog = false
            }
        )
    }

    // Edit todo dialog
    if (showEditDialog) {
        editingTodo?.let { todo ->
            TodoFormDialog(
                title = "Edit Todo",
                todoLists = uiState.todoLists,
                initialTitle = todo.title,
                initialDescription = todo.description ?: "",
                initialPriority = todo.priority,
                initialDueDate = todo.dueDate ?: "",
                initialTodoListId = todo.todoListId,
                onDismiss = { showEditDialog = false; editingTodo = null },
                onConfirm = { todoTitle, description, priority, dueDate, todoListId ->
                    viewModel.updateTodo(
                        id = todo.id,
                        title = todoTitle,
                        description = description.ifBlank { null },
                        priority = priority,
                        dueDate = dueDate.ifBlank { null },
                        todoListId = todoListId
                    )
                    showEditDialog = false
                    editingTodo = null
                }
            )
        }
    }
}

@Composable
private fun TodosList(
    todos: List<TodoResponse>,
    onTap: (TodoResponse) -> Unit,
    onToggle: (TodoResponse) -> Unit,
    onDelete: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(todos, key = { it.id }) { todo ->
            TodoCard(
                todo = todo,
                onTap = { onTap(todo) },
                onToggle = { onToggle(todo) },
                onDelete = { onDelete(todo.id) }
            )
        }
    }
}

@Composable
private fun TodoCard(
    todo: TodoResponse,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (todo.priority) {
        "high" -> PriorityHigh
        "low" -> PriorityLow
        else -> PriorityMedium
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
            IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (todo.completed) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (todo.completed) "Mark incomplete" else "Mark complete",
                    tint = if (todo.completed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                    color = if (todo.completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = todo.priority.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor
                    )
                    if (todo.dueDate != null) {
                        Text(
                            text = "Due: ${todo.dueDate.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (todo.sessionCount > 0) {
                        Text(
                            text = "${todo.sessionCount} session(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete todo",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoFormDialog(
    title: String,
    todoLists: List<com.tuxplanner.app.data.model.TodoListResponse>,
    initialTitle: String = "",
    initialDescription: String = "",
    initialPriority: String = "medium",
    initialDueDate: String = "",
    initialTodoListId: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Int?) -> Unit
) {
    var todoTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var priority by rememberSaveable { mutableStateOf(initialPriority) }
    var dueDate by rememberSaveable { mutableStateOf(initialDueDate) }
    var selectedListId by rememberSaveable { mutableStateOf(initialTodoListId) }
    var titleError by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var listExpanded by remember { mutableStateOf(false) }

    val priorities = listOf("low", "medium", "high")
    val selectedListName = todoLists.find { it.id == selectedListId }?.name ?: "None"

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
                        TextButton(
                            onClick = {
                                if (todoTitle.isBlank()) { titleError = true; return@TextButton }
                                onConfirm(todoTitle, description, priority, dueDate, selectedListId)
                            }
                        ) { Text("Save") }
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
                    value = todoTitle,
                    onValueChange = { todoTitle = it; titleError = false },
                    label = { Text("Title *") },
                    isError = titleError,
                    supportingText = if (titleError) ({ Text("Required") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MarkdownEditorField(
                    label = "Description",
                    value = description,
                    onValueChange = { description = it }
                )
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = priority.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        priorities.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.replaceFirstChar { it.uppercase() }) },
                                onClick = { priority = p; priorityExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due date (yyyy-MM-dd HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (todoLists.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = listExpanded,
                        onExpandedChange = { listExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedListName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Todo List") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = listExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = listExpanded,
                            onDismissRequest = { listExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { selectedListId = null; listExpanded = false }
                            )
                            todoLists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name) },
                                    onClick = { selectedListId = list.id; listExpanded = false }
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
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No todos yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap + to add a todo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
