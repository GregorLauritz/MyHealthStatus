package app.readylytics.health.feature.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File

private const val WIDGET_DATASTORE_FILE = "readylytics_widget_snapshot.json"

private val Context.widgetSnapshotDataStore: DataStore<WidgetSnapshot> by dataStore(
    fileName = WIDGET_DATASTORE_FILE,
    serializer = WidgetSnapshotSerializer,
)

object WidgetSnapshotDefinition : GlanceStateDefinition<WidgetSnapshot> {
    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<WidgetSnapshot> = context.widgetSnapshotDataStore

    override fun getLocation(
        context: Context,
        fileKey: String,
    ): File = File(context.filesDir, "datastore/$WIDGET_DATASTORE_FILE")
}
