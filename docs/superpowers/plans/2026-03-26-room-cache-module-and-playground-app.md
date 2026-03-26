# Room Cache Module Extraction + Halogen Playground App

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract Room KMP cache into a standalone optional module (`halogen-cache-room`), rework the sample app into "Halogen Playground" with model tuning controls (temperature, topK, topP), and verify CI is green.

**Architecture:** The `ThemeCache` interface in `halogen-engine` is already the abstraction layer. Room code moves from `halogen-engine/src/roomMain/` into a new `halogen-cache-room` KMP module that consumers opt into. The sample app gets renamed, gains LLM parameter controls, and drops all "Habitat" references. LLM provider constructors are updated to accept tuning parameters that the Playground UI can adjust at runtime.

**Tech Stack:** Kotlin Multiplatform, Room KMP 2.7.1, Compose Multiplatform 1.10.1, kotlinx.serialization, kotlinx.coroutines

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `halogen-cache-room/build.gradle.kts` | KMP module build config for Room cache |
| `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/RoomThemeCache.kt` | `ThemeCache` implementation backed by Room |
| `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/RoomThemeCacheConfig.kt` | Configuration (eviction policy, max age) |
| `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/HalogenDatabase.kt` | Room database definition (moved from engine) |
| `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/ThemeDao.kt` | Room DAO (moved from engine) |
| `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/ThemeEntity.kt` | Room entity (moved from engine) |
| `halogen-cache-room/src/commonTest/kotlin/halogen/cache/room/RoomThemeCacheTest.kt` | Unit tests for Room cache |
| `halogen-cache-room/consumer-rules.pro` | ProGuard rules |
| `halogen-cache-room/api/android/halogen-cache-room.api` | API dump (generated) |
| `halogen-cache-room/api/jvm/halogen-cache-room.api` | API dump (generated) |
| `halogen-cache-room/api/halogen-cache-room.klib.api` | API dump (generated) |

### Modified Files

| File | Change |
|------|--------|
| `settings.gradle.kts` | Add `:halogen-cache-room` module |
| `build.gradle.kts` | Add `:halogen-cache-room` to Dokka aggregation |
| `halogen-engine/build.gradle.kts` | Remove Room dependencies and `roomMain` source set |
| `halogen-engine/src/commonMain/kotlin/halogen/engine/HalogenCache.kt` | Remove `room()` placeholder comment |
| `sample/build.gradle.kts` | Add `halogen-cache-room` dependency |
| `sample/src/main/AndroidManifest.xml` | Rename to "Halogen Playground" |
| `sample/src/main/res/values/strings.xml` | Rename app_name |
| `sample/README.md` | Update title and description |
| `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenDemoApp.kt` | Rename nav labels, remove "Habitat" refs |
| `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt` | Accept tuning params when rebuilding providers |
| `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt` | Add temperature/topK/topP sliders |
| `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt` | Expose tuning params, rebuild provider on change |
| `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt` | Accept temperature, topP, maxTokens as constructor params |
| `halogen-provider-nano/src/main/kotlin/halogen/provider/nano/GeminiNanoProvider.kt` | Add topP parameter |

### Deleted Files

| File | Reason |
|------|--------|
| `halogen-engine/src/roomMain/kotlin/halogen/engine/db/HalogenDatabase.kt` | Moved to `halogen-cache-room` |
| `halogen-engine/src/roomMain/kotlin/halogen/engine/db/ThemeDao.kt` | Moved to `halogen-cache-room` |
| `halogen-engine/src/roomMain/kotlin/halogen/engine/db/ThemeEntity.kt` | Moved to `halogen-cache-room` |
| `halogen-engine/schemas/` | Moved to `halogen-cache-room/schemas/` |

---

## Workstream A: Extract Room Cache Module

### Task 1: Create `halogen-cache-room` module skeleton

**Files:**
- Create: `halogen-cache-room/build.gradle.kts`
- Create: `halogen-cache-room/consumer-rules.pro`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add module to settings.gradle.kts**

In `settings.gradle.kts`, add `:halogen-cache-room` to the includes:

```kotlin
include(":halogen-core")
include(":halogen-compose")
include(":halogen-engine")
include(":halogen-cache-room")
include(":halogen-provider-nano")
include(":sample")
```

- [ ] **Step 2: Create the module build file**

