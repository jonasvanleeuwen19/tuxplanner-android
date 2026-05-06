package com.tuxplanner.app.data.network

import com.tuxplanner.app.data.model.CalendarListCreate
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.CalendarListUpdate
import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.EventUpdate
import com.tuxplanner.app.data.model.MessageResponse
import com.tuxplanner.app.data.model.SetupRequest
import com.tuxplanner.app.data.model.SetupStatusResponse
import com.tuxplanner.app.data.model.TaskSessionCreate
import com.tuxplanner.app.data.model.TaskSessionResponse
import com.tuxplanner.app.data.model.TaskSessionUpdate
import com.tuxplanner.app.data.model.TodoCreate
import com.tuxplanner.app.data.model.TodoListCreate
import com.tuxplanner.app.data.model.TodoListResponse
import com.tuxplanner.app.data.model.TodoListUpdate
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.data.model.TodoUpdate
import com.tuxplanner.app.data.model.UserInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TuxPlannerApiService {

    // ── Auth ─────────────────────────────────────────────────

    @GET("api/auth/setup-status")
    suspend fun getSetupStatus(): Response<SetupStatusResponse>

    @POST("api/auth/setup")
    suspend fun setup(@Body body: SetupRequest): Response<MessageResponse>

    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): Response<MessageResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<UserInfo>

    // ── Calendar Lists ────────────────────────────────────────

    @GET("api/calendar-lists/")
    suspend fun getCalendarLists(): Response<List<CalendarListResponse>>

    @POST("api/calendar-lists/")
    suspend fun createCalendarList(@Body body: CalendarListCreate): Response<CalendarListResponse>

    @PUT("api/calendar-lists/{id}")
    suspend fun updateCalendarList(
        @Path("id") id: Int,
        @Body body: CalendarListUpdate
    ): Response<CalendarListResponse>

    @DELETE("api/calendar-lists/{id}")
    suspend fun deleteCalendarList(@Path("id") id: Int): Response<Unit>

    // ── Events ────────────────────────────────────────────────

    @GET("api/events/")
    suspend fun getEvents(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): Response<List<EventResponse>>

    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Response<EventResponse>

    @POST("api/events/")
    suspend fun createEvent(@Body event: EventCreate): Response<EventResponse>

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Int,
        @Body event: EventUpdate
    ): Response<EventResponse>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: Int): Response<Unit>

    // ── Todos ─────────────────────────────────────────────────

    @GET("api/todos/")
    suspend fun getTodos(
        @Query("todo_list_id") todoListId: Int? = null,
        @Query("event_id") eventId: Int? = null
    ): Response<List<TodoResponse>>

    @GET("api/todos/{id}")
    suspend fun getTodo(@Path("id") id: Int): Response<TodoResponse>

    @POST("api/todos/")
    suspend fun createTodo(@Body todo: TodoCreate): Response<TodoResponse>

    @PUT("api/todos/{id}")
    suspend fun updateTodo(
        @Path("id") id: Int,
        @Body todo: TodoUpdate
    ): Response<TodoResponse>

    @DELETE("api/todos/{id}")
    suspend fun deleteTodo(@Path("id") id: Int): Response<Unit>

    // ── Todo Lists ────────────────────────────────────────────

    @GET("api/todo-lists/")
    suspend fun getTodoLists(): Response<List<TodoListResponse>>

    @POST("api/todo-lists/")
    suspend fun createTodoList(@Body body: TodoListCreate): Response<TodoListResponse>

    @PUT("api/todo-lists/{id}")
    suspend fun updateTodoList(
        @Path("id") id: Int,
        @Body body: TodoListUpdate
    ): Response<TodoListResponse>

    @DELETE("api/todo-lists/{id}")
    suspend fun deleteTodoList(@Path("id") id: Int): Response<Unit>

    // ── Task Sessions ─────────────────────────────────────────

    @GET("api/todos/{todoId}/sessions/")
    suspend fun getTaskSessions(@Path("todoId") todoId: Int): Response<List<TaskSessionResponse>>

    @POST("api/todos/{todoId}/sessions/")
    suspend fun createTaskSession(
        @Path("todoId") todoId: Int,
        @Body body: TaskSessionCreate
    ): Response<TaskSessionResponse>

    @PUT("api/todos/{todoId}/sessions/{sessionId}")
    suspend fun updateTaskSession(
        @Path("todoId") todoId: Int,
        @Path("sessionId") sessionId: Int,
        @Body body: TaskSessionUpdate
    ): Response<TaskSessionResponse>

    @DELETE("api/todos/{todoId}/sessions/{sessionId}")
    suspend fun deleteTaskSession(
        @Path("todoId") todoId: Int,
        @Path("sessionId") sessionId: Int
    ): Response<Unit>
}
