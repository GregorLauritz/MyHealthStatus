package app.readylytics.health.feature.widget.data

import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object WidgetSnapshotSerializer : Serializer<WidgetSnapshot> {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

    override val defaultValue: WidgetSnapshot
        get() = WidgetSnapshot.EMPTY

    override suspend fun readFrom(input: InputStream): WidgetSnapshot =
        try {
            val content = input.readBytes().decodeToString()
            if (content.isBlank()) defaultValue else json.decodeFromString<WidgetSnapshot>(content)
            // Suppress SwallowedException: Glance widgets require a non-crashing fallback to defaultValue
            // upon corrupted or invalid snapshot JSON on disk. Logging via Android framework (android.util.Log)
            // is unavailable in pure JVM unit tests without mocking.
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            defaultValue
        }

    override suspend fun writeTo(
        t: WidgetSnapshot,
        output: OutputStream,
    ) {
        val content = json.encodeToString(t)
        output.write(content.toByteArray(Charsets.UTF_8))
    }
}
