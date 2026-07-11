package com.avtar.cabbilling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Reusable brand gradients derived from the active color scheme. */
object AppGradients {

    /** Diagonal primary → secondary sweep used on hero surfaces and CTAs. */
    @Composable
    @ReadOnlyComposable
    fun brand(): Brush {
        val cs = MaterialTheme.colorScheme
        return Brush.linearGradient(
            colors = listOf(cs.primary, cs.secondary),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
}
