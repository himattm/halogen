package me.mmckenna.halogen.sample.screens.image

data class PresetImage(val label: String, val url: String)

val imagePresets = listOf(
    PresetImage(
        label = "Sunset",
        url = "https://images.unsplash.com/photo-1477266190403-a01b87100271?w=400&q=80",
    ),
    PresetImage(
        label = "Forest",
        url = "https://images.unsplash.com/photo-1568864453925-206c927dab0a?w=400&q=80",
    ),
    PresetImage(
        label = "City Night",
        url = "https://images.unsplash.com/photo-1755308482593-f733b46e15ff?w=400&q=80",
    ),
    PresetImage(
        label = "Desert",
        url = "https://images.unsplash.com/photo-1745437980540-b234c90a6557?w=400&q=80",
    ),
    PresetImage(
        label = "Flowers",
        url = "https://images.unsplash.com/photo-1723117965347-52d57b36c77f?w=400&q=80",
    ),
    PresetImage(
        label = "Ocean",
        url = "https://images.unsplash.com/photo-1756572798425-401c88442dee?w=400&q=80",
    ),
    PresetImage(
        label = "Autumn",
        url = "https://images.unsplash.com/photo-1760370048296-47c72b7a7354?w=400&q=80",
    ),
    PresetImage(
        label = "Aurora",
        url = "https://images.unsplash.com/photo-1715454199831-f9ceb01785f9?w=400&q=80",
    ),
)
