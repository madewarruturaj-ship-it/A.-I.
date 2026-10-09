package com.example.ai

import com.example.data.model.CorrectionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.TaskSkillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LessonDraft(
    val title: String,
    val subject: String,
    val topic: String,
    val summary: String,
    val facts: String,
    val instructions: String,
    val examples: String,
    val constraints: String,
    val followUpQuestion: String? = null
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("title", title)
        obj.put("subject", subject)
        obj.put("topic", topic)
        obj.put("summary", summary)
        obj.put("facts", facts)
        obj.put("instructions", instructions)
        obj.put("examples", examples)
        obj.put("constraints", constraints)
        obj.put("followUpQuestion", followUpQuestion ?: "")
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): LessonDraft? {
            return try {
                val obj = JSONObject(jsonStr)
                LessonDraft(
                    title = obj.optString("title", "Untitled Lesson"),
                    subject = obj.optString("subject", "General"),
                    topic = obj.optString("topic", "General"),
                    summary = obj.optString("summary", ""),
                    facts = obj.optString("facts", ""),
                    instructions = obj.optString("instructions", ""),
                    examples = obj.optString("examples", ""),
                    constraints = obj.optString("constraints", ""),
                    followUpQuestion = obj.optString("followUpQuestion").takeIf { it.isNotBlank() }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class EngineResult(
    val textResponse: String,
    val lessonDraft: LessonDraft? = null,
    val referencedLessons: List<String> = emptyList(),
    val referencedCorrections: List<String> = emptyList(),
    val isHonestNotice: Boolean = false
)

class TeachingEngine {

    suspend fun processTeachInput(
        userMessage: String,
        existingLessons: List<LessonEntity>
    ): EngineResult = withContext(Dispatchers.Default) {
        val trimmed = userMessage.trim()

        // If Gemini is available, we try structured extraction with Gemini first
        if (GeminiApiClient.isApiKeyConfigured()) {
            val systemPrompt = """
                You are a humble, attentive AI student being taught by your human teacher.
                The user is teaching you a topic, procedure, rule, or fact.
                Your task:
                1. Identify key facts, step-by-step instructions, examples (good/bad), and constraints (things to avoid).
                2. If the user's explanation is very vague or missing crucial steps, generate a polite follow-up question.
                3. Respond in valid JSON with these exact keys:
                {
                  "reply": "Friendly response acknowledging what you learned and asking confirmation to save",
                  "title": "Short descriptive lesson title",
                  "subject": "Broad subject (e.g. Writing, Coding, Cooking, Productivity, Personal, Other)",
                  "topic": "Specific topic",
                  "summary": "1-2 sentence core takeaway",
                  "facts": "Key factual knowledge separated by newlines",
                  "instructions": "Numbered step-by-step rules or guidelines",
                  "examples": "Good and bad examples",
                  "constraints": "Constraints, warnings, or things to avoid",
                  "followUp": "Follow-up question if unclear, or empty string"
                }
            """.trimIndent()

            val aiResult = GeminiApiClient.generateResponse(systemPrompt, trimmed)
            if (aiResult.isSuccess) {
                val raw = aiResult.getOrNull() ?: ""
                val parsed = parseLessonDraftFromJson(raw)
                if (parsed != null) {
                    val reply = parsed.first
                    val draft = parsed.second
                    return@withContext EngineResult(
                        textResponse = reply,
                        lessonDraft = draft
                    )
                }
            }
        }

        // Built-in intelligent Knowledge Extraction Engine (Offline / Free-First)
        extractDraftLocally(trimmed)
    }

    private fun extractDraftLocally(input: String): EngineResult {
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val words = input.split(Regex("\\s+"))

        // Detect if input is too short or ambiguous
        val isVeryBrief = words.size < 6
        val followUp = if (isVeryBrief) {
            "Could you give me an example or specify the exact steps you'd like me to follow when doing this?"
        } else null

        // Derive title & subject heuristically
        val title = deriveTitle(input)
        val subject = deriveSubject(input)
        val topic = deriveTopic(input, title)

        // Separate facts, instructions, examples, and constraints
        val factsList = mutableListOf<String>()
        val instructionsList = mutableListOf<String>()
        val examplesList = mutableListOf<String>()
        val constraintsList = mutableListOf<String>()

        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("never") || lower.startsWith("don't") || lower.startsWith("do not") || lower.contains("avoid") -> {
                    constraintsList.add("• " + line.removePrefix("- ").removePrefix("• "))
                }
                lower.startsWith("example:") || lower.startsWith("e.g.") || lower.contains("for example") || lower.contains("good:") || lower.contains("bad:") -> {
                    examplesList.add("• " + line.removePrefix("- ").removePrefix("• "))
                }
                Regex("^(\\d+[.)]|step\\s+\\d+|always|first|second|then|finally)", RegexOption.IGNORE_CASE).containsMatchIn(line) -> {
                    instructionsList.add("• " + line.removePrefix("- ").removePrefix("• "))
                }
                else -> {
                    factsList.add("• " + line.removePrefix("- ").removePrefix("• "))
                }
            }
        }

        // If instructions are empty but we have input, treat the main body as instruction
        if (instructionsList.isEmpty() && lines.isNotEmpty()) {
            instructionsList.add("• Apply rule: $input")
        }

        val draft = LessonDraft(
            title = title,
            subject = subject,
            topic = topic,
            summary = "User taught: ${input.take(140)}${if (input.length > 140) "..." else ""}",
            facts = if (factsList.isNotEmpty()) factsList.joinToString("\n") else "• User provided direct rule instructions.",
            instructions = instructionsList.joinToString("\n"),
            examples = if (examplesList.isNotEmpty()) examplesList.joinToString("\n") else "• Follow the user's explicit guidelines as taught.",
            constraints = if (constraintsList.isNotEmpty()) constraintsList.joinToString("\n") else "• Adhere strictly to the taught parameters.",
            followUpQuestion = followUp
        )

        val reply = buildString {
            append("🎓 **Understood! I took notes on this lesson.**\n\n")
            append("I've outlined the lesson as **\"$title\"** under **$subject**.\n\n")
            if (followUp != null) {
                append("❓ **Quick Clarification:** $followUp\n\n")
            }
            append("Review the structured lesson card below. Tap **Confirm & Save Lesson** when you're happy, and I'll keep it permanently in my memory!")
        }

        return EngineResult(
            textResponse = reply,
            lessonDraft = draft
        )
    }

    private fun deriveTitle(input: String): String {
        val clean = input.replace(Regex("^(teach me|i want to teach you|remember that|here is how to|how to|rule for)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        val firstSentence = clean.split(Regex("[.!?\n]")).firstOrNull()?.trim() ?: clean
        val titleWords = firstSentence.split(Regex("\\s+")).take(5).joinToString(" ")
        return if (titleWords.length in 4..40) {
            titleWords.replaceFirstChar { it.uppercase() }
        } else {
            "Lesson: ${clean.take(25).trim().replaceFirstChar { it.uppercase() }}"
        }
    }

    private fun deriveSubject(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("email") || lower.contains("write") || lower.contains("message") || lower.contains("reply") -> "Writing & Communication"
            lower.contains("code") || lower.contains("python") || lower.contains("kotlin") || lower.contains("bug") || lower.contains("function") -> "Software & Tech"
            lower.contains("meeting") || lower.contains("summary") || lower.contains("task") || lower.contains("work") -> "Productivity"
            lower.contains("cook") || lower.contains("recipe") || lower.contains("food") -> "Cooking & Food"
            lower.contains("workout") || lower.contains("health") || lower.contains("diet") -> "Health & Fitness"
            else -> "General Knowledge"
        }
    }

    private fun deriveTopic(input: String, title: String): String {
        return title.replace(Regex("^(Lesson:|How to)\\s*", RegexOption.IGNORE_CASE), "").trim()
    }

    private fun parseLessonDraftFromJson(raw: String): Pair<String, LessonDraft>? {
        return try {
            val jsonStr = raw.substringAfter("{").substringBeforeLast("}")
            val fullJson = "{$jsonStr}"
            val obj = JSONObject(fullJson)
            val reply = obj.optString("reply", "I have structured the lesson as requested.")
            val draft = LessonDraft(
                title = obj.optString("title", "Saved Lesson"),
                subject = obj.optString("subject", "General"),
                topic = obj.optString("topic", "General"),
                summary = obj.optString("summary", ""),
                facts = obj.optString("facts", ""),
                instructions = obj.optString("instructions", ""),
                examples = obj.optString("examples", ""),
                constraints = obj.optString("constraints", ""),
                followUpQuestion = obj.optString("followUp").takeIf { it.isNotBlank() }
            )
            Pair(reply, draft)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun processDoInput(
        userMessage: String,
        lessons: List<LessonEntity>,
        corrections: List<CorrectionEntity>,
        tasks: List<TaskSkillEntity>
    ): EngineResult = withContext(Dispatchers.Default) {
        val trimmed = userMessage.trim().lowercase()

        // 1. Search relevant lessons based on keyword match
        val matchedLessons = lessons.filter { l ->
            val titleMatches = l.title.lowercase().split(" ").any { it.length > 3 && trimmed.contains(it) }
            val topicMatches = l.topic.lowercase().split(" ").any { it.length > 3 && trimmed.contains(it) }
            val subjectMatches = l.subject.lowercase().split(" ").any { it.length > 3 && trimmed.contains(it) }
            val contentMatches = l.instructions.lowercase().split(" ").any { it.length > 4 && trimmed.contains(it) }
            titleMatches || topicMatches || subjectMatches || contentMatches
        }.take(3)

        // 2. Search relevant active corrections (highest priority)
        val matchedCorrections = corrections.filter { c ->
            c.isActive && (
                c.targetTopic.lowercase().split(" ").any { it.length > 3 && trimmed.contains(it) } ||
                c.correctedRule.lowercase().split(" ").any { it.length > 4 && trimmed.contains(it) }
            )
        }

        // 3. Search matching tasks
        val matchedTask = tasks.firstOrNull { t ->
            t.name.lowercase().split(" ").any { it.length > 3 && trimmed.contains(it) } ||
            t.purpose.lowercase().split(" ").any { it.length > 3 && trimmed.contains(it) }
        }

        // FEATURE 8: Honesty and Reliability
        // "Never invent a lesson that I did not teach. If no relevant lesson exists, say so."
        if (matchedLessons.isEmpty() && matchedTask == null && lessons.isEmpty()) {
            return@withContext EngineResult(
                textResponse = "🔍 **No Lessons in Memory Yet**\n\nI haven't been taught any lessons or rules yet. My notebook is currently empty!\n\nSwitch to **TEACH mode** (or tap the TEACH toggle above) and explain what you'd like me to learn, or add a task in the **Task Library**.",
                isHonestNotice = true
            )
        }

        if (matchedLessons.isEmpty() && matchedTask == null) {
            return@withContext EngineResult(
                textResponse = "🔍 **No Matching Lessons Found**\n\nI looked through my saved notebook of **${lessons.size} lessons**, but I do not have any instructions for this specific request.\n\nTo ensure I never invent rules you haven't approved:\n1. Switch to **TEACH mode** to teach me this skill.\n2. Or check the **Notebook** tab to see what I currently know.",
                isHonestNotice = true
            )
        }

        val referencedLessonsList = matchedLessons.map { it.title }
        val referencedCorrectionsList = matchedCorrections.map { it.correctedRule }

        // If Gemini is available, pass the retrieved context as grounding
        if (GeminiApiClient.isApiKeyConfigured()) {
            val groundingPrompt = buildString {
                appendLine("You are 'My Teaching AI', a faithful personal apprentice. You execute tasks STRICTLY following the lessons and corrections taught by your human teacher.")
                appendLine("CRITICAL RULES:")
                appendLine("1. You must obey the user's taught instructions and constraints above all else.")
                appendLine("2. If a correction is provided, it OVERRIDES any previous instruction.")
                appendLine("3. You must explicitly list at the beginning or end which lessons and corrections you applied.")
                appendLine("4. Never invent rules not taught.")
                appendLine()
                appendLine("--- RETRIEVED LESSONS ---")
                matchedLessons.forEach { l ->
                    appendLine("Lesson: ${l.title}")
                    appendLine("Instructions: ${l.instructions}")
                    appendLine("Constraints: ${l.constraints}")
                    appendLine("Examples: ${l.examples}")
                }
                if (matchedTask != null) {
                    appendLine("--- REUSABLE TASK ---")
                    appendLine("Task: ${matchedTask.name}")
                    appendLine("Procedure: ${matchedTask.stepMethod}")
                    appendLine("Expected Output: ${matchedTask.expectedOutput}")
                }
                if (matchedCorrections.isNotEmpty()) {
                    appendLine("--- ACTIVE USER CORRECTIONS (HIGHEST PRIORITY) ---")
                    matchedCorrections.forEach { c ->
                        appendLine("Correction for ${c.targetTopic}: ${c.correctedRule}")
                    }
                }
            }

            val aiResult = GeminiApiClient.generateResponse(groundingPrompt, userMessage)
            if (aiResult.isSuccess) {
                val response = aiResult.getOrNull() ?: ""
                return@withContext EngineResult(
                    textResponse = response,
                    referencedLessons = referencedLessonsList,
                    referencedCorrections = referencedCorrectionsList
                )
            }
        }

        // Local execution engine if offline or no key
        val localResponse = buildString {
            append("⚡ **Task Executed Using Saved Memory**\n\n")
            if (matchedTask != null) {
                append("📋 **Method Applied:** ${matchedTask.name}\n")
                append("${matchedTask.stepMethod}\n\n")
            }

            append("📝 **Output:**\n")
            append("> Working with input: \"$userMessage\"\n\n")
            matchedLessons.forEach { l ->
                append("Applying guidelines from **${l.title}**:\n")
                append("${l.instructions}\n\n")
                if (l.constraints.isNotBlank()) {
                    append("⚠️ **Strict Constraints Respected:**\n${l.constraints}\n\n")
                }
            }

            if (matchedCorrections.isNotEmpty()) {
                append("🛡️ **Active Corrections Applied (Top Priority):**\n")
                matchedCorrections.forEach { c ->
                    append("• ${c.correctedRule}\n")
                }
                append("\n")
            }

            append("---\n")
            append("💡 _Did I follow your rules correctly? You can give feedback below or tell me if anything was wrong to save a new correction._")
        }

        EngineResult(
            textResponse = localResponse,
            referencedLessons = referencedLessonsList,
            referencedCorrections = referencedCorrectionsList
        )
    }

    /**
     * Extracts a new correction when the user points out a mistake.
     */
    fun extractCorrection(
        userFeedback: String,
        targetTopic: String,
        lastAssistantOutput: String
    ): CorrectionEntity {
        val cleanMistake = if (lastAssistantOutput.isNotBlank()) {
            lastAssistantOutput.take(120)
        } else {
            "Previous output was flagged as incorrect."
        }

        val rule = userFeedback
            .replace(Regex("^(that's wrong because|that is wrong|incorrect|no,|don't do that|fix this:)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .replaceFirstChar { it.uppercase() }

        return CorrectionEntity(
            targetTopic = targetTopic.ifBlank { "General Tasks" },
            originalMistake = cleanMistake,
            correctedRule = if (rule.isNotBlank()) rule else userFeedback,
            explanation = "User corrected previous mistake. This rule will take precedence in future tasks.",
            isActive = true
        )
    }
}
