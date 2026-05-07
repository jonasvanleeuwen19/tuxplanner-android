package com.tuxplanner.app.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DateTimeInputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val TimeInputFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun DateTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false
) {
    val context = LocalContext.current

    fun openDateTimePicker() {
        val initial = runCatching { LocalDateTime.parse(value, DateTimeInputFormatter) }
            .getOrElse { LocalDateTime.now() }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = LocalDateTime.of(year, month + 1, dayOfMonth, hour, minute)
                        onValueChange(picked.format(DateTimeInputFormatter))
                    },
                    initial.hour,
                    initial.minute,
                    true
                ).show()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            if (allowClear && value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear date")
                }
            } else {
                IconButton(onClick = { openDateTimePicker() }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun TimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openTimePicker() {
        val initial = runCatching { LocalTime.parse(value, TimeInputFormatter) }
            .getOrElse { LocalTime.of(9, 0) }

        TimePickerDialog(
            context,
            { _, hour, minute ->
                onValueChange(LocalTime.of(hour, minute).format(TimeInputFormatter))
            },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { openTimePicker() }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick time")
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
