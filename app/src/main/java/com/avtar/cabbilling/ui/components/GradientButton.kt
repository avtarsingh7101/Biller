package com.avtar.cabbilling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avtar.cabbilling.ui.theme.AppGradients

/** Full-width gradient call-to-action button with optional icon and loading state. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val brand = AppGradients.brand()
    val content = MaterialTheme.colorScheme.onPrimary
    val disabledContent = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) Modifier.background(brand)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp), color = content)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = if (enabled) content else disabledContent)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (enabled) content else disabledContent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
