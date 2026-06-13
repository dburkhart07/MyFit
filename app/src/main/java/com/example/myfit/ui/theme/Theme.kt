package com.example.myfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MyFitColorScheme = darkColorScheme(
    // Primary action = the white "Log in" / "Submit" buttons with dark text.
    primary = White,
    onPrimary = Gray900,

    // Secondary = the teal accent used for links ("Sign up", "Edit", "Back").
    secondary = Teal400,
    onSecondary = Gray900,

    // Tertiary = the cyan that pairs with teal in the logo / success gradients.
    tertiary = Cyan500,
    onTertiary = White,

    // App background and the card/dialog surfaces sitting on top of it.
    background = Gray950,
    onBackground = White,
    surface = Gray900,
    onSurface = White,

    // Inputs and secondary buttons (gray-800) plus their muted label text (gray-400).
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,

    // Elevated surface containers (dialogs, nav) step up through the gray scale.
    surfaceContainerLowest = Gray950,
    surfaceContainerLow = Gray900,
    surfaceContainer = Gray900,
    surfaceContainerHigh = Gray800,
    surfaceContainerHighest = Gray700,

    // Borders / outlines.
    outline = Gray700,
    outlineVariant = Gray600,

    // Destructive / error.
    error = Red400,
    onError = Gray900,
    errorContainer = Red600,
    onErrorContainer = White,
)

@Composable
fun MyFitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MyFitColorScheme,
        typography = Typography,
        content = content
    )
}