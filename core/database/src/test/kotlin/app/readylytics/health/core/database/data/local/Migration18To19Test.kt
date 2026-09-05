package app.readylytics.health.core.database.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import app.readylytics.health.core.database.data.local.migration.MIGRATION_18_19
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration18To19Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), HealthDatabase::class.java)

    @Test
    fun migrationExecutesExpectedSchemaAlter() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        MIGRATION_18_19.migrate(db)

        verify {
            db.execSQL("ALTER TABLE daily_summaries ADD COLUMN workoutRecommendationJson TEXT DEFAULT NULL")
        }
    }

    @Test
    fun migration18To19PreservesOldSummaryAndAddsNullableWorkoutRecommendationColumn() {
        helper.createDatabase(TEST_DATABASE, 18).apply {
            seedDailySummary(this)
            close()
        }

        val database = helper.runMigrationsAndValidate(TEST_DATABASE, 19, true, *DatabaseMigrations.all)

        database.query(
            "SELECT sleepScore, vo2Max, workoutRecommendationJson FROM daily_summaries",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(85f, cursor.getFloat(0), 0f)
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }

        val snapshotJson =
            "{\"ruleVersion\":1,\"wakeSessionId\":null,\"wakeTimeMs\":null," +
                "\"decision\":{\"state\":\"NO_SLEEP\",\"reasons\":[]},\"examples\":[]}"
        database.execSQL(
            "UPDATE daily_summaries SET workoutRecommendationJson = ? WHERE dateMidnightMs = ?",
            arrayOf<Any>(snapshotJson, 1_767_225_600_000L),
        )
        database.query(
            "SELECT workoutRecommendationJson FROM daily_summaries WHERE dateMidnightMs = ?",
            arrayOf<Any>(1_767_225_600_000L),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getString(0).contains("NO_SLEEP"))
        }
    }

    private fun seedDailySummary(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO daily_summaries (
                dateMidnightMs, sleepScore, diag_isCalibrating, diag_stagesSuspicious,
                diag_lateNadir, diag_hrvMissing, diag_timezoneJump
            ) VALUES (1767225600000, 85.0, 0, 0, 0, 0, 0)
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DATABASE = "migration-18-19-test"
    }
}
