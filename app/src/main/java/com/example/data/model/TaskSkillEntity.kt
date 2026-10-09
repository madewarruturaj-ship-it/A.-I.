package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a reusable skill/procedure taught to the AI.
 * Contains step-by-step instructions, input/output contracts, and test cases.
 */
@Entity(tableName = "task_skills")
data class TaskSkillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val purpose: String,
    val category: String = "General",
    val stepMethod: String,
    val requiredInput: String = "",
    val expectedOutput: String = "",
    val examples: String = "",
    val knownMistakes: String = "",
    val corrections: String = "",
    val testCases: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
