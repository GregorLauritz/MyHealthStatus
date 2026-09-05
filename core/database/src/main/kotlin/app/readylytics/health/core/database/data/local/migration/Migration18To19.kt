package app.readylytics.health.core.database.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_18_19 =
    object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE daily_summaries ADD COLUMN workoutRecommendationJson TEXT DEFAULT NULL")
        }
    }
