package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.LessonDraft
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CorrectionEntity
import com.example.data.model.LessonEntity
import com.example.speech.SpeechState
import com.example.ui.components.ApprenticeHeader
import com.example.ui.components.LessonDetailDialog
import com.example.ui.components.LessonDraftCard
import com.example.ui.components.VoiceInputBar
import com.example.ui.theme.DoBlue
import com.example.ui.theme.TeachGreen
import com.example.ui.viewmodel.AppMode
import com.example.ui.viewmodel.TeachingViewModel
import org.json.JSONArray

@Composable
fun AssistantScreen(
    viewModel: TeachingViewModel,
    modifier: Modifier = Modifier
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val lessons by viewModel.allLessons.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val corrections by viewModel.allCorrections.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val pendingCorrection by viewModel.pendingCorrection.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var editingLessonDraft by remember { mutableStateOf<LessonDraft?>(null) }
    var correctionDialogTarget by remember { mutableStateOf<ChatMessageEntity?>(null) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("assistant_screen")
    ) {
        // AI Apprentice Student Header with Mode Switch
        ApprenticeHeader(
            currentMode = currentMode,
            lessonCount = lessons.size,
            taskCount = tasks.size,
            correctionCount = corrections.filter { it.isActive }.size,
            onModeSelect = { viewModel.setMode(it) }
        )

        // Chat Timeline
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                EmptyChatWelcome(
                    currentMode = currentMode,
                    onExampleClick = { viewModel.sendMessage(it) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            isSpeaking = isSpeaking,
                            onSpeak = { viewModel.speakText(it) },
                            onStopSpeak = { viewModel.stopSpeaking() },
                            onRate = { rating ->
                                viewModel.rateMessage(msg.id, rating) {
                                    correctionDialogTarget = msg
                                }
                            },
                            onConfirmDraft = { draft -> viewModel.confirmSaveLesson(draft) },
                            onEditDraft = { draft -> editingLessonDraft = draft },
                            onDismissDraft = { viewModel.dismissLessonDraft() }
                        )
                    }
                }
            }
        }

        // Voice and Text Input Bar
        VoiceInputBar(
            currentMode = currentMode,
            inputText = inputText,
            onInputTextChange = { viewModel.setInputText(it) },
            onSend = { viewModel.sendMessage() },
            speechState = speechState,
            onMicrophoneClick = { viewModel.onMicrophoneClick() },
            onCancelListening = { viewModel.cancelSpeechListening() },
            isProcessing = isProcessing,
            onSuggestionClick = { suggestion ->
                viewModel.setInputText(suggestion)
                viewModel.sendMessage(suggestion)
            }
        )
    }

    // Pending Correction Confirmation Dialog
    if (pendingCorrection != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCorrection() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save New Correction Rule?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "I've extracted your correction:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pendingCorrection!!.correctedRule,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "This will override older instructions and be applied to all future tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSaveCorrection(pendingCorrection!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = TeachGreen)
                ) {
                    Text("Confirm & Remember")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissCorrection() }) {
                    Text("Discard")
                }
            }
        )
    }

    // Manual Correction Feedback Dialog when user taps thumbs down
    if (correctionDialogTarget != null) {
        var userMistakeNote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { correctionDialogTarget = null },
            title = {
                Text("What went wrong?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tell me what was incorrect so I can learn the new rule:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = userMistakeNote,
                        onValueChange = { userMistakeNote = it },
                        placeholder = { Text("E.g.: Never use casual greetings with clients; always include deadline...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("correction_input_dialog")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userMistakeNote.isNotBlank()) {
                            viewModel.sendMessage("Correction: $userMistakeNote")
                            correctionDialogTarget = null
                        }
                    },
                    enabled = userMistakeNote.isNotBlank()
                ) {
                    Text("Submit Correction")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { correctionDialogTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Draft Dialog
    if (editingLessonDraft != null) {
        val draft = editingLessonDraft!!
        val mockLesson = LessonEntity(
            title = draft.title,
            subject = draft.subject,
            topic = draft.topic,
            summary = draft.summary,
            facts = draft.facts,
            instructions = draft.instructions,
            examples = draft.examples,
            constraints = draft.constraints
        )
        LessonDetailDialog(
            initialLesson = mockLesson,
            onDismiss = { editingLessonDraft = null },
            onSave = { updated ->
                viewModel.saveOrUpdateLesson(updated)
                editingLessonDraft = null
                viewModel.dismissLessonDraft()
            }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit,
    onRate: (Int) -> Unit,
    onConfirmDraft: (LessonDraft) -> Unit,
    onEditDraft: (LessonDraft) -> Unit,
    onDismissDraft: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = if (message.mode == "TEACH") TeachGreen else DoBlue,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (message.mode == "TEACH") Icons.Default.School else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.92f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Role & Mode Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = if (isUser) "You (Teacher)" else "AI Apprentice",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (message.mode == "TEACH") TeachGreen.copy(alpha = 0.15f) else DoBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = message.mode,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (message.mode == "TEACH") TeachGreen else DoBlue,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Message Bubble Card
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )

                    // Lessons / Rules Referenced Chips
                    if (!message.lessonsReferencedJson.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val referencedList = remember(message.lessonsReferencedJson) {
                            try {
                                val arr = JSONArray(message.lessonsReferencedJson)
                                (0 until arr.length()).map { arr.getString(it) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        if (referencedList.isNotEmpty()) {
                            Text(
                                text = "Applied Knowledge & Rules:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = DoBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            referencedList.forEach { ref ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DoBlue.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ref,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = DoBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Draft Card Embedded if Assistant proposed a lesson
                    if (!isUser && !message.lessonDraftJson.isNullOrBlank()) {
                        val draft = remember(message.lessonDraftJson) {
                            LessonDraft.fromJson(message.lessonDraftJson)
                        }
                        if (draft != null) {
                            LessonDraftCard(
                                draft = draft,
                                onConfirmSave = { onConfirmDraft(draft) },
                                onEdit = { onEditDraft(draft) },
                                onDismiss = onDismissDraft
                            )
                        }
                    }
                }
            }

            // Message Actions Row (TTS & Rating for Assistant)
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Voice audio button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) onStopSpeak() else onSpeak(message.content)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "Play/Stop Voice Response",
                                tint = if (isSpeaking) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (isSpeaking) "Stop voice" else "Listen",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Rating / Correction Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onRate(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Rate Good",
                                tint = if (message.feedbackRating == 1) TeachGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRate(-1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "Flag Mistake / Correct",
                                tint = if (message.feedbackRating == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(
    currentMode: AppMode,
    onExampleClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (currentMode == AppMode.TEACH) TeachGreen.copy(alpha = 0.15f) else DoBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (currentMode == AppMode.TEACH) Icons.Default.School else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (currentMode == AppMode.TEACH) TeachGreen else DoBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = if (currentMode == AppMode.TEACH) "Welcome to Teach Mode! 🎓" else "Welcome to Do Mode! ⚡",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (currentMode == AppMode.TEACH)
                        "Talk or type to explain procedures, writing styles, recipes, or rules. I'll summarize what I learned and ask to save it to my permanent notebook!"
                    else
                        "Ask me to perform tasks! I will search my saved notebook of lessons and rules, apply your exact instructions, and cite the rules I followed.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Try tapping an example below or tap the microphone:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val sample = if (currentMode == AppMode.TEACH)
                    "Teach: Always greet clients with 'Hello [Name]', keep under 100 words, and never use slang."
                else
                    "Draft a client update email for the new product launch"

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExampleClick(sample) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (currentMode == AppMode.TEACH) TeachGreen else DoBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sample,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
