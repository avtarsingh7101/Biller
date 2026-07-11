package com.avtar.cabbilling.util

/**
 * Maps a caught [Throwable] to a short, safe, user-facing message. Business-rule
 * violations are raised as [IllegalStateException] with a friendly message and
 * are surfaced verbatim; anything else falls back to a generic line so raw
 * stack details never reach the UI.
 */
fun Throwable.toUserMessage(
    fallback: String = "Something went wrong. Please try again."
): String = when (this) {
    is IllegalStateException, is IllegalArgumentException -> message ?: fallback
    else -> fallback
}
