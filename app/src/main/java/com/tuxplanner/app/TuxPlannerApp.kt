package com.tuxplanner.app

import android.app.Application
import com.tuxplanner.app.data.network.ApiClient
import com.tuxplanner.app.data.preferences.AppPreferences
import com.tuxplanner.app.data.repository.AuthRepository
import com.tuxplanner.app.data.repository.CalendarListRepository
import com.tuxplanner.app.data.repository.EventRepository
import com.tuxplanner.app.data.repository.TaskSessionRepository
import com.tuxplanner.app.data.repository.TodoListRepository
import com.tuxplanner.app.data.repository.TodoRepository

class AppContainer(app: Application) {
    val appPreferences = AppPreferences(app)
    val apiClient = ApiClient(appPreferences, app)
    val authRepository = AuthRepository(apiClient, appPreferences)
    val eventRepository = EventRepository(apiClient)
    val todoRepository = TodoRepository(apiClient)
    val calendarListRepository = CalendarListRepository(apiClient)
    val todoListRepository = TodoListRepository(apiClient)
    val taskSessionRepository = TaskSessionRepository(apiClient)
}

class TuxPlannerApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
