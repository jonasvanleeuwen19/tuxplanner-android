package com.tuxplanner.app.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.EventResponse
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

// ── Shared helpers ────────────────────────────────────────────────────────────

internal fun formatEventDateTime(isoStart: String): String = runCatching {
    val ldt = java.time.LocalDateTime.parse(isoStart.take(19))
    ldt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
}.getOrDefault(isoStart.take(16))

internal fun eventColor(event: EventResponse, calendarLists: List<CalendarListResponse>): Color {
    val hex = calendarLists.find { it.id == event.calendarListId }?.color
        ?: event.color
        ?: "#1565C0"
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF1565C0))
}

/** Single event row card, shared across all three calendar views. */
@Composable
internal fun EventCard(
    event: EventResponse,
    calendarLists: List<CalendarListResponse>,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = eventColor(event, calendarLists)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onTap() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (event.allDay) "All day" else formatEventDateTime(event.start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.source != "ical") {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Month view ────────────────────────────────────────────────────────────────

@Composable
fun MonthCalendarView(
    events: List<EventResponse>,
    calendarLists: List<CalendarListResponse>,
    displayedYearMonth: YearMonth,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onTapEvent: (EventResponse) -> Unit,
    onDeleteEvent: (Int) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = displayedYearMonth.atDay(1)
    val daysInMonth = displayedYearMonth.lengthOfMonth()
    // ISO day-of-week: Monday=1 … Sunday=7, we want offset so Monday is column 0
    val startOffset = (firstDay.dayOfWeek.value - 1) // 0 = Mon, 6 = Sun

    val eventsByDate = remember(events) {
        events.groupBy {
            runCatching { LocalDate.parse(it.start.take(10)) }.getOrNull()
        }.filterKeys { it != null }.mapKeys { it.key!! }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Month navigation ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = displayedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        // ── Day-of-week header ──
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Calendar grid ──
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7 // ceiling division
        (0 until rows).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                (0 until 7).forEach { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = displayedYearMonth.atDay(dayNumber)
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        val dayEvents = eventsByDate[date] ?: emptyList()
                        DayCell(
                            day = dayNumber,
                            isToday = isToday,
                            isSelected = isSelected,
                            events = dayEvents,
                            calendarLists = calendarLists,
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Selected-day events ──
        val selectedEvents = (eventsByDate[selectedDate] ?: emptyList())
            .sortedBy { it.start }
        if (selectedEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(selectedEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        calendarLists = calendarLists,
                        onTap = { onTapEvent(event) },
                        onDelete = { onDeleteEvent(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    events: List<EventResponse>,
    calendarLists: List<CalendarListResponse>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            if (events.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    events.take(3).forEach { event ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(eventColor(event, calendarLists))
                        )
                    }
                }
            }
        }
    }
}

// ── Week view ─────────────────────────────────────────────────────────────────

@Composable
fun WeekCalendarView(
    events: List<EventResponse>,
    calendarLists: List<CalendarListResponse>,
    weekStart: LocalDate,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTapEvent: (EventResponse) -> Unit,
    onDeleteEvent: (Int) -> Unit
) {
    val today = LocalDate.now()
    val weekEnd = weekStart.plusDays(6)

    val eventsByDate = remember(events) {
        events.groupBy {
            runCatching { LocalDate.parse(it.start.take(10)) }.getOrNull()
        }.filterKeys { it != null }.mapKeys { it.key!! }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Week navigation ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onPrevWeek) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
            }
            val shortDate = DateTimeFormatter.ofPattern("d MMM")
            Text(
                text = "${weekStart.format(shortDate)} – ${weekEnd.format(shortDate)} ${weekEnd.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
            }
        }

        // ── 7-day strip ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (0..6).forEach { offset ->
                val date = weekStart.plusDays(offset.toLong())
                val isToday = date == today
                val hasEvents = (eventsByDate[date]?.isNotEmpty()) == true
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (hasEvents) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Events for each day of the week ──
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            (0..6).forEach { offset ->
                val date = weekStart.plusDays(offset.toLong())
                val dayEvents = (eventsByDate[date] ?: emptyList()).sortedBy { it.start }
                item(key = "header_$date") {
                    DaySectionHeader(date = date, today = today, eventCount = dayEvents.size)
                }
                items(dayEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        calendarLists = calendarLists,
                        onTap = { onTapEvent(event) },
                        onDelete = { onDeleteEvent(event.id) }
                    )
                }
                if (dayEvents.isEmpty()) {
                    item(key = "empty_$date") {
                        Text(
                            text = "No events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── List / Agenda view ────────────────────────────────────────────────────────

@Composable
fun AgendaCalendarView(
    events: List<EventResponse>,
    calendarLists: List<CalendarListResponse>,
    onTapEvent: (EventResponse) -> Unit,
    onDeleteEvent: (Int) -> Unit
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No events",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val today = LocalDate.now()
    val eventsByDate = remember(events) {
        events
            .groupBy {
                runCatching { LocalDate.parse(it.start.take(10)) }.getOrNull()
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .toSortedMap()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        eventsByDate.forEach { (date, dayEvents) ->
            item(key = "header_$date") {
                DaySectionHeader(date = date, today = today, eventCount = dayEvents.size)
            }
            items(dayEvents.sortedBy { it.start }, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    calendarLists = calendarLists,
                    onTap = { onTapEvent(event) },
                    onDelete = { onDeleteEvent(event.id) }
                )
            }
        }
    }
}

// ── Shared section header ─────────────────────────────────────────────────────

@Composable
private fun DaySectionHeader(date: LocalDate, today: LocalDate, eventCount: Int) {
    val isToday = date == today
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isToday) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (eventCount > 0) "$eventCount event${if (eventCount > 1) "s" else ""}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
