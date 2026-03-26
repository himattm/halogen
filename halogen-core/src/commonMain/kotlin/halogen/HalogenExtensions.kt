package halogen

/**
 * Container for developer-defined custom theme tokens.
 * The LLM populates these in the "ext" field of [HalogenThemeSpec].
 */
public class HalogenExtensions(private val values: Map<String, String>) {

    /** Retrieve the value for [key], or `null` if not present. */
    public operator fun get(key: String): String? = values[key]

    /** Return all extension keys. */
    public fun keys(): Set<String> = values.keys

    /** `true` when no extensions are present. */
    public fun isEmpty(): Boolean = values.isEmpty()

    /** Return a read-only copy of the extensions as a [Map]. */
    public fun toMap(): Map<String, String> = values

    public companion object {
        /** Create an empty [HalogenExtensions] instance. */
        public fun empty(): HalogenExtensions = HalogenExtensions(emptyMap())
    }
}

/**
 * Describes a custom extension token that will be included in the LLM prompt.
 *
 * @param key The token key (e.g. "success", "warning").
 * @param description Human-readable description for the LLM (e.g. "A green-ish color for success states").
 */
public data class HalogenExtension(
    val key: String,
    val description: String,
)
