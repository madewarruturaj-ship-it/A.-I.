package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a verified lesson taught by the user.
 * Stored persistently in the on-device Room database.
 */
@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val topic: String,
    val summary: String,
    val facts: String = "",
    val instructions: String = "",
    val examples: String = "",
    val constraints: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 0
)
