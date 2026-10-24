package se.novila.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/** What happened to a file the user chose to import. Nothing is changed unless it is [Imported]. */
enum class ImportResult {
    Imported,

    /** Not a Novila export, or damaged. */
    Unreadable,

    /** Written by a later version of Novila, whose data this one may not read. Update the app first. */
    TooNew,
}

/**
 * The user's own copy of their health data, as one file they save and keep themselves: every logged day, both
 * coaches' answers and results, the profile, goals, food preferences with own recipes, measurements and the cycle.
 * The same files *Delete data* empties, apart from the reminders, which belong to the phone they are set on. App
 * settings (theme, the introduction) stay too.
 *
 * The file is written and read through the system's file picker (Storage Access Framework), so the app needs no
 * permission, never chooses where the file goes and never sends it anywhere. It is plain JSON, readable by the user
 * and by other apps (GDPR article 20), and not encrypted: the user decides where it is kept, and the app says so.
 * Importing replaces what is in the app with exactly what the file holds.
 *
 * Versions: the values are kept exactly as the stores save them, each with its own version (`DayEntryCodec`,
 * `CoachPreferencesCodec` and the others), so an export from an older app is read by the same code that reads that
 * older data on the phone, and migrated the same way. An export from a later app may hold a version this one doesn't
 * know, which would read as nothing, so it is refused ([ImportResult.TooNew]) rather than half read.
 */
@SingleIn(AppScope::class)
class DataExport internal constructor(private val stores: Map<String, PreferencesStore>) {
    @Inject constructor(
        health: HealthStore,
        coach: CoachStore,
        sleepCoach: SleepCoachStore,
        profile: ProfileStore,
        goals: GoalsStore,
        food: FoodPreferencesStore,
        measurements: MeasurementsStore,
        cycle: CycleStore,
    ) : this(
        listOf(
            health.store,
            coach.store,
            sleepCoach.store,
            profile.store,
            goals.store,
            food.store,
            measurements.store,
            cycle.store,
        ).associateBy { it.name },
    )

    /** Everything as one JSON text, exported on [today] by the app's build [version]. Waits for the first reads. */
    suspend fun export(today: LocalDate, version: Long): String =
        ExportCodec.encode(ExportFile(version, stores.mapValues { (_, store) -> store.read() }), today)

    /**
     * Replaces the data with what [text] holds: each file in it as it was exported, and the files it doesn't have
     * emptied. Changes nothing unless the whole text can be read, and nothing when a later build than the app's
     * [version] wrote it.
     */
    fun restore(text: String, version: Long): ImportResult {
        val export = try {
            ExportCodec.decode(text)
        } catch (e: ExportTooNewException) {
            return ImportResult.TooNew
        } catch (e: ExportFormatException) {
            return ImportResult.Unreadable
        }
        if (export.version > version) return ImportResult.TooNew
        val files = export.files
        stores.forEach { (name, store) -> store.replace(files[name] ?: mutablePreferencesOf()) }
        return ImportResult.Imported
    }

    companion object {
        /** The largest file that is read, far more than years of logs; anything bigger is not an export. */
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}

/** An export's [files] by name, written by the app's build [version]. */
internal class ExportFile(val version: Long, val files: Map<String, Preferences>)

internal class ExportFormatException(message: String) : Exception(message)

internal class ExportTooNewException : Exception("written by a later version of Novila")

/**
 * The export as JSON: `{"app": "Novila", "format": 1, "version": 42, "exported": "2026-10-23", "files": {...}}`,
 * where `version` is the build that wrote it and each file
 * is its preference file's name and each value a one-entry object with its type, e.g.
 * `"2026-10-22": {"string": "v8;..."}` or `"steps": {"int": 8000}`. The values are stored exactly as the app stores
 * them, so an import gives back the same data. A [FORMAT] higher than this version knows is refused, and only the
 * files the app has are read.
 */
internal object ExportCodec {
    const val FORMAT = 1
    private const val APP = "Novila"
    private val json = Json { prettyPrint = true }

    fun encode(export: ExportFile, exported: LocalDate): String = json.encodeToString(
        JsonElement.serializer(),
        buildJsonObject {
            put("app", APP)
            put("format", FORMAT)
            put("version", export.version)
            put("exported", exported.toString())
            put(
                "files",
                JsonObject(export.files.toSortedMap().mapValues { (_, preferences) -> encode(preferences) }),
            )
        },
    )

    private fun encode(preferences: Preferences): JsonObject = JsonObject(
        preferences.asMap().entries.sortedBy { it.key.name }.associate { (key, value) ->
            key.name to when (value) {
                is String -> typed("string", JsonPrimitive(value))
                is Int -> typed("int", JsonPrimitive(value))
                is Long -> typed("long", JsonPrimitive(value))
                is Float -> typed("float", JsonPrimitive(value))
                is Double -> typed("double", JsonPrimitive(value))
                is Boolean -> typed("boolean", JsonPrimitive(value))
                is Set<*> -> typed("string_set", JsonArray(value.map { JsonPrimitive(it.toString()) }))
                else -> throw IllegalArgumentException("${key.name} has a type the export doesn't know")
            }
        },
    )

    private fun typed(type: String, value: JsonElement) = JsonObject(mapOf(type to value))

    /** The export in [text]. Throws when it is not an export this version can read. */
    fun decode(text: String): ExportFile {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: SerializationException) {
            throw ExportFormatException("not JSON")
        } catch (e: IllegalArgumentException) {
            throw ExportFormatException("not an object")
        }
        if (root["app"]?.primitive()?.content != APP) throw ExportFormatException("not a Novila export")
        val format = root["format"]?.primitive()?.intOrNull ?: throw ExportFormatException("no format")
        if (format > FORMAT) throw ExportTooNewException()
        val version = root["version"]?.primitive()?.longOrNull ?: throw ExportFormatException("no version")
        val files = root["files"] as? JsonObject ?: throw ExportFormatException("no files")
        val read = files.mapValues { (name, file) ->
            val values = file as? JsonObject ?: throw ExportFormatException("$name is not an object")
            mutablePreferencesOf().apply {
                values.forEach { (key, value) -> set(key, value) }
            }
        }
        return ExportFile(version, read)
    }

    private fun MutablePreferences.set(key: String, typed: JsonElement) {
        val entry = (typed as? JsonObject)?.entries?.singleOrNull() ?: throw ExportFormatException("$key has no type")
        val (type, value) = entry
        fun bad(): Nothing = throw ExportFormatException("$key is not a $type")
        when (type) {
            "string" -> this[stringPreferencesKey(key)] = value.primitive()?.takeIf { it.isString }?.content ?: bad()

            "int" -> this[intPreferencesKey(key)] = value.primitive()?.intOrNull ?: bad()

            "long" -> this[longPreferencesKey(key)] = value.primitive()?.longOrNull ?: bad()

            "float" -> this[floatPreferencesKey(key)] = value.primitive()?.floatOrNull ?: bad()

            "double" -> this[doublePreferencesKey(key)] = value.primitive()?.doubleOrNull ?: bad()

            "boolean" -> this[booleanPreferencesKey(key)] = value.primitive()?.booleanOrNull ?: bad()

            "string_set" -> {
                val items = value as? JsonArray ?: bad()
                this[stringSetPreferencesKey(key)] =
                    items.map { it.primitive()?.takeIf { item -> item.isString }?.content ?: bad() }.toSet()
            }

            else -> throw ExportFormatException("$key has an unknown type $type")
        }
    }

    private fun JsonElement.primitive(): JsonPrimitive? = this as? JsonPrimitive
}
