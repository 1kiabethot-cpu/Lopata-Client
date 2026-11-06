package com.lopata.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val Pink = Color(0xFFFF35B8)
val PinkDim = Color(0xFF6B1752)
val Panel = Color(0xFF120A12)
val Soft = Color(0xFFE8DCE8)

private val scheme = darkColorScheme(
    primary = Pink, onPrimary = Color.Black, secondary = Pink,
    background = Color.Black, onBackground = Soft,
    surface = Panel, onSurface = Soft, surfaceVariant = Panel, outline = PinkDim
)

@Composable
fun LopataTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)

@Composable
fun NeonCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().background(Panel, shape)
            .border(1.dp, Pink.copy(alpha = 0.55f), shape).padding(16.dp),
        content = content
    )
}

@Composable
fun NeonButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    Button(
        onClick = onClick, enabled = enabled, interactionSource = src,
        modifier = modifier.scale(s).heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Pink, contentColor = Color.Black,
            disabledContainerColor = PinkDim, disabledContentColor = Color.Gray)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Pink), colors = ButtonDefaults.outlinedButtonColors(contentColor = Pink)
    ) { Text(text) }
}

@Composable
fun H(text: String) = Text(
    text, color = Pink, fontWeight = FontWeight.Bold,
    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp)
)
