package app.readylytics.health.feature.widget.data

import androidx.datastore.core.Serializer
import app.readylytics.health.core.model.domain.util.logW
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "WidgetSnapshotSerializer"

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
        } catch (e: SerializationException) {
            logW(tag = TAG, throwable = e) { "Failed to deserialize widget snapshot from DataStore" }
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