Create `halogen-cache-room/build.gradle.kts`. This mirrors the engine's Room setup but as its own module:

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.halogen.kmp.library)
    alias(libs.plugins.halogen.publishing)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("room") {
                withAndroidTarget()
                withJvm()
                withIosArm64()
                withIosSimulatorArm64()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":halogen-core"))
            api(project(":halogen-engine"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        val roomMain by getting {
            dependencies {
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.cache.room"
}

dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.room.compiler)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dokka {
    dokkaSourceSets.configureEach {
        if (name.contains("room", ignoreCase = true)) {
            suppress.set(true)
        }
    }
}

mavenPublishing {
    pom {
        name.set("Halogen Cache — Room")
        description.set("Optional Room KMP persistent cache for Halogen themes")
    }
}
```

- [ ] **Step 3: Create consumer-rules.pro**

Create `halogen-cache-room/consumer-rules.pro`:

```
# Room KMP entities
-keep class halogen.cache.room.db.** { *; }
```

- [ ] **Step 4: Add to root Dokka aggregation**

In root `build.gradle.kts`, add the new module to Dokka dependencies:

```kotlin
dependencies {
    dokka(project(":halogen-core"))
    dokka(project(":halogen-engine"))
    dokka(project(":halogen-compose"))
    dokka(project(":halogen-provider-nano"))
    dokka(project(":halogen-cache-room"))
}
```

- [ ] **Step 5: Verify module syncs**

Run: `./gradlew -q --console=plain :halogen-cache-room:help`

Expected: No errors (module recognized by Gradle).

- [ ] **Step 6: Commit**

```bash
git add halogen-cache-room/build.gradle.kts halogen-cache-room/consumer-rules.pro settings.gradle.kts build.gradle.kts
git commit -m "feat: add halogen-cache-room module skeleton"
```

---

### Task 2: Move Room database classes from engine to cache-room

**Files:**
- Create: `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/HalogenDatabase.kt`
- Create: `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/ThemeDao.kt`
- Create: `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/ThemeEntity.kt`
- Move: `halogen-engine/schemas/` -> `halogen-cache-room/schemas/`
- Delete: `halogen-engine/src/roomMain/kotlin/halogen/engine/db/`

- [ ] **Step 1: Create ThemeEntity in new module**

Create `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/ThemeEntity.kt`:

```kotlin
package halogen.cache.room.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "halogen_themes")
internal data class ThemeEntity(
    @PrimaryKey val key: String,
    val spec: String,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val source: String,
    val sizeBytes: Int,
)
```

- [ ] **Step 2: Create ThemeDao in new module**

Create `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/ThemeDao.kt`:

```kotlin
package halogen.cache.room.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface ThemeDao {
    @Query("SELECT * FROM halogen_themes WHERE `key` = :key")
    suspend fun getByKey(key: String): ThemeEntity?

    @Upsert
    suspend fun upsert(entity: ThemeEntity)

    @Query("DELETE FROM halogen_themes WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM halogen_themes")
    suspend fun deleteAll()

    @Query("SELECT `key` FROM halogen_themes")
    suspend fun getAllKeys(): List<String>

    @Query("SELECT * FROM halogen_themes")
    suspend fun getAll(): List<ThemeEntity>

    @Query("DELETE FROM halogen_themes WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM halogen_themes")
    suspend fun count(): Int
}
```

- [ ] **Step 3: Create HalogenDatabase in new module**

Create `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/db/HalogenDatabase.kt`:

```kotlin
package halogen.cache.room.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [ThemeEntity::class], version = 1)
@ConstructedBy(HalogenDatabaseConstructor::class)
internal abstract class HalogenDatabase : RoomDatabase() {
    abstract fun themeDao(): ThemeDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object HalogenDatabaseConstructor : RoomDatabaseConstructor<HalogenDatabase>
```

- [ ] **Step 4: Move schema files**

```bash
mkdir -p halogen-cache-room/schemas
cp -r halogen-engine/schemas/ halogen-cache-room/schemas/
```

Rename the schema directory to match the new package. The schema file is at `halogen-engine/schemas/halogen.engine.db.HalogenDatabase/1.json`. Since the class is now `halogen.cache.room.db.HalogenDatabase`, the schema directory name will be generated fresh by Room KSP on first build. Remove the old one:

```bash
rm -rf halogen-cache-room/schemas/halogen.engine.db.HalogenDatabase
```

- [ ] **Step 5: Delete old Room files from engine**

```bash
rm -rf halogen-engine/src/roomMain/kotlin/halogen/engine/db/
rm -rf halogen-engine/schemas/
```

- [ ] **Step 6: Verify compilation**

Run: `./gradlew -q --console=plain :halogen-cache-room:compileKotlinJvm`

Expected: Compiles (Room KSP generates the schema in the new location).

- [ ] **Step 7: Commit**

```bash
git add halogen-cache-room/src/ halogen-cache-room/schemas/
git rm -r halogen-engine/src/roomMain/kotlin/halogen/engine/db/
git rm -r halogen-engine/schemas/
git commit -m "feat: move Room database classes to halogen-cache-room module"
```

---

### Task 3: Implement RoomThemeCache

**Files:**
- Create: `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/RoomThemeCache.kt`
- Create: `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/RoomThemeCacheConfig.kt`

- [ ] **Step 1: Create RoomThemeCacheConfig**

Create `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/RoomThemeCacheConfig.kt`:

```kotlin
package halogen.cache.room

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Configuration for [RoomThemeCache].
 *
 * @param maxEntries Maximum number of themes to store. 0 = unlimited.
 * @param maxAge Maximum age before auto-eviction on read. null = no expiry.
 */
public data class RoomThemeCacheConfig(
    val maxEntries: Int = 0,
    val maxAge: Duration? = null,
) {
    public companion object {
        public val Default: RoomThemeCacheConfig = RoomThemeCacheConfig()

        public fun withLimit(maxEntries: Int): RoomThemeCacheConfig =
            RoomThemeCacheConfig(maxEntries = maxEntries)

        public fun withExpiry(maxAge: Duration): RoomThemeCacheConfig =
            RoomThemeCacheConfig(maxAge = maxAge)
    }
}
```

- [ ] **Step 2: Create RoomThemeCache**

Create `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/RoomThemeCache.kt`:

```kotlin
package halogen.cache.room

import halogen.HalogenThemeSpec
import halogen.engine.CacheEvent
import halogen.engine.ThemeCacheEntry
import halogen.engine.ThemeCache
import halogen.engine.ThemeSource
import halogen.cache.room.db.ThemeDao
import halogen.cache.room.db.ThemeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json

/**
 * [ThemeCache] implementation backed by Room KMP.
 *
 * Persists themes to SQLite so they survive process death, app restarts,
 * and device reboots. Available on Android, iOS, and JVM (not wasmJs).
 */
public class RoomThemeCache internal constructor(
    private val dao: ThemeDao,
    private val config: RoomThemeCacheConfig = RoomThemeCacheConfig.Default,
) : ThemeCache {

    private val json = Json { ignoreUnknownKeys = true }
    private val _changes = MutableSharedFlow<CacheEvent>(extraBufferCapacity = 64)

    override suspend fun get(key: String): HalogenThemeSpec? {
        val entity = dao.getByKey(key) ?: return null

        // Check max age expiry
        if (config.maxAge != null) {
            val age = currentTimeMillis() - entity.createdAt
            if (age > config.maxAge.inWholeMilliseconds) {
                dao.delete(key)
                _changes.tryEmit(CacheEvent.Evicted(key))
                return null
            }
        }

        // Update last accessed time
        dao.upsert(entity.copy(lastAccessedAt = currentTimeMillis()))
        return json.decodeFromString<HalogenThemeSpec>(entity.spec)
    }

    override suspend fun put(key: String, spec: HalogenThemeSpec, source: ThemeSource) {
        val serialized = json.encodeToString(HalogenThemeSpec.serializer(), spec)
        val now = currentTimeMillis()
        dao.upsert(
            ThemeEntity(
                key = key,
                spec = serialized,
                createdAt = now,
                lastAccessedAt = now,
                source = source.name,
                sizeBytes = serialized.encodeToByteArray().size,
            )
        )
        _changes.tryEmit(CacheEvent.Inserted(key, source))

        // Enforce max entries by evicting oldest
        if (config.maxEntries > 0) {
            val count = dao.count()
            if (count > config.maxEntries) {
                val all = dao.getAll().sortedBy { it.lastAccessedAt }
                val toEvict = all.take(count - config.maxEntries)
                toEvict.forEach { dao.delete(it.key) }
                if (toEvict.size == 1) {
                    _changes.tryEmit(CacheEvent.Evicted(toEvict.first().key))
                } else if (toEvict.size > 1) {
                    _changes.tryEmit(CacheEvent.EvictedBatch(toEvict.map { it.key }.toSet()))
                }
            }
        }
    }

    override suspend fun contains(key: String): Boolean =
        dao.getByKey(key) != null

    override suspend fun evict(key: String) {
        dao.delete(key)
        _changes.tryEmit(CacheEvent.Evicted(key))
    }

    override suspend fun evict(keys: Set<String>) {
        keys.forEach { dao.delete(it) }
        _changes.tryEmit(CacheEvent.EvictedBatch(keys))
    }

    override suspend fun clear() {
        dao.deleteAll()
        _changes.tryEmit(CacheEvent.Cleared)
    }

    override suspend fun keys(): Set<String> =
        dao.getAllKeys().toSet()

    override suspend fun size(): Int =
        dao.count()

    override suspend fun entries(): List<ThemeCacheEntry> =
        dao.getAll().map { entity ->
            ThemeCacheEntry(
                key = entity.key,
                source = ThemeSource.valueOf(entity.source),
                createdAt = entity.createdAt,
                lastAccessedAt = entity.lastAccessedAt,
                sizeBytes = entity.sizeBytes,
            )
        }

    override fun observeChanges(): Flow<CacheEvent> = _changes

    /**
     * Evict all entries older than [cutoff] milliseconds (epoch time).
     */
    public suspend fun evictOlderThan(cutoffMillis: Long) {
        val keysBeforeEviction = dao.getAllKeys().toSet()
        dao.deleteOlderThan(cutoffMillis)
        val keysAfterEviction = dao.getAllKeys().toSet()
        val evictedKeys = keysBeforeEviction - keysAfterEviction
        if (evictedKeys.isNotEmpty()) {
            _changes.tryEmit(CacheEvent.EvictedBatch(evictedKeys))
        }
    }
}

internal expect fun currentTimeMillis(): Long
```

- [ ] **Step 3: Create platform-specific currentTimeMillis implementations**

Create `halogen-cache-room/src/androidMain/kotlin/halogen/cache/room/CurrentTime.android.kt`:

```kotlin
package halogen.cache.room

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

Create `halogen-cache-room/src/jvmMain/kotlin/halogen/cache/room/CurrentTime.jvm.kt`:

```kotlin
package halogen.cache.room

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
```

Create `halogen-cache-room/src/iosMain/kotlin/halogen/cache/room/CurrentTime.ios.kt`:

```kotlin
package halogen.cache.room

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
```

- [ ] **Step 4: Create RoomThemeCache factory (expect/actual for database instantiation)**

Create `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/HalogenRoomCache.kt`:

```kotlin
package halogen.cache.room

/**
 * Factory for creating a [RoomThemeCache] backed by Room KMP.
 *
 * Platform-specific: call the appropriate platform factory method
 * to obtain a database instance, then pass it here.
 *
 * ```kotlin
 * // Android
 * val cache = HalogenRoomCache.create(
 *     dao = HalogenRoomCache.createDatabase(context).themeDao(),
 * )
 *
 * // JVM / iOS
 * val cache = HalogenRoomCache.create(
 *     dao = HalogenRoomCache.createDatabase(dbFilePath).themeDao(),
 * )
 * ```
 */
public object HalogenRoomCache {

    /**
     * Create a [RoomThemeCache] from a [ThemeDao].
     */
    public fun create(
        dao: halogen.cache.room.db.ThemeDao,
        config: RoomThemeCacheConfig = RoomThemeCacheConfig.Default,
    ): RoomThemeCache = RoomThemeCache(dao, config)
}
```

Wait - the ThemeDao is `internal`. We need a different approach. Let me revise: the factory should take a `HalogenDatabase` and extract the DAO internally. But `HalogenDatabase` is also internal. The factory needs to own database creation.

Let me revise. We need platform-specific database builders. The standard Room KMP pattern is:

- Android: `Room.databaseBuilder(context, HalogenDatabase::class.java, name)`
- JVM: `Room.databaseBuilder<HalogenDatabase>(name)`
- iOS: `Room.databaseBuilder<HalogenDatabase>(name, instantiateImpl(...))`

Since the database is `internal`, the factory is in the same module and can access it.

**Revised Step 4:**

Create `halogen-cache-room/src/roomMain/kotlin/halogen/cache/room/RoomCacheFactory.kt`:

```kotlin
package halogen.cache.room

import halogen.cache.room.db.HalogenDatabase

/**
 * Platform-specific factory function for building the Room database.
 * Each platform (Android, JVM, iOS) provides its own implementation.
 */
internal expect fun buildHalogenDatabase(name: String): HalogenDatabase
```

Create `halogen-cache-room/src/androidMain/kotlin/halogen/cache/room/RoomCacheFactory.android.kt`:

```kotlin
package halogen.cache.room

import android.content.Context
import androidx.room.Room
import halogen.cache.room.db.HalogenDatabase

private var appContext: Context? = null

/**
 * Must be called once (typically in Application.onCreate or before first use)
 * to provide Android context for Room database creation.
 */
public fun HalogenRoomCache.initialize(context: Context) {
    appContext = context.applicationContext
}

internal actual fun buildHalogenDatabase(name: String): HalogenDatabase {
    val ctx = appContext ?: error(
        "HalogenRoomCache.initialize(context) must be called before creating a Room cache on Android."
    )
    return Room.databaseBuilder<HalogenDatabase>(ctx, name).build()
}
```

Create `halogen-cache-room/src/jvmMain/kotlin/halogen/cache/room/RoomCacheFactory.jvm.kt`:

```kotlin
package halogen.cache.room

import androidx.room.Room
import halogen.cache.room.db.HalogenDatabase

internal actual fun buildHalogenDatabase(name: String): HalogenDatabase =
    Room.databaseBuilder<HalogenDatabase>(name).build()
```

Create `halogen-cache-room/src/iosMain/kotlin/halogen/cache/room/RoomCacheFactory.ios.kt`:

```kotlin
package halogen.cache.room

import androidx.room.Room
import halogen.cache.room.db.HalogenDatabase
import halogen.cache.room.db.HalogenDatabaseConstructor
import platform.Foundation.NSHomeDirectory

internal actual fun buildHalogenDatabase(name: String): HalogenDatabase =
    Room.databaseBuilder<HalogenDatabase>(
        name = NSHomeDirectory() + "/$name",
        factory = { HalogenDatabaseConstructor.initialize() },
    ).build()
```

- [ ] **Step 5: Update HalogenRoomCache factory to use platform builder**

Replace the `HalogenRoomCache.kt` created earlier. Create `halogen-cache-room/src/commonMain/kotlin/halogen/cache/room/HalogenRoomCache.kt`:

```kotlin
package halogen.cache.room

import halogen.engine.ThemeCache

/**
 * Factory for creating a Room-backed [ThemeCache].
 *
 * ```kotlin
 * // Android — call initialize(context) first in Application.onCreate
 * HalogenRoomCache.initialize(applicationContext)
 * val cache = HalogenRoomCache.create()
 *
 * // JVM / iOS — no initialization needed
 * val cache = HalogenRoomCache.create()
 * ```
 */
public object HalogenRoomCache {

    private const val DEFAULT_DB_NAME = "halogen_themes.db"

    /**
     * Create a Room-backed [ThemeCache].
     *
     * @param dbName Database file name (default: "halogen_themes.db")
     * @param config Cache configuration (eviction limits, max age)
     */
    public fun create(
        dbName: String = DEFAULT_DB_NAME,
        config: RoomThemeCacheConfig = RoomThemeCacheConfig.Default,
    ): ThemeCache {
        val database = buildHalogenDatabase(dbName)
        return RoomThemeCache(database.themeDao(), config)
    }
}
```

- [ ] **Step 6: Verify compilation**

Run: `./gradlew -q --console=plain :halogen-cache-room:compileKotlinJvm`

Expected: Compiles successfully.

- [ ] **Step 7: Commit**

```bash
git add halogen-cache-room/src/
git commit -m "feat: implement RoomThemeCache with platform-specific factories"
```

---

### Task 4: Remove Room from halogen-engine

**Files:**
- Modify: `halogen-engine/build.gradle.kts`
- Modify: `halogen-engine/src/commonMain/kotlin/halogen/engine/HalogenCache.kt`
- Delete: `halogen-engine/src/androidMain/kotlin/halogen/engine/CurrentTime.android.kt` (if Room-specific)
- Delete: `halogen-engine/src/jvmMain/kotlin/halogen/engine/CurrentTime.jvm.kt`
- Delete: `halogen-engine/src/iosMain/kotlin/halogen/engine/CurrentTime.ios.kt`

- [ ] **Step 1: Strip Room dependencies from engine build.gradle.kts**

In `halogen-engine/build.gradle.kts`, remove:
- The `room` and `ksp` plugins
- The `roomMain` source set hierarchy and its dependencies
- The `dependencies { }` block with KSP Room compiler entries
- The `room { schemaDirectory(...) }` block
- The Dokka suppression for `roomMain`

The engine should keep only:
- `halogen.kmp-library`, `halogen.publishing`, `kotlin.serialization` plugins
- `commonMain` depends on `halogen-core`, `kotlinx-coroutines-core`, `kotlinx-serialization-json`
- `androidMain` depends on `kotlinx-coroutines-android`
- `commonTest` depends on `kotlinx-coroutines-test`
- Platform expect/actual files remain for `CurrentTime` and `Platform` (these are used by engine, not Room-specific)

Check if `CurrentTime` expect/actual in engine is used by anything other than Room. Read the engine source to confirm. If `CurrentTime` is only used by the engine's own logic (e.g., `MemoryThemeCache` uses `currentTimeMillis()`), keep it. If it was only for Room, remove it.

Based on exploration: `MemoryThemeCache` uses `currentTimeMillis()` from `halogen.engine.CurrentTime`, so keep the engine's own `CurrentTime` expect/actual. The engine's `CurrentTime` files are at `halogen-engine/src/*/kotlin/halogen/engine/CurrentTime.*.kt` - these stay.

The engine build.gradle.kts should look like:

```kotlin
plugins {
    alias(libs.plugins.halogen.kmp.library)
    alias(libs.plugins.halogen.publishing)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":halogen-core"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "me.mmckenna.halogen.engine"
}

mavenPublishing {
    pom {
        name.set("Halogen Engine")
        description.set("Persistence and orchestration engine for Halogen on-device AI inference")
    }
}
```

- [ ] **Step 2: Remove room() placeholder from HalogenCache.kt**

In `halogen-engine/src/commonMain/kotlin/halogen/engine/HalogenCache.kt`, remove the comment about Room:

The file should contain only:

```kotlin
package halogen.engine

/**
 * Factory for built-in [ThemeCache] implementations.
 *
 * For persistent caching, add the `halogen-cache-room` dependency
 * and use [HalogenRoomCache.create()].
 */
public object HalogenCache {

    /**
     * In-memory LRU cache. Fast but does not survive process death.
     */
    public fun memory(maxEntries: Int = 20): ThemeCache = MemoryThemeCache(maxEntries)

    /**
     * No-op cache. Every resolve hits the LLM provider.
     */
    public fun none(): ThemeCache = NoOpThemeCache()
}
```

- [ ] **Step 3: Delete old roomMain source directories from engine**

Verify the directories were already deleted in Task 2. If not:

```bash
rm -rf halogen-engine/src/roomMain/
```

- [ ] **Step 4: Verify engine compiles without Room**

Run: `./gradlew -q --console=plain :halogen-engine:compileKotlinJvm :halogen-engine:compileKotlinAndroid`

Expected: Compiles with no Room references.

- [ ] **Step 5: Run engine tests**

Run: `./gradlew -q --console=plain :halogen-engine:jvmTest`

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add halogen-engine/build.gradle.kts halogen-engine/src/commonMain/kotlin/halogen/engine/HalogenCache.kt
git rm -rf halogen-engine/src/roomMain/ 2>/dev/null || true
git rm -rf halogen-engine/schemas/ 2>/dev/null || true
git commit -m "refactor: remove Room from halogen-engine, now optional via halogen-cache-room"
```

---

### Task 5: Update API dumps and run full build

**Files:**
- Regenerate: `halogen-engine/api/` (API dumps change since Room types removed)
- Generate: `halogen-cache-room/api/` (new module API dump)

- [ ] **Step 1: Regenerate API dumps**

Run: `./gradlew -q --console=plain apiDump`

This regenerates all `.api` files for binary compatibility validation.

- [ ] **Step 2: Run full API check**

Run: `./gradlew -q --console=plain apiCheck`

Expected: Pass (dumps match).

- [ ] **Step 3: Run all tests**

Run: `./gradlew -q --console=plain :halogen-core:jvmTest :halogen-engine:jvmTest :halogen-compose:jvmTest :halogen-cache-room:jvmTest`

Expected: All pass.

- [ ] **Step 4: Commit API dumps**

```bash
git add halogen-engine/api/ halogen-cache-room/api/
git commit -m "chore: regenerate API dumps after Room extraction"
```

---

## Workstream B: Halogen Playground App

### Task 6: Rename sample app to "Halogen Playground"

**Files:**
- Modify: `sample/src/main/res/values/strings.xml`
- Modify: `sample/src/main/AndroidManifest.xml`
- Modify: `sample/README.md`

- [ ] **Step 1: Update strings.xml**

In `sample/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Halogen Playground</string>
</resources>
```

- [ ] **Step 2: Update AndroidManifest.xml label**

In `sample/src/main/AndroidManifest.xml`, change `android:label` to reference `@string/app_name` (it likely already does via the theme, but verify). The manifest should have:

```xml
android:label="@string/app_name"
```

- [ ] **Step 3: Update README.md**

Replace the content of `sample/README.md`:

```markdown
# Halogen Playground

Interactive demo app for the [Halogen](https://github.com/himattm/halogen) library. Generate Material 3 themes from natural language prompts and tune LLM parameters in real time.

## Features

- **Playground** - Type a prompt, adjust temperature/topK/topP, generate a theme, see every M3 component update live
- **Weather** - 8 weather conditions, each auto-generating a contextual theme
- **Test Harness** - Matrix view: run prompts against multiple config presets, compare results
- **Settings** - Switch LLM providers (Gemini Nano / OpenAI), manage cache, download models

## Running

1. Open in Android Studio
2. Select the `sample` run configuration
3. Run on a device or emulator

## Cloud Provider (Optional)

Add your OpenAI API key to `local.properties`:

```properties
OPENAI_API_KEY=sk-...
```

Without an API key, the app falls back to Gemini Nano (on supported devices) or the default theme.
```

- [ ] **Step 4: Commit**

```bash
git add sample/src/main/res/values/strings.xml sample/src/main/AndroidManifest.xml sample/README.md
git commit -m "chore: rename sample app to Halogen Playground"
```

---

### Task 7: Make OpenAiProvider accept tuning parameters

**Files:**
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt`

- [ ] **Step 1: Update OpenAiProvider constructor**

In `OpenAiProvider.kt`, change the constructor to accept tuning params:

```kotlin
class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    var temperature: Float = 0.3f,
    var topP: Float? = null,
    var maxTokens: Int = 300,
) : HalogenLlmProvider {
```

Note `temperature`, `topP`, and `maxTokens` are `var` so the Playground can adjust them at runtime without rebuilding the provider.

- [ ] **Step 2: Update generate() to use topP**

In the `generate()` function, update the ChatRequest construction:

```kotlin
val request = ChatRequest(
    model = model,
    messages = listOf(Message(role = "user", content = prompt)),
    temperature = temperature,
    maxTokens = maxTokens,
    topP = topP,
)
```

- [ ] **Step 3: Add topP to ChatRequest**

In `ChatRequest.kt`, add the `topP` field:

```kotlin
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 300,
    @SerializedName("top_p") val topP: Float? = null,
)
```

- [ ] **Step 4: Verify sample compiles**

Run: `./gradlew -q --console=plain :sample:compileDebugKotlin`

Expected: Compiles.

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/OpenAiProvider.kt sample/src/main/kotlin/me/mmckenna/halogen/sample/llms/openai/ChatRequest.kt
git commit -m "feat: make OpenAiProvider temperature, topP, maxTokens configurable"
```

---

### Task 8: Make GeminiNanoProvider topP configurable

**Files:**
- Modify: `halogen-provider-nano/src/main/kotlin/halogen/provider/nano/GeminiNanoProvider.kt`

- [ ] **Step 1: Add topP parameter to GeminiNanoProvider**

The GeminiNanoProvider already takes `temperature` and `topK` in the constructor. Add `topP` and make them `var`:

```kotlin
public class GeminiNanoProvider(
    public var temperature: Float = 0.2f,
    public var topK: Int = 10,
    public var topP: Float? = null,
) : HalogenLlmProvider {
```

Note: Gemini Nano's Prompt API may not support topP. If the ML Kit API doesn't have a topP setter, we store the value but log a warning. Check the `GenerativeModel` builder - if it only accepts temperature and topK, that's fine; the parameter is there for UI consistency and we document that it's ignored on Nano.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew -q --console=plain :halogen-provider-nano:compileDebugKotlin`

Expected: Compiles.

- [ ] **Step 3: Commit**

```bash
git add halogen-provider-nano/src/main/kotlin/halogen/provider/nano/GeminiNanoProvider.kt
git commit -m "feat: make GeminiNanoProvider temperature, topK, topP mutable"
```

---

### Task 9: Add LLM tuning controls to PlaygroundScreen

**Files:**
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt`

- [ ] **Step 1: Add tuning state to PlaygroundViewModel**

In `PlaygroundViewModel.kt`, add state for LLM parameters:

```kotlin
// LLM tuning parameters
var temperature by mutableFloatStateOf(0.3f)
    private set
var topK by mutableIntStateOf(10)
    private set
var topP by mutableFloatStateOf(0.9f)
    private set
var topPEnabled by mutableStateOf(false)
    private set
var maxTokens by mutableIntStateOf(300)
    private set
var showTuningControls by mutableStateOf(false)
    private set

fun updateTemperature(value: Float) {
    temperature = value
    syncTuningParams()
}

fun updateTopK(value: Int) {
    topK = value
    syncTuningParams()
}

fun updateTopP(value: Float) {
    topP = value
    syncTuningParams()
}

fun updateTopPEnabled(enabled: Boolean) {
    topPEnabled = enabled
    syncTuningParams()
}

fun updateMaxTokens(value: Int) {
    maxTokens = value
    syncTuningParams()
}

fun toggleTuningControls() {
    showTuningControls = !showTuningControls
}

private fun syncTuningParams() {
    appViewModel.updateTuningParams(
        temperature = temperature,
        topK = topK,
        topP = if (topPEnabled) topP else null,
        maxTokens = maxTokens,
    )
}
```

- [ ] **Step 2: Add updateTuningParams to HalogenAppViewModel**

In `HalogenAppViewModel.kt`, add a method that pushes tuning params to the current provider:

```kotlin
fun updateTuningParams(
    temperature: Float,
    topK: Int,
    topP: Float?,
    maxTokens: Int,
) {
    // Update Nano provider if active
    nanoProvider?.let {
        it.temperature = temperature
        it.topK = topK
        it.topP = topP
    }
    // Update OpenAI provider if active
    openAiProvider?.let {
        it.temperature = temperature
        it.topP = topP
        it.maxTokens = maxTokens
    }
}
```

Note: `nanoProvider` and `openAiProvider` must be accessible. If they're currently private, make them `internal` or add a method. Check current code - from exploration, `nanoProvider` is a `val` in the ViewModel. The OpenAI provider is created inline. Refactor so both are stored as properties.

- [ ] **Step 3: Add tuning UI to PlaygroundScreen**

In `PlaygroundScreen.kt`, add a collapsible "Model Settings" section below the prompt input and above the Generate button. Add this after the config preset dropdown:

```kotlin
// Model Settings toggle
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        "Model Settings",
        style = MaterialTheme.typography.titleSmall,
    )
    IconButton(onClick = { viewModel.toggleTuningControls() }) {
        Icon(
            imageVector = if (viewModel.showTuningControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (viewModel.showTuningControls) "Hide" else "Show",
        )
    }
}

AnimatedVisibility(visible = viewModel.showTuningControls) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Temperature
        Text(
            "Temperature: ${"%.2f".format(viewModel.temperature)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = viewModel.temperature,
            onValueChange = { viewModel.updateTemperature(it) },
            valueRange = 0f..2f,
            steps = 19, // 0.1 increments
        )

        // Top K
        Text(
            "Top K: ${viewModel.topK}",
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = viewModel.topK.toFloat(),
            onValueChange = { viewModel.updateTopK(it.toInt()) },
            valueRange = 1f..100f,
            steps = 98,
        )

        // Top P (with enable toggle)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = viewModel.topPEnabled,
                onCheckedChange = { viewModel.updateTopPEnabled(it) },
            )
            Text(
                "Top P: ${"%.2f".format(viewModel.topP)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (viewModel.topPEnabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
        Slider(
            value = viewModel.topP,
            onValueChange = { viewModel.updateTopP(it) },
            valueRange = 0f..1f,
            steps = 19,
            enabled = viewModel.topPEnabled,
        )

        // Max Tokens
        Text(
            "Max Tokens: ${viewModel.maxTokens}",
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = viewModel.maxTokens.toFloat(),
            onValueChange = { viewModel.updateMaxTokens(it.toInt()) },
            valueRange = 100f..1000f,
            steps = 8, // 100-step increments
        )
    }
}
```

- [ ] **Step 4: Verify sample compiles and runs**

Run: `./gradlew -q --console=plain :sample:assembleDebug`

Expected: Builds successfully.

- [ ] **Step 5: Commit**

```bash
git add sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundScreen.kt sample/src/main/kotlin/me/mmckenna/halogen/sample/screens/playground/PlaygroundViewModel.kt sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt
git commit -m "feat: add LLM tuning controls (temperature, topK, topP, maxTokens) to Playground"
```

---

### Task 10: Wire Room cache into sample app

**Files:**
- Modify: `sample/build.gradle.kts`
- Modify: `sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt`

- [ ] **Step 1: Add halogen-cache-room dependency to sample**

In `sample/build.gradle.kts`, add:

```kotlin
implementation(project(":halogen-cache-room"))
```

- [ ] **Step 2: Initialize HalogenRoomCache in HalogenAppViewModel**

In `HalogenAppViewModel.kt`, update the engine builder to use Room cache:

```kotlin
import halogen.cache.room.HalogenRoomCache

