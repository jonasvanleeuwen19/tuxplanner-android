package com.tuxplanner.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuxplanner.app.BuildConfig
import com.tuxplanner.app.TuxPlannerApp
import com.tuxplanner.app.data.model.IcalFeedResponse

private enum class SettingsPage { Root, ServerAndAccounts, ExternalCalendar }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as TuxPlannerApp).container

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container.appPreferences, container.apiClient, container.authRepository) as T
        }
    )
    val externalViewModel: ExternalCalendarViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExternalCalendarViewModel(container.icalFeedRepository, container.calendarListRepository) as T
        }
    )

    var page by rememberSaveable { mutableStateOf(SettingsPage.Root) }
    val settingsState by settingsViewModel.uiState.collectAsState()
    val externalState by externalViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(settingsState.savedMessage) {
        settingsState.savedMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(externalState.error) {
        externalState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (page) {
                            SettingsPage.Root -> "Settings"
                            SettingsPage.ServerAndAccounts -> "Server and Accounts"
                            SettingsPage.ExternalCalendar -> "External Calendar"
                        }
                    )
                },
                navigationIcon = {
                    if (page != SettingsPage.Root) {
                        IconButton(onClick = { page = SettingsPage.Root }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (page) {
            SettingsPage.Root -> SettingsRootPage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onOpenServerAndAccounts = { page = SettingsPage.ServerAndAccounts },
                onOpenExternalCalendar = { page = SettingsPage.ExternalCalendar }
            )
            SettingsPage.ServerAndAccounts -> ServerAndAccountsPage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = settingsState,
                onSave = settingsViewModel::saveServerConfig,
                onLogout = { settingsViewModel.logout(onLogout) }
            )
            SettingsPage.ExternalCalendar -> ExternalCalendarPage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                uiState = externalState,
                onRefresh = externalViewModel::refresh,
                onAddFeed = externalViewModel::addFeed,
                onSync = externalViewModel::syncFeed,
                onToggleFeed = externalViewModel::toggleFeed,
                onDeleteFeed = externalViewModel::deleteFeed,
                onToggleListVisibility = externalViewModel::toggleCalendarListVisibility,
                onSetListColor = externalViewModel::updateCalendarColor
            )
        }
    }
}

@Composable
private fun SettingsRootPage(
    modifier: Modifier = Modifier,
    onOpenServerAndAccounts: () -> Unit,
    onOpenExternalCalendar: () -> Unit
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(onClick = onOpenServerAndAccounts, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.Dns, contentDescription = "Server and accounts")
                Spacer(Modifier.weight(1f))
                Text("Server and Accounts")
            }
        }
        Card(onClick = onOpenExternalCalendar, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.CloudSync, contentDescription = "External calendar")
                Spacer(Modifier.weight(1f))
                Text("External Calendar")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ServerAndAccountsPage(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    onSave: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var hostInput by rememberSaveable(state.host) { mutableStateOf(state.host) }
    var portInput by rememberSaveable(state.port) { mutableStateOf(state.port) }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Backend", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("Host") },
            placeholder = { Text("10.0.2.2") },
            singleLine = true,
            isError = state.hostError != null,
            supportingText = state.hostError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = portInput,
            onValueChange = { portInput = it },
            label = { Text("Port") },
            placeholder = { Text("8000") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.portError != null,
            supportingText = state.portError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onSave(hostInput, portInput) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Text("  Save")
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Text("  Logout")
        }
    }
}

@Composable
private fun ExternalCalendarPage(
    modifier: Modifier = Modifier,
    uiState: ExternalCalendarUiState,
    onRefresh: () -> Unit,
    onAddFeed: (String, String, String, String, String?, String?, () -> Unit) -> Unit,
    onSync: (Int) -> Unit,
    onToggleFeed: (IcalFeedResponse) -> Unit,
    onDeleteFeed: (Int) -> Unit,
    onToggleListVisibility: (Int, Boolean) -> Unit,
    onSetListColor: (Int, String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) { Text("Add Calendar") }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.feeds.isEmpty()) {
            Text("No external calendars added", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.feeds, key = { it.id }) { feed ->
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(feed.name, style = MaterialTheme.typography.titleSmall)
                            Text(feed.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onSync(feed.id) }) { Text(if (uiState.syncingFeedId == feed.id) "Syncing..." else "Sync") }
                                TextButton(onClick = { onToggleFeed(feed) }) {
                                    Icon(
                                        if (feed.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (feed.isActive) "Pause feed" else "Resume feed"
                                    )
                                    Text(if (feed.isActive) "Pause" else "Resume")
                                }
                                TextButton(onClick = { onDeleteFeed(feed.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete feed", tint = MaterialTheme.colorScheme.error)
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            uiState.calendarLists.filter { it.icalFeedId == feed.id }.forEach { list ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(list.caldavCalendarName ?: list.name, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { onSetListColor(list.id, nextColor(list.color)) }) { Text("Color") }
                                    Switch(
                                        checked = list.isVisible,
                                        onCheckedChange = { checked -> onToggleListVisibility(list.id, checked) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddFeedDialog(
            isAdding = uiState.isAdding,
            onDismiss = { showAdd = false },
            onConfirm = { name, url, color, type, user, pass ->
                onAddFeed(name, url, color, type, user, pass) { showAdd = false }
            }
        )
    }
}

@Composable
private fun AddFeedDialog(
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?, String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#3b82f6") }
    var type by rememberSaveable { mutableStateOf("ical") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add External Calendar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true)
                OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color (#hex)") }, singleLine = true)
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (ical/caldav)") }, singleLine = true)
                if (type == "caldav") {
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isAdding,
                onClick = {
                    onConfirm(
                        name.trim(),
                        url.trim(),
                        color.trim(),
                        type.trim().lowercase(),
                        username.trim().ifBlank { null },
                        password.ifBlank { null }
                    )
                }
            ) { Text(if (isAdding) "Adding..." else "Add & Sync") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun nextColor(current: String): String {
    val palette = listOf(
        "#3b82f6", "#8b5cf6", "#06b6d4", "#10b981",
        "#f59e0b", "#ef4444", "#ec4899", "#0ea5e9"
    )
    val idx = palette.indexOfFirst { it.equals(current, ignoreCase = true) }
    return palette[(if (idx < 0) 0 else idx + 1) % palette.size]
}
