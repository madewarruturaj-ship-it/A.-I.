package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores explicit corrections given by the user when an answer was wrong.
 * Has higher precedence than older instructions during task execution.
 */
@Entity(tableName = "corrections")
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val relatedLessonId: Long? = null,
    val targetTopic: String = "",
    val originalMistake: String,
    val correctedRule: String,
    val explanation: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
