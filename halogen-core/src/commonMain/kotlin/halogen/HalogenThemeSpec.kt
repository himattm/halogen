package halogen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The compact LLM output contract. Contains seed colors, typography hints,
 * and shape hints that the library expands into a full Material 3 theme.
 *
 * One spec produces both light and dark color schemes.
 */
@Serializable
public data class HalogenThemeSpec(
    @SerialName("pri") val primary: String,
    @SerialName("sec") val secondary: String,
    @SerialName("ter") val tertiary: String,
    @SerialName("neuL") val neutralLight: String,
    @SerialName("neuD") val neutralDark: String,
    @SerialName("err") val error: String,
    @SerialName("font") val fontMood: String,
    @SerialName("hw") val headingWeight: Int,
    @SerialName("bw") val bodyWeight: Int,
    @SerialName("ls") val tightLetterSpacing: Boolean,
    @SerialName("cs") val cornerStyle: String,
    @SerialName("cx") val cornerScale: Float,
    @SerialName("ext") val extensions: Map<String, String>? = null,
) {
    public companion object {
        private val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** Deserialize a [HalogenThemeSpec] from a JSON string. */
        public fun fromJson(json: String): HalogenThemeSpec {
            return this.json.decodeFromString(serializer(), json)
        }

        /** Serialize a [HalogenThemeSpec] to a JSON string. */
        public fun toJson(spec: HalogenThemeSpec): String {
            return json.encodeToString(serializer(), spec)
        }
    }
}
