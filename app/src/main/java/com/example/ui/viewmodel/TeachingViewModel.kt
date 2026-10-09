package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.EngineResult
import com.example.ai.LessonDraft
import com.example.ai.TeachingEngine
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CorrectionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.TaskSkillEntity
import com.example.data.repository.TeachingRepository
import com.example.speech.SpeechManager
import com.example.speech.SpeechState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class AppMode {
    TEACH,
    DO
}

class TeachingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = TeachingRepository(database.teachingDao())
    val speechManager = SpeechManager(application)
    private val engine = TeachingEngine()

    // Mode: TEACH or DO
    private val _currentMode = MutableStateFlow(AppMode.TEACH)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    // Persistent database flows
    val allLessons: StateFlow<List<LessonEntity>> = repository.allLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCorrections: StateFlow<List<CorrectionEntity>> = repository.allCorrections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskSkillEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Input state
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // UI Loading state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Pending lesson draft for confirmation
    private val _pendingLessonDraft = MutableStateFlow<LessonDraft?>(null)
    val pendingLessonDraft: StateFlow<LessonDraft?> = _pendingLessonDraft.asStateFlow()

    // Pending correction confirmation
    private val _pendingCorrection = MutableStateFlow<CorrectionEntity?>(null)
    val pendingCorrection: StateFlow<CorrectionEntity?> = _pendingCorrection.asStateFlow()

    // Notification / Toast event
    private val _snackBarEvent = MutableSharedFlow<String>()
    val snackBarEvent: SharedFlow<String> = _snackBarEvent.asSharedFlow()

    // Speech states forwarded
    val speechState: StateFlow<SpeechState> = speechManager.speechState
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking

    init {
        viewModelScope.launch {
            repository.seedInitialKnowledgeIfEmpty()
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setMode(mode: AppMode) {
        _currentMode.value = mode
    }

    fun toggleMode() {
        _currentMode.value = if (_currentMode.value == AppMode.TEACH) AppMode.DO else AppMode.TEACH
    }

    fun onMicrophoneClick() {
        val currentState = speechManager.speechState.value
        if (currentState is SpeechState.Listening) {
            speechManager.stopListening()
        } else {
            speechManager.startListening { spokenText ->
                if (spokenText.isNotBlank()) {
                    _inputText.value = spokenText
                    sendMessage(spokenText)
                }
            }
        }
    }

    fun cancelSpeechListening() {
        speechManager.cancelListening()
    }

    fun speakText(text: String) {
        speechManager.speak(text)
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    fun sendMessage(userText: String? = null) {
        val message = (userText ?: _inputText.value).trim()
        if (message.isBlank() || _isProcessing.value) return

        _inputText.value = ""
        _isProcessing.value = true

        viewModelScope.launch {
            // 1. Insert User message
            val mode = _currentMode.value
            val userMsg = ChatMessageEntity(
                role = "user",
                mode = mode.name,
                content = message
            )
            repository.insertMessage(userMsg)

            try {
                // Check if user is issuing an explicit correction to the previous assistant output
                val lastAssistant = chatMessages.value.lastOrNull { it.role == "assistant" }
                val isCorrectionPhrase = message.startsWith("that's wrong", ignoreCase = true) ||
                        message.startsWith("that is wrong", ignoreCase = true) ||
                        message.startsWith("no, ", ignoreCase = true) ||
                        message.startsWith("you made a mistake", ignoreCase = true) ||
                        message.startsWith("correction:", ignoreCase = true)

                if (isCorrectionPhrase && lastAssistant != null) {
                    val topic = deriveTargetTopic(lastAssistant)
                    val correction = engine.extractCorrection(message, topic, lastAssistant.content)
                    _pendingCorrection.value = correction

                    val replyText = "⚠️ **Correction Noted!**\n\nI understand: \"${correction.correctedRule}\".\n\nWould you like me to save this correction to my permanent memory so I don't repeat the mistake?"
                    val assistantMsg = ChatMessageEntity(
                        role = "assistant",
                        mode = mode.name,
                        content = replyText
                    )
                    repository.insertMessage(assistantMsg)
                    speechManager.speak(replyText)
                    _isProcessing.value = false
                    return@launch
                }

                // Normal Mode Handling
                val result: EngineResult = if (mode == AppMode.TEACH) {
                    val currentLessons = repository.getAllLessonsSnapshot()
                    engine.processTeachInput(message, currentLessons)
                } else {
                    val lessons = repository.getAllLessonsSnapshot()
                    val corrections = repository.getActiveCorrectionsSnapshot()
                    val tasks = repository.getAllTasksSnapshot()
                    engine.processDoInput(message, lessons, corrections, tasks)
                }

                // If TEACH produced a draft, set it for user confirmation
                if (result.lessonDraft != null) {
                    _pendingLessonDraft.value = result.lessonDraft
                }

                // Format referenced lessons JSON for UI badge display
                val referencedJson = if (result.referencedLessons.isNotEmpty() || result.referencedCorrections.isNotEmpty()) {
                    val arr = JSONArray()
                    result.referencedLessons.forEach { arr.put("Lesson: $it") }
                    result.referencedCorrections.forEach { arr.put("Correction: $it") }
                    arr.toString()
                } else null

                val assistantMsg = ChatMessageEntity(
                    role = "assistant",
                    mode = mode.name,
                    content = result.textResponse,
                    lessonDraftJson = result.lessonDraft?.toJson(),
                    lessonsReferencedJson = referencedJson
                )
                repository.insertMessage(assistantMsg)

                // Increment lesson usage if referenced
                if (mode == AppMode.DO && result.referencedLessons.isNotEmpty()) {
                    val currentLessons = repository.getAllLessonsSnapshot()
                    result.referencedLessons.forEach { title ->
                        currentLessons.firstOrNull { it.title.equals(title, ignoreCase = true) }?.let {
                            repository.incrementLessonUse(it.id)
                        }
                    }
                }

                // Speak assistant answer if not in silent mode
                speechManager.speak(result.textResponse)

            } catch (e: Exception) {
                val errorMsg = "Sorry, I had trouble processing that: ${e.localizedMessage ?: "Unknown error"}"
                repository.insertMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        mode = mode.name,
                        content = errorMsg
                    )
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun deriveTargetTopic(lastMessage: ChatMessageEntity): String {
        return if (!lastMessage.lessonsReferencedJson.isNullOrBlank()) {
            "Previous Task"
        } else {
            "General Skills"
        }
    }

    fun confirmSaveLesson(draft: LessonDraft) {
        viewModelScope.launch {
            val entity = LessonEntity(
                title = draft.title,
                subject = draft.subject,
                topic = draft.topic,
                summary = draft.summary,
                facts = draft.facts,
                instructions = draft.instructions,
                examples = draft.examples,
                constraints = draft.constraints
            )
            val newId = repository.insertLesson(entity)
            _pendingLessonDraft.value = null

            val confirmMsg = ChatMessageEntity(
                role = "assistant",
                mode = AppMode.TEACH.name,
                content = "✅ **Lesson Saved!**\n\nI have permanently stored **\"${draft.title}\"** into my notebook. I will remember these rules whenever you ask me to do tasks in **DO mode**."
            )
            repository.insertMessage(confirmMsg)
            _snackBarEvent.emit("Saved '${draft.title}' to Notebook!")
            speechManager.speak("Saved ${draft.title} to my notebook!")
        }
    }

    fun dismissLessonDraft() {
        _pendingLessonDraft.value = null
    }

    fun confirmSaveCorrection(correction: CorrectionEntity) {
        viewModelScope.launch {
            repository.insertCorrection(correction)
            _pendingCorrection.value = null

            val confirmMsg = ChatMessageEntity(
                role = "assistant",
                mode = AppMode.DO.name,
                content = "✅ **Correction Remembered!**\n\nI've committed the new rule: *\"${correction.correctedRule}\"* to memory. It now has top priority when doing ${correction.targetTopic} tasks."
            )
            repository.insertMessage(confirmMsg)
            _snackBarEvent.emit("Correction saved with top priority!")
            speechManager.speak("Correction saved! I will apply this rule from now on.")
        }
    }

    fun dismissCorrection() {
        _pendingCorrection.value = null
    }

    fun rateMessage(messageId: Long, rating: Int, onSuggestCorrection: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateMessageFeedback(messageId, rating)
            if (rating == -1) {
                _snackBarEvent.emit("Thank you for the feedback. Tell me what was wrong to correct it!")
                onSuggestCorrection?.invoke()
            } else if (rating == 1) {
                _snackBarEvent.emit("Glad I followed your instructions!")
            }
        }
    }

    fun saveOrUpdateLesson(lesson: LessonEntity) {
        viewModelScope.launch {
            if (lesson.id == 0L) {
                repository.insertLesson(lesson)
                _snackBarEvent.emit("New lesson added!")
            } else {
                repository.updateLesson(lesson.copy(updatedAt = System.currentTimeMillis()))
                _snackBarEvent.emit("Lesson updated!")
            }
        }
    }

    fun deleteLesson(lesson: LessonEntity) {
        viewModelScope.launch {
            repository.deleteLesson(lesson)
            _snackBarEvent.emit("Lesson deleted from notebook.")
        }
    }

    fun saveOrUpdateTask(task: TaskSkillEntity) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.insertTask(task)
                _snackBarEvent.emit("New skill added to Task Library!")
            } else {
                repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
                _snackBarEvent.emit("Task skill updated!")
            }
        }
    }

    fun deleteTask(task: TaskSkillEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _snackBarEvent.emit("Task removed from library.")
        }
    }

    fun toggleCorrectionActive(correction: CorrectionEntity) {
        viewModelScope.launch {
            repository.setCorrectionActive(correction.id, !correction.isActive)
        }
    }

    fun deleteCorrection(correction: CorrectionEntity) {
        viewModelScope.launch {
            repository.deleteCorrection(correction)
            _snackBarEvent.emit("Correction removed.")
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearAllMessages()
            _snackBarEvent.emit("Conversation history cleared.")
        }
    }

    fun exportNotebook(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val md = repository.exportAllKnowledgeAsMarkdown()
            onReady(md)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
