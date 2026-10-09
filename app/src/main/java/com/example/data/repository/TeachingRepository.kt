package com.example.data.repository

import com.example.data.local.TeachingDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CorrectionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.TaskSkillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TeachingRepository(private val dao: TeachingDao) {

    val allLessons: Flow<List<LessonEntity>> = dao.getAllLessons()
    val allCorrections: Flow<List<CorrectionEntity>> = dao.getAllCorrections()
    val activeCorrections: Flow<List<CorrectionEntity>> = dao.getActiveCorrections()
    val allTasks: Flow<List<TaskSkillEntity>> = dao.getAllTasks()
    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()

    fun searchLessons(query: String): Flow<List<LessonEntity>> = dao.searchLessons(query)
    fun searchTasks(query: String): Flow<List<TaskSkillEntity>> = dao.searchTasks(query)

    suspend fun getLessonById(id: Long): LessonEntity? = withContext(Dispatchers.IO) {
        dao.getLessonById(id)
    }

    suspend fun insertLesson(lesson: LessonEntity): Long = withContext(Dispatchers.IO) {
        dao.insertLesson(lesson)
    }

    suspend fun updateLesson(lesson: LessonEntity) = withContext(Dispatchers.IO) {
        dao.updateLesson(lesson)
    }

    suspend fun deleteLesson(lesson: LessonEntity) = withContext(Dispatchers.IO) {
        dao.deleteLesson(lesson)
    }

    suspend fun incrementLessonUse(id: Long) = withContext(Dispatchers.IO) {
        dao.incrementLessonUse(id)
    }

    suspend fun insertCorrection(correction: CorrectionEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCorrection(correction)
    }

    suspend fun updateCorrection(correction: CorrectionEntity) = withContext(Dispatchers.IO) {
        dao.updateCorrection(correction)
    }

    suspend fun deleteCorrection(correction: CorrectionEntity) = withContext(Dispatchers.IO) {
        dao.deleteCorrection(correction)
    }

    suspend fun setCorrectionActive(id: Long, isActive: Boolean) = withContext(Dispatchers.IO) {
        dao.setCorrectionActive(id, isActive)
    }

    suspend fun insertTask(task: TaskSkillEntity): Long = withContext(Dispatchers.IO) {
        dao.insertTask(task)
    }

    suspend fun updateTask(task: TaskSkillEntity) = withContext(Dispatchers.IO) {
        dao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskSkillEntity) = withContext(Dispatchers.IO) {
        dao.deleteTask(task)
    }

    suspend fun insertMessage(message: ChatMessageEntity): Long = withContext(Dispatchers.IO) {
        dao.insertMessage(message)
    }

    suspend fun updateMessageFeedback(id: Long, rating: Int) = withContext(Dispatchers.IO) {
        dao.updateMessageFeedback(id, rating)
    }

    suspend fun clearAllMessages() = withContext(Dispatchers.IO) {
        dao.clearAllMessages()
    }

    suspend fun getActiveCorrectionsSnapshot(): List<CorrectionEntity> = withContext(Dispatchers.IO) {
        dao.getActiveCorrectionsSnapshot()
    }

    suspend fun getAllLessonsSnapshot(): List<LessonEntity> = withContext(Dispatchers.IO) {
        dao.getAllLessonsSnapshot()
    }

    suspend fun getAllTasksSnapshot(): List<TaskSkillEntity> = withContext(Dispatchers.IO) {
        dao.getAllTasksSnapshot()
    }

    /**
     * Seeds initial practical lessons so a beginner user can immediately test
     * DO mode and see how lessons and rules guide the student AI.
     */
    suspend fun seedInitialKnowledgeIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllLessonsSnapshot()
        if (existing.isNotEmpty()) return@withContext

        // Starter Lesson 1: Professional Email
        val lesson1 = LessonEntity(
            title = "Professional Client Email",
            subject = "Communication",
            topic = "Email Writing",
            summary = "Strict guidelines for drafting concise and courteous client communications.",
            facts = "Clients value direct clarity and polite brevity. Wall of text emails are frequently ignored.",
            instructions = "1. Greet courteously (e.g., 'Hello [Name]').\n2. State the key purpose in the very first sentence.\n3. Limit total length to under 120 words.\n4. Use bullet points for any options or deadlines.\n5. Include a clear call to action.",
            examples = "Good: 'Hello Sarah, Here is the finalized draft for Friday's review. Please let me know your thoughts by 3 PM. Best regards, Alex.'\nBad: Casual greetings like 'Hey there buddy' or unstructured paragraphs.",
            constraints = "Never use casual abbreviations (like 'thx' or 'u'). Never use all uppercase for emphasis."
        )
        dao.insertLesson(lesson1)

        // Starter Lesson 2: Action Item Meeting Minutes
        val lesson2 = LessonEntity(
            title = "Executive Meeting Minutes",
            subject = "Productivity",
            topic = "Meeting Summaries",
            summary = "Extracting decisions and actionable tasks from messy meeting discussions.",
            facts = "Meetings result in decisions and commitments. Summaries must highlight who is responsible for what.",
            instructions = "1. Section 1: 3-sentence summary of the core discussion.\n2. Section 2: Key Decisions Agreed.\n3. Section 3: Action Items formatted as: '[ ] Task description — @Assignee (Due: Date)'.",
            examples = "Good: '[ ] Deploy v1.2 update — @DevTeam (Due: Oct 15)'",
            constraints = "Do not transcribe casual chatter or side comments. Keep strictly to outcomes."
        )
        dao.insertLesson(lesson2)

        // Starter Task: Executive Status Brief
        val task1 = TaskSkillEntity(
            name = "Draft Client Status Brief",
            purpose = "Convert raw notes into a polished 3-part client status update.",
            category = "Communication",
            stepMethod = "Step 1: Parse the user's progress notes.\nStep 2: Apply the 'Professional Client Email' rules.\nStep 3: Highlight accomplishments first, next steps second, blockers third.\nStep 4: Verify word count is under 120 words.",
            requiredInput = "Project name, achievements completed, next steps, any blockers",
            expectedOutput = "Formatted client email with greeting, status bullets, and signature",
            examples = "Input: Project Alpha, finished login, working on dashboard, waiting on logo\nOutput: Polished professional brief with clear milestones.",
            knownMistakes = "Forgetting to mention the blocked item or writing overly technical jargon.",
            corrections = "Latest correction: Always place blockers in a distinct caution section.",
            testCases = "Test Case: Input with 2 finished features and 1 delay."
        )
        dao.insertTask(task1)

        // Starter Correction
        val correction1 = CorrectionEntity(
            targetTopic = "Email Writing",
            originalMistake = "AI used informal sign-off 'Cheers' in client correspondence.",
            correctedRule = "In business and client emails, always sign off with 'Best regards,' or 'Sincerely,'. Never use 'Cheers' or 'Bye'.",
            explanation = "User corrected previous draft because 'Cheers' was too casual for corporate clients.",
            isActive = true
        )
        dao.insertCorrection(correction1)
    }

    /**
     * Exports all lessons, corrections, and tasks into human-readable Markdown.
     */
    suspend fun exportAllKnowledgeAsMarkdown(): String = withContext(Dispatchers.IO) {
        val lessons = dao.getAllLessonsSnapshot()
        val corrections = dao.getActiveCorrectionsSnapshot()
        val tasks = dao.getAllTasksSnapshot()

        buildString {
            appendLine("# 🎓 My Teaching AI — Notebook Export")
            appendLine("Exported on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine()

            appendLine("## 📚 Lessons Learned (${lessons.size})")
            if (lessons.isEmpty()) {
                appendLine("_No lessons saved yet._")
            } else {
                lessons.forEachIndexed { i, l ->
                    appendLine("### ${i + 1}. ${l.title} [${l.subject} / ${l.topic}]")
                    appendLine("- **Summary**: ${l.summary}")
                    if (l.facts.isNotBlank()) appendLine("- **Key Facts**:\n${l.facts}")
                    if (l.instructions.isNotBlank()) appendLine("- **Instructions / Rules**:\n${l.instructions}")
                    if (l.examples.isNotBlank()) appendLine("- **Examples**:\n${l.examples}")
                    if (l.constraints.isNotBlank()) appendLine("- **Constraints**:\n${l.constraints}")
                    appendLine()
                }
            }

            appendLine("## 🛠️ Task Library (${tasks.size})")
            if (tasks.isEmpty()) {
                appendLine("_No task skills saved yet._")
            } else {
                tasks.forEachIndexed { i, t ->
                    appendLine("### ${i + 1}. ${t.name} (${t.category})")
                    appendLine("- **Purpose**: ${t.purpose}")
                    appendLine("- **Step-by-Step Method**:\n${t.stepMethod}")
                    if (t.requiredInput.isNotBlank()) appendLine("- **Required Input**: ${t.requiredInput}")
                    if (t.expectedOutput.isNotBlank()) appendLine("- **Expected Output**: ${t.expectedOutput}")
                    appendLine()
                }
            }

            appendLine("## ⚠️ Active Corrections & Feedback (${corrections.size})")
            if (corrections.isEmpty()) {
                appendLine("_No active corrections._")
            } else {
                corrections.forEachIndexed { i, c ->
                    appendLine("### ${i + 1}. Topic: ${c.targetTopic}")
                    appendLine("- **Previous Mistake**: ${c.originalMistake}")
                    appendLine("- **New Golden Rule**: ${c.correctedRule}")
                    if (c.explanation.isNotBlank()) appendLine("- **Note**: ${c.explanation}")
                    appendLine()
                }
            }
        }
    }
}
