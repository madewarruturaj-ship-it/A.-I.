package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LessonEntity
import com.example.ui.theme.TeachGreen

@Composable
fun LessonDetailDialog(
    initialLesson: LessonEntity?,
    onDismiss: () -> Unit,
    onSave: (LessonEntity) -> Unit,
    onDelete: ((LessonEntity) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialLesson?.title ?: "") }
    var subject by remember { mutableStateOf(initialLesson?.subject ?: "General") }
    var topic by remember { mutableStateOf(initialLesson?.topic ?: "General") }
    var summary by remember { mutableStateOf(initialLesson?.summary ?: "") }
    var facts by remember { mutableStateOf(initialLesson?.facts ?: "") }
    var instructions by remember { mutableStateOf(initialLesson?.instructions ?: "") }
    var examples by remember { mutableStateOf(initialLesson?.examples ?: "") }
    var constraints by remember { mutableStateOf(initialLesson?.constraints ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialLesson == null || initialLesson.id == 0L) "New Lesson" else "Edit Lesson",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Lesson Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("lesson_title_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Topic") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions & Rules *") },
                    placeholder = { Text("Step 1: ...\nStep 2: ...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("lesson_instructions_input")
                )

                OutlinedTextField(
                    value = facts,
                    onValueChange = { facts = it },
                    label = { Text("Key Facts & Concepts") },
                    placeholder = { Text("Important facts the AI should remember") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = examples,
                    onValueChange = { examples = it },
                    label = { Text("Good & Bad Examples") },
                    placeholder = { Text("Good example:\nBad example:") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = constraints,
                    onValueChange = { constraints = it },
                    label = { Text("Constraints & Things to Avoid") },
                    placeholder = { Text("Never do X. Always avoid Y.") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val toSave = (initialLesson ?: LessonEntity(
                            title = title,
                            subject = subject,
                            topic = topic,
                            summary = summary
                        )).copy(
                            title = title,
                            subject = subject,
                            topic = topic,
                            summary = summary,
                            facts = facts,
                            instructions = instructions,
                            examples = examples,
                            constraints = constraints,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank() && instructions.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TeachGreen),
                modifier = Modifier.testTag("save_lesson_button")
            ) {
                Text("Save Lesson")
            }
        },
        dismissButton = {
            Row {
                if (initialLesson != null && initialLesson.id != 0L && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialLesson) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
