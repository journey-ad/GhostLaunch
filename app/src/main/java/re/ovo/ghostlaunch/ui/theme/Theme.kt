package re.ovo.ghostlaunch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EinkColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF333333),
    onSecondary = Color.White,
    tertiary = Color(0xFF555555),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color.Black,
    outline = Color(0xFF888888)
)

@Composable
fun GhostLaunchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EinkColorScheme,
        typography = Typography,
        content = content
    )
}
