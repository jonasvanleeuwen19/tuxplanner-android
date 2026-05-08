package com.tuxplanner.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.CalendarListUpdate
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.IcalFeedCreate
import com.tuxplanner.app.data.model.IcalFeedResponse
import com.tuxplanner.app.data.model.IcalFeedUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.CalendarListRepository
import com.tuxplanner.app.data.repository.IcalFeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExternalCalendarUiState(
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val syncingFeedId: Int? = null,
    val feeds: List<IcalFeedResponse> = emptyList(),
    val calendarLists: List<CalendarListResponse> = emptyList(),
    val error: String? = null
)

class ExternalCalendarViewModel(
    private val feedRepository: IcalFeedRepository,
    private val calendarListRepository: CalendarListRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExternalCalendarUiState())
    val uiState: StateFlow<ExternalCalendarUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val feedsResult = feedRepository.getFeeds()
            val listsResult = calendarListRepository.getCalendarLists()
            val lists = if (listsResult is ApiResult.Success) listsResult.data else _uiState.value.calendarLists
            when (feedsResult) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    feeds = feedsResult.data,
                    calendarLists = lists,
                    error = null
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    calendarLists = lists,
                    error = feedsResult.message
                )
            }
        }
    }

    fun addFeed(
        name: String,
        url: String,
        color: String,
        feedType: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true, error = null)
            val normalizedType = if (feedType.equals("webcal", ignoreCase = true)) "ical" else feedType.lowercase()
            val normalizedUrl = if (url.startsWith("webcal://")) {
                "https://${url.removePrefix("webcal://")}"
            } else url
            when (val result = feedRepository.createFeed(IcalFeedCreate(
                name = name,
                url = normalizedUrl,
                color = color,
                feedType = normalizedType
            ))) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isAdding = false)
                    refresh()
                    onDone()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    error = result.message
                )
            }
        }
    }

    fun syncFeed(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncingFeedId = id)
            when (feedRepository.syncFeed(id)) {
                is ApiResult.Success -> refresh()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(syncingFeedId = null)
            }
        }
    }

    fun toggleFeed(feed: IcalFeedResponse) {
        viewModelScope.launch {
            when (val result = feedRepository.updateFeed(feed.id, IcalFeedUpdate(isActive = !feed.isActive))) {
                is ApiResult.Success -> refresh()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun deleteFeed(feedId: Int) {
        viewModelScope.launch {
            when (val result = feedRepository.deleteFeed(feedId)) {
                is ApiResult.Success -> refresh()
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun updateFeedName(feedId: Int, name: String) {
        viewModelScope.launch {
            feedRepository.updateFeed(feedId, IcalFeedUpdate(name = name))
            refresh()
        }
    }

    fun updateCalendarColor(calendarListId: Int, color: String) {
        viewModelScope.launch {
            calendarListRepository.updateCalendarList(
                calendarListId,
                CalendarListUpdate(color = color)
            )
            refresh()
        }
    }

    fun toggleCalendarListVisibility(listId: Int, isVisible: Boolean) {
        viewModelScope.launch {
            calendarListRepository.updateCalendarList(
                listId,
                CalendarListUpdate(isVisible = isVisible)
            )
            refresh()
        }
    }
}
