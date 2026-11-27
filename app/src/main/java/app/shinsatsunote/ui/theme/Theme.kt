package app.shinsatsunote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val AppColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Surface,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Ink,
    secondary = Warm,
    onSecondary = Surface,
    secondaryContainer = WarmSoft,
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Seg,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
)

// カードと入口は 16dp(モックの border-radius:16px)、シートの上の角は 20dp
private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

/** ライトのみ(モックがライトのみのため)。ダークは公開後に検討する */
@Composable
fun ShinsatsuNoteTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** アプリ独自の色を取り出す: `AppTheme.colors.warm` */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}
