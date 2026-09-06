package app.readylytics.health.docs

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Drift-detection for the Workout Recommendation feature's documentation, split out of
 * [DocumentationDriftTest] (which was pushed over detekt's `LargeClass` threshold by these
 * assertions) per Task 7 of the HRV workout-recommendations plan. Per `.claude/CLAUDE.md`'s
 * Documentation Synchronization Rule, ABOUT.md, docs/about.md, DATA_FLOW.md, and the in-app
 * `workout_recommendation_*` strings must describe the same behavior — this test fails if they
 * drift apart, and fails if the "Workout Recommendation" section is ever deleted from either
 * About page.
 */
class WorkoutRecommendationDocumentationDriftTest {
    private val aboutMd = readRepoFile("ABOUT.md")
    private val publicAboutMd = readRepoFile("docs/about.md")
    private val dataFlowMd = readRepoFile("internal-docs/DATA_FLOW.md")
    private val appStringsXml = readRepoFile("app/src/main/res/values/strings.xml")
    private val workoutRecommendationInfoBodyResource =
        Regex(
            """<string\s+name="workout_recommendation_info_body"[^>]*>(.*?)</string>""",
            RegexOption.DOT_MATCHES_ALL,
        ).find(appStringsXml)?.groupValues?.get(1)
            ?: error("Missing workout_recommendation_info_body resource")

    /**
     * Every phrase below is unique to the "Workout Recommendation" section — none is a bare
     * occurrence of the word "workout", which appears unrelated dozens of times elsewhere in these
     * same files (Workout GPS routes, TRIMP, Workouts tab, …) and would make a looser assertion
     * pass even if this section were deleted entirely.
     */
    @Test
    fun `workout recommendation documentation is synchronized across About surfaces`() {
        val requiredRecommendationPhrases =
            listOf(
                "## Workout Recommendation",
                "Rest, Easy workout, or Push",
                "±1.2 Athlete / ±1.5 Active / ±2.0 Sedentary",
                "a score below 60 caps guidance at Easy",
                "a Residual Fatigue value above 70 × your configured gain",
                "at least seven valid nights of data exist",
                "a missing HRV reading takes priority over any illness signal",
                "up to three of your own past workouts from the preceding 30 days",
                "one per distinct workout type, each longer than 15 minutes",
                "Tapping one opens that exact recorded workout",
                "shown by default and can be hidden from your Dashboard layout",
                "no generated TRIMP target, workout duration, or heart-rate target",
                "not a \"safe,\" \"optimal,\" or validated strength or training prescription",
                "it is not medical or training advice",
                "no network request is made to produce it",
                "included like any other computed data",
                "No cloud feature or telemetry was introduced",
            )

        for ((surface, text) in listOf(
            "ABOUT.md" to aboutMd,
            "docs/about.md" to publicAboutMd,
        )) {
            val normalized = normalizeWhitespace(text)
            for (phrase in requiredRecommendationPhrases) {
                assertTrue(normalized.contains(phrase), "$surface must contain '$phrase'")
            }
        }
    }

    @Test
    fun `workout recommendation is not called a safe, optimal, or validated prescription anywhere in About surfaces`() {
        for ((surface, text) in listOf(
            "ABOUT.md" to aboutMd,
            "docs/about.md" to publicAboutMd,
        )) {
            assertTrue(
                text.contains("not a safe, optimal, or validated training prescription"),
                "$surface honest-limitations entry for the Workout Recommendation is missing or reworded",
            )
        }
    }

    /**
     * `workout_recommendation_info_body` is the exact string shown behind the dashboard card's
     * "About this guidance" toggle. It is extracted by name (not just searched for in the whole
     * strings.xml file) so this test fails if that specific resource's copy drifts, rather than
     * passing on an unrelated "workout" mention somewhere else in `app/strings.xml`.
     */
    @Test
    fun `workout_recommendation_info_body explains what the guidance is based on`() {
        val normalized = normalizeWhitespace(workoutRecommendationInfoBodyResource)

        assertTrue(
            normalized.contains("This guidance compares this morning"),
            "workout_recommendation_info_body must open by naming the morning HRV input",
        )
        assertTrue(
            normalized.contains("last night"),
            "workout_recommendation_info_body must name last night's sleep as an input",
        )
        assertTrue(
            normalized.contains("your recent training fatigue"),
            "workout_recommendation_info_body must name recent training fatigue as an input",
        )
        assertTrue(
            normalized.contains("against your own usual range"),
            "workout_recommendation_info_body must state the comparison is against the user's own range",
        )
        assertTrue(
            normalized.contains("past workouts that matched a similar effort level"),
            "workout_recommendation_info_body must mention the past-workout examples",
        )
    }

    @Test
    fun `example fan-out call site documents the no-Health-Connect-fetch guarantee`() {
        val normalized = normalizeWhitespace(dataFlowMd)
        assertTrue(
            normalized.contains("this recompute never triggers a Health Connect fetch merely to repair examples"),
            "DATA_FLOW.md's DailySyncUseCase.resolveInlineOldestTargetDay section must restate the " +
                "no-Health-Connect-fetch-for-example-repair guarantee",
        )
    }

    @Test
    fun `morning snapshot determinism section documents the retention-bounded reproducibility caveat`() {
        val normalized = normalizeWhitespace(dataFlowMd)
        assertTrue(
            normalized.contains("Reproducibility is bounded by retention, not unconditional"),
            "DATA_FLOW.md §2.11.4 must document that recommendation replay is bounded by retained raw history",
        )
        assertTrue(
            normalized.contains("idempotent-within-a-tier"),
            "DATA_FLOW.md must tie the recommendation-replay caveat to the existing idempotent-within-a-tier doctrine",
        )
    }

    /** Collapses whitespace runs (including line wraps) to a single space for wrap-tolerant matching. */
    private fun normalizeWhitespace(text: String): String = text.replace(Regex("\\s+"), " ")

    private fun readRepoFile(pathFromRepoRoot: String): String {
        val candidates =
            listOf(
                File(pathFromRepoRoot),
                File("../$pathFromRepoRoot"),
                File("../../$pathFromRepoRoot"),
            )
        val file = candidates.firstOrNull { it.exists() }
        assertTrue(file != null, "could not locate $pathFromRepoRoot from working dir ${File(".").absolutePath}")
        return file.readText()
    }
}
