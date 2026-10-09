package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists conversation dialogue between the user and the AI student.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant"
    val mode: String, // "TEACH", "DO", "CHAT"
    val content: String,
    val lessonDraftJson: String? = null,
    val lessonsReferencedJson: String? = null,
    val feedbackRating: Int = 0, // 0 = none, 1 = up, -1 = down
    val timestamp: Long = System.currentTimeMillis()
)
