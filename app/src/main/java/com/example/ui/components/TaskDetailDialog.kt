package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskSkillEntity
import com.example.ui.theme.DoBlue

@Composable
fun TaskDetailDialog(
    initialTask: TaskSkillEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskSkillEntity) -> Unit,
    onDelete: ((TaskSkillEntity) -> Unit)? = null,
    onRunTaskTest: ((TaskSkillEntity, String, (String) -> Unit) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Details/Edit, 1 = Test Runner

    var name by remember { mutableStateOf(initialTask?.name ?: "") }
    var purpose by remember { mutableStateOf(initialTask?.purpose ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "General") }
    var stepMethod by remember { mutableStateOf(initialTask?.stepMethod ?: "") }
    var requiredInput by remember { mutableStateOf(initialTask?.requiredInput ?: "") }
    var expectedOutput by remember { mutableStateOf(initialTask?.expectedOutput ?: "") }
    var examples by remember { mutableStateOf(initialTask?.examples ?: "") }
    var knownMistakes by remember { mutableStateOf(initialTask?.knownMistakes ?: "") }
    var testCases by remember { mutableStateOf(initialTask?.testCases ?: "") }

    // Test Runner State
    var testInput by remember { mutableStateOf("") }
    var testOutput by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialTask == null || initialTask.id == 0L) "New Task Skill" else initialTask.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (initialTask != null && initialTask.id != 0L) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Procedure Specs") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("⚡ Test Skill") }
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    // Edit Specs Tab
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Skill / Task Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("task_name_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Purpose / Goal *") },
                        placeholder = { Text("What this task achieves") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = stepMethod,
                        onValueChange = { stepMethod = it },
                        label = { Text("Step-by-Step Method *") },
                        placeholder = { Text("Step 1: ...\nStep 2: ...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("task_steps_input")
                    )

                    OutlinedTextField(
                        value = requiredInput,
                        onValueChange = { requiredInput = it },
                        label = { Text("Required Input") },
                        placeholder = { Text("What information is needed to begin?") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expectedOutput,
                        onValueChange = { expectedOutput = it },
                        label = { Text("Expected Output") },
                        placeholder = { Text("Format and shape of final result") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = examples,
                        onValueChange = { examples = it },
                        label = { Text("Examples & Edge Cases") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = knownMistakes,
                        onValueChange = { knownMistakes = it },
                        label = { Text("Known Mistakes & Pitfalls") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = testCases,
                        onValueChange = { testCases = it },
                        label = { Text("Test Cases") },
                        placeholder = { Text("Sample test inputs to verify this skill") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Test Skill Tab
                    Text(
                        text = "🧪 Interactive Task Test Runner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DoBlue
                    )
                    Text(
                        text = "Test this skill with a new input to verify that the AI follows your step-by-step procedure correctly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = testInput,
                        onValueChange = { testInput = it },
                        label = { Text("Test Input") },
                        placeholder = { Text("Enter sample data or instructions...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("task_test_input")
                    )

                    Button(
                        onClick = {
                            if (testInput.isNotBlank() && initialTask != null && onRunTaskTest != null) {
                                isTesting = true
                                testOutput = ""
                                onRunTaskTest(initialTask, testInput) { result ->
                                    testOutput = result
                                    isTesting = false
                                }
                            }
                        },
                        enabled = testInput.isNotBlank() && !isTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = DoBlue),
                        modifier = Modifier.fillMaxWidth().testTag("run_task_test_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Executing Steps...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Skill Test")
                        }
                    }

                    if (testOutput.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Execution Result:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DoBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = testOutput,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        if (name.isNotBlank() && stepMethod.isNotBlank()) {
                            val toSave = (initialTask ?: TaskSkillEntity(
                                name = name,
                                purpose = purpose,
                                stepMethod = stepMethod
                            )).copy(
                                name = name,
                                purpose = purpose,
                                category = category,
                                stepMethod = stepMethod,
                                requiredInput = requiredInput,
                                expectedOutput = expectedOutput,
                                examples = examples,
                                knownMistakes = knownMistakes,
                                testCases = testCases,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(toSave)
                        }
                    },
                    enabled = name.isNotBlank() && stepMethod.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DoBlue),
                    modifier = Modifier.testTag("save_task_button")
                ) {
                    Text("Save Skill")
                }
            }
        },
        dismissButton = {
            Row {
                if (initialTask != null && initialTask.id != 0L && onDelete != null && selectedTab == 0) {
                    OutlinedButton(
                        onClick = { onDelete(initialTask) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
