package halogen.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import halogen.HalogenThemeSpec

/**
 * A drop-in Material 3 settings card for generating themes via natural language.
 *
 * Contains a text field for the user's prompt, a "Generate" button,
 * a loading indicator, a color preview of the current spec, and a "Reset" button.
 *
 * @param onGenerate Called with the user's natural language input when "Generate" is tapped.
 * @param isLoading Whether a theme generation request is in progress.
 * @param currentSpec The currently applied theme spec, or `null` if none.
 * @param modifier Modifier applied to the outer [Card].
 */
@Composable
public fun HalogenSettingsCard(
    onGenerate: (String) -> Unit,
    isLoading: Boolean = false,
    currentSpec: HalogenThemeSpec? = null,
    modifier: Modifier = Modifier,
) {
    var prompt by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Describe your theme") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                enabled = !isLoading,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onGenerate(prompt) },
                    enabled = prompt.isNotBlank() && !isLoading,
                ) {
                    Text("Generate")
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            if (currentSpec != null) {
                Spacer(modifier = Modifier.height(4.dp))
                HalogenColorPreview(
                    spec = currentSpec,
                    isDark = isSystemInDarkTheme(),
                )
            }

            OutlinedButton(
                onClick = {
                    prompt = ""
                    onGenerate("")
                },
                enabled = !isLoading,
            ) {
                Text("Reset")
            }
        }
    }
}
