package me.mmckenna.halogen.sample.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import halogen.engine.HalogenEngine

data class WeatherCondition(
    val name: String,
    val hint: String,
    val emoji: String,
    val temp: String,
    val description: String,
    val humidity: String,
    val wind: String,
    val forecast: String,
)

internal val weatherConditions = listOf(
    WeatherCondition(
        name = "Sunrise",
        hint = "early morning sunrise. The sky is transitioning from darkness to light. " +
            "Think of the warmth of first light hitting the horizon, the glow before " +
            "the sun fully appears. Hopeful, gentle, the start of something new. " +
            "The feeling of waking up to a beautiful day.",
        emoji = "\uD83C\uDF05",
        temp = "62\u00B0F",
        description = "Clear skies with a golden sunrise",
        humidity = "65%",
        wind = "5 mph",
        forecast = "Beautiful start to the day",
    ),
    WeatherCondition(
        name = "Sunny",
        hint = "bright midday sunshine. Cloudless sky overhead, everything bathed in " +
            "warm light. The feeling of a perfect summer afternoon \u2014 optimistic, " +
            "energetic, cheerful. Picnics in the park, kids playing outside, " +
            "sunglasses and lemonade.",
        emoji = "\u2600\uFE0F",
        temp = "78\u00B0F",
        description = "Clear and sunny",
        humidity = "40%",
        wind = "8 mph",
        forecast = "Perfect weather all afternoon",
    ),
    WeatherCondition(
        name = "Cloudy",
        hint = "overcast day with a thick blanket of clouds. No sun visible. " +
            "The world feels muted, hushed, contemplative. Like looking out a " +
            "window on a quiet afternoon with a cup of tea. Not sad, just calm " +
            "and understated. Everything feels softer and less contrasty.",
        emoji = "\u2601\uFE0F",
        temp = "65\u00B0F",
        description = "Overcast with thick clouds",
        humidity = "70%",
        wind = "12 mph",
        forecast = "Clouds clearing by evening",
    ),
    WeatherCondition(
        name = "Rainy",
        hint = "steady rainfall. Puddles forming on sidewalks, the sound of rain " +
            "on windows. The world seen through a wet windshield. Moody but not " +
            "threatening \u2014 cozy if you're inside, refreshing if you embrace it. " +
            "Think of the cool, damp feeling of a rainy autumn afternoon.",
        emoji = "\uD83C\uDF27\uFE0F",
        temp = "55\u00B0F",
        description = "Steady rain throughout",
        humidity = "90%",
        wind = "15 mph",
        forecast = "Rain expected until midnight",
    ),
    WeatherCondition(
        name = "Storm",
        hint = "intense thunderstorm. Lightning illuminating dark skies, thunder " +
            "shaking the windows. Dramatic, powerful, electric. The kind of storm " +
            "that makes you stop what you're doing and watch from the window. " +
            "Raw energy and nature at its most dramatic.",
        emoji = "\u26C8\uFE0F",
        temp = "52\u00B0F",
        description = "Severe thunderstorms",
        humidity = "95%",
        wind = "25 mph",
        forecast = "Storm warning until 9 PM",
    ),
    WeatherCondition(
        name = "Sunset",
        hint = "golden hour fading into dusk. The sun is low on the horizon, " +
            "casting long shadows. The sky is a painting that changes every minute. " +
            "Warm, romantic, nostalgic \u2014 the feeling of ending a perfect day. " +
            "Everything is bathed in that magical warm glow.",
        emoji = "\uD83C\uDF07",
        temp = "72\u00B0F",
        description = "Beautiful sunset colors",
        humidity = "45%",
        wind = "6 mph",
        forecast = "Clear skies tonight",
    ),
    WeatherCondition(
        name = "Night",
        hint = "clear night sky. Stars visible overhead, the moon casting a cool glow. " +
            "The world is quiet and still. The feeling of looking up at the cosmos " +
            "from your backyard. Peaceful, mysterious, vast. The coolness of " +
            "night air after a warm day.",
        emoji = "\uD83C\uDF19",
        temp = "58\u00B0F",
        description = "Clear night sky",
        humidity = "55%",
        wind = "3 mph",
        forecast = "Cool and clear overnight",
    ),
    WeatherCondition(
        name = "Snow",
        hint = "fresh snowfall covering everything. The world is hushed and pristine. " +
            "Footprints in fresh powder, breath visible in the air. The crispness " +
            "of a winter morning, the way snow makes everything look clean and new. " +
            "Cold but beautiful, like a snow globe come to life.",
        emoji = "\u2744\uFE0F",
        temp = "28\u00B0F",
        description = "Heavy snowfall",
        humidity = "80%",
        wind = "18 mph",
        forecast = "4-6 inches expected",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    state: WeatherState,
    engine: HalogenEngine,
    modifier: Modifier = Modifier,
) {
    val uiState by state.uiState.collectAsState()
    val weather = weatherConditions[uiState.selectedIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text("Weather") })

        // Weather condition tabs
        ScrollableTabRow(
            selectedTabIndex = uiState.selectedIndex,
            edgePadding = 8.dp,
        ) {
            weatherConditions.forEachIndexed { index, condition ->
                Tab(
                    selected = uiState.selectedIndex == index,
                    onClick = { state.selectWeather(index) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(condition.emoji, fontSize = 20.sp)
                            Text(condition.name, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                )
            }
        }

        if (uiState.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    "  Generating ${weather.name} theme...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Main weather card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(weather.emoji, fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        weather.temp,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Text(
                        weather.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Details card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    DetailRow("Humidity", weather.humidity)
                    DetailRow("Wind", weather.wind)
                    DetailRow("Condition", weather.name)
                }
            }

            // Forecast card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Forecast", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(
                        weather.forecast,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Hourly forecast
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Hourly", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        listOf("Now", "1h", "2h", "3h", "4h", "5h").forEachIndexed { i, label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                                Text(weather.emoji, fontSize = 16.sp)
                                val tempOffset = listOf(0, -1, -2, -1, 0, 1)[i]
                                val baseTemp = weather.temp.dropLast(2).toIntOrNull() ?: 70
                                Text(
                                    "${baseTemp + tempOffset}\u00B0",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            // About card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("About This Demo", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Each weather condition generates a unique Material 3 theme " +
                            "using Halogen. Tap the tabs above to see the " +
                            "entire UI recolor \u2014 every card, text, and surface adapts to " +
                            "match the current weather.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