// In init or engine builder:
HalogenRoomCache.initialize(application)

private fun buildEngine(provider: HalogenLlmProvider): HalogenEngine {
    return Halogen.Builder()
        .provider(provider)
        .cache(HalogenRoomCache.create())
        .defaultTheme(HalogenDefaults.light())
        .build()
}
```

- [ ] **Step 3: Verify sample compiles**

Run: `./gradlew -q --console=plain :sample:assembleDebug`

Expected: Builds.

- [ ] **Step 4: Commit**

```bash
git add sample/build.gradle.kts sample/src/main/kotlin/me/mmckenna/halogen/sample/HalogenAppViewModel.kt
git commit -m "feat: use Room cache in sample app for persistent theme storage"
```

---

### Task 11: Update CI workflow for new module

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Add halogen-cache-room test tasks to CI**

In `.github/workflows/ci.yml`, add to the `check` job:

```yaml
- name: halogen-cache-room - JVM unit tests
  run: ./gradlew -q --console=plain :halogen-cache-room:jvmTest
```

Add to the `ios-tests` job:

```yaml
- name: halogen-cache-room - compile iOS targets
  run: |
    ./gradlew -q --console=plain :halogen-cache-room:compileKotlinIosArm64
    ./gradlew -q --console=plain :halogen-cache-room:compileKotlinIosSimulatorArm64
