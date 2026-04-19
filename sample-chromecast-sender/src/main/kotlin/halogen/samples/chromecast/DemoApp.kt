package halogen.samples.chromecast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import halogen.chromecast.HalogenCastSession
import halogen.engine.HalogenEngine
import kotlinx.coroutines.launch

@Composable
fun DemoApp(engine: HalogenEngine, handoff: HalogenCastSession) {
    val active by engine.activeTheme.collectAsState()
    val state by handoff.state.collectAsState()
    val scope = rememberCoroutineScope()

    val background = active?.neutralLight?.toColor() ?: MaterialTheme.colorScheme.background
    val onBackground = active?.neutralDark?.toColor() ?: MaterialTheme.colorScheme.onBackground

    Box(Modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                "Halogen Cast Demo",
                color = onBackground,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a swatch to apply it locally; if connected to a Chromecast, it's migrated to the receiver automatically.",
                color = onBackground.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            CastButtonPlaceholder(onBackground)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(DemoThemes.ALL) { named ->
                    SwatchCard(named) {
                        scope.launch { engine.apply(named.name, named.spec) }
                    }
                }
            }
        }
        HandoffToast(state = state, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun SwatchCard(named: DemoThemes.Named, onClick: () -> Unit) {
    val spec = named.spec
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(spec.primary)
                Swatch(spec.secondary)
                Swatch(spec.tertiary)
                Swatch(spec.error)
            }
            Spacer(Modifier.height(12.dp))
            Text(named.name, fontWeight = FontWeight.Medium)
            Text("primary ${spec.primary}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Swatch(hex: String) {
    Box(
        Modifier
            .size(28.dp)
            .background(hex.toColor(), RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun CastButtonPlaceholder(onBackground: Color) {
    // The real app uses MediaRouteButton wired via CastButtonFactory in an AndroidView.
    // Kept minimal here to avoid pulling XML layout bindings into the demo.
    Text(
        "Tap the platform Cast button in your status bar / app bar to start a session.",
        color = onBackground.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodySmall,
    )
}

internal fun String.toColor(): Color {
    val trimmed = removePrefix("#")
    val rgb = trimmed.toLong(16)
    val r = ((rgb shr 16) and 0xFF).toInt()
    val g = ((rgb shr 8) and 0xFF).toInt()
    val b = (rgb and 0xFF).toInt()
    return Color(r, g, b)
}
