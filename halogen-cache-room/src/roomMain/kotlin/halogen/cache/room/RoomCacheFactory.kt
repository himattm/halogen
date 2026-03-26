package halogen.cache.room

import halogen.cache.room.db.HalogenDatabase

internal expect fun buildHalogenDatabase(name: String): HalogenDatabase
