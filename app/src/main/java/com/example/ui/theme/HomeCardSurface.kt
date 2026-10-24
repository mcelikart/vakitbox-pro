package com.example.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The weather panel's frosted surface, shared by the home screen cards. */
@Composable
fun homeCardShape(): Shape {
    val theme = LocalAppTheme.current
    return if (theme != AppTheme.CLASSIC_EMERALD && theme.spec.isGlass) {
        theme.cardShape
    } else {
        RoundedCornerShape(24.dp)
    }
}

@Composable
fun Modifier.homeCardSurface(goldFrame: Boolean = false): Modifier {
    val theme = LocalAppTheme.current
    val shape = homeCardShape()
    val gold = LocalThemeColors.current.goldMotifColor
    val frame = if (goldFrame) {
        Modifier.border(
            BorderStroke(1.2.dp, Brush.linearGradient(listOf(gold.copy(alpha = 0.85f), gold.copy(alpha = 0.4f), gold.copy(alpha = 0.8f)))),
            shape
        )
    } else if (theme != AppTheme.CLASSIC_EMERALD && theme.spec.isGlass) {
        Modifier.glassmorphicCard(theme = theme, shape = shape, border = theme.buttonBorder)
    } else {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    }
    return this.frostedGlassBackdrop(shape).then(frame)
}

@Composable
fun HomeCardSurface(
    modifier: Modifier = Modifier,
    goldFrame: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.homeCardSurface(goldFrame),
        shape = homeCardShape(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(0.dp),
        content = content
    )
}
