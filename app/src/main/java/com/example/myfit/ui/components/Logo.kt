package com.example.myfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myfit.ui.theme.MyFitTheme

/**
 * 1. What: Circular gradient app badge with the "MyFit" wordmark centered inside.
 * 2. Who: Used at the top of the auth screens (LoginScreen, SignupScreen).
 * 3. When: Always shown on those screens; [modifier] lets callers size or position it.
 */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(colors = listOf(colors.secondary, colors.tertiary))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "MyFit",
            color = colors.onTertiary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 1. What: Design-time preview of the app logo badge.
 * 2. Who: Called by Android Studio's Compose preview renderer.
 * 3. When: Rendered at design time in the IDE; never runs in the shipped app.
 */
@Preview(showBackground = true)
@Composable
private fun LogoPreview() {
    MyFitTheme {
        Logo()
    }
}