```

The `wasmjs-tests` job does NOT need a halogen-cache-room entry since Room doesn't support wasmJs.

- [ ] **Step 2: Verify CI config is valid**

Run: `./gradlew -q --console=plain :halogen-cache-room:jvmTest`

Expected: Tests pass (or no tests yet - that's fine, the task runs clean).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add halogen-cache-room to CI test matrix"
```

---

### Task 12: Final verification - full CI suite locally

- [ ] **Step 1: Run all library tests**

```bash
./gradlew -q --console=plain :halogen-core:allTests :halogen-engine:jvmTest :halogen-compose:jvmTest :halogen-cache-room:jvmTest :halogen-provider-nano:testDebugUnitTest
```

Expected: All pass.

- [ ] **Step 2: Run API check**

```bash
./gradlew -q --console=plain apiCheck
```

Expected: Pass.

- [ ] **Step 3: Build sample app**

```bash
./gradlew -q --console=plain :sample:assembleDebug
```

Expected: Builds successfully.

- [ ] **Step 4: Run iOS compilation**

```bash
./gradlew -q --console=plain :halogen-core:compileKotlinIosSimulatorArm64 :halogen-engine:compileKotlinIosSimulatorArm64 :halogen-compose:compileKotlinIosSimulatorArm64 :halogen-cache-room:compileKotlinIosSimulatorArm64
```

Expected: All compile.

- [ ] **Step 5: Verify wasmJs tests (engine and core only - Room not on wasm)**

```bash
./gradlew -q --console=plain :halogen-core:wasmJsBrowserTest :halogen-engine:wasmJsBrowserTest :halogen-compose:wasmJsBrowserTest
```

Expected: All pass.
