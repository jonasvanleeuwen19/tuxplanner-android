package com.tuxplanner.app.ui.common

/** Returns an error message if [host] is invalid, or null if it is valid. */
fun validateHost(host: String): String? = when {
    host.isBlank() -> "Host cannot be empty"
    host.contains(' ') -> "Host must not contain spaces"
    !host.matches(Regex("^[a-zA-Z0-9._\\-]+$")) -> "Host contains invalid characters"
    else -> null
}

/** Returns an error message if [port] is invalid, or null if it is valid. */
fun validatePort(port: String): String? {
    val portNum = port.toIntOrNull()
    return when {
        port.isBlank() -> "Port cannot be empty"
        portNum == null -> "Port must be a number"
        portNum < 1 || portNum > 65535 -> "Port must be between 1 and 65535"
        else -> null
    }
}
