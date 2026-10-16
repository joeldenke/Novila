package se.novila.coach

import se.novila.exercise.Dosage
import se.novila.exercise.ExerciseType
import java.time.format.TextStyle
import java.util.Locale

/** Whether the on-device language model can be used, as the system reports it. */
enum class AssistantAvailability { Unknown, Unavailable, Downloadable, Downloading, Available }

/**
 * A language model that runs on the device (Gemini Nano through AICore). The coach's plan never comes from
 * it: the model only puts the plan the planner made, and its reasons, into a few personal sentences.
 * Implementations never throw, apart from cancellation.
 */
interface CoachLanguageModel {
    suspend fun availability(): AssistantAvailability

    /** Asks the system to download the model. True once it is there. */
    suspend fun download(): Boolean

    suspend fun write(prompt: String): Result<String>
}

/**
 * What the prompt needs that only the UI knows: the user's [locale], every exercise's [exerciseNames] and the
 * plan's reasons as the sentences the screen shows ([notes]), all in the user's language.
 */
data class CoachPromptInput(val locale: Locale, val exerciseNames: Map<String, String>, val notes: List<String>)

/**
 * Builds the prompt for the coach's message and cleans the answer. Plain Kotlin, so it is unit tested. The
 * prompt holds the plan as facts and asks for a short, calm explanation in the user's language, without
 * changing the plan, giving medical advice or using emojis (Novila shows none).
 */
object CoachPrompt {
    const val MAX_LENGTH = 700

    fun build(plan: CoachPlan, preferences: CoachPreferences, input: CoachPromptInput): String {
        val language = if (input.locale.language == "sv") "Swedish" else "English"
        val week = plan.program.sessions.joinToString("\n") { session ->
            val day = session.day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val what = when {
                session.exercises.isNotEmpty() -> session.exercises.joinToString(", ") { planned ->
                    val name = input.exerciseNames[planned.exerciseId] ?: planned.exerciseId
                    val unit = if (planned.dosage == Dosage.Seconds) " s" else ""
                    val side = if (planned.perSide) " per side" else ""
                    "$name ${planned.sets} x ${planned.amount}$unit$side"
                }

                session.type == ExerciseType.Mobility -> "mobility and stretching"

                else -> session.type.name.lowercase(Locale.ENGLISH)
            }
            "- $day (${session.type.name.lowercase(Locale.ENGLISH)}, ${session.minutes} min): $what"
        }
        val goal = when (preferences.goal) {
            CoachGoal.Rehab -> "rehab for the ${rehabArea(preferences.rehabArea)}, phase ${preferences.rehabPhase} of 3"
            else -> preferences.goal.name.lowercase(Locale.ENGLISH)
        }
        val rehab = if (preferences.goal == CoachGoal.Rehab) {
            "\nRemind them to stop and contact a physiotherapist or doctor if the pain gets worse. " +
                "Never diagnose or promise recovery."
        } else {
            ""
        }
        val days = preferences.daysPerWeek
        val minutes = preferences.sessionMinutes
        return """
            |You are a calm, encouraging training coach in the health app Novila.
            |The user's goal: $goal. They train $days days a week, about $minutes minutes.
            |This week's program, already decided:
            |$week
            |Why it looks like this:
            |${input.notes.joinToString("\n") { "- $it" }}
            |Write 3 or 4 short sentences in $language to the user: what the week is about, why, and one practical
            |tip for the first session. Only use the exercises and numbers above; do not add or change anything.
            |No lists, headings, markdown or emojis. Give no medical advice.$rehab
        """.trimMargin()
    }

    /** The model's answer as plain text: no emojis or markdown, at most [MAX_LENGTH] characters, whole sentences. */
    fun clean(answer: String): String {
        val plain = buildString {
            var i = 0
            while (i < answer.length) {
                val codePoint = answer.codePointAt(i)
                if (!isEmoji(codePoint)) appendCodePoint(codePoint)
                i += Character.charCount(codePoint)
            }
        }
            .replace(Regex("[*_#`]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (plain.length <= MAX_LENGTH) return plain
        val cut = plain.take(MAX_LENGTH)
        val end = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
        return if (end > 0) cut.take(end + 1) else cut.trimEnd() + "…"
    }

    private fun isEmoji(codePoint: Int): Boolean =
        codePoint in 0x1F000..0x1FAFF ||
            codePoint in 0x2600..0x27BF ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint == 0x200D

    private fun rehabArea(area: RehabArea) = when (area) {
        RehabArea.Knee -> "knee"
        RehabArea.LowerBack -> "lower back"
    }
}
