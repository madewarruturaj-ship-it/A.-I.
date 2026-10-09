package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CorrectionEntity
import com.example.data.model.LessonEntity
import com.example.data.model.TaskSkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeachingDao {

    // === LESSONS ===
    @Query("SELECT * FROM lessons ORDER BY updatedAt DESC")
    fun getAllLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLessonById(id: Long): LessonEntity?

    @Query("""
        SELECT * FROM lessons 
        WHERE title LIKE '%' || :query || '%' 
           OR subject LIKE '%' || :query || '%' 
           OR topic LIKE '%' || :query || '%' 
           OR facts LIKE '%' || :query || '%' 
           OR instructions LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchLessons(query: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons")
    suspend fun getAllLessonsSnapshot(): List<LessonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET useCount = useCount + 1, updatedAt = :now WHERE id = :id")
    suspend fun incrementLessonUse(id: Long, now: Long = System.currentTimeMillis())

    // === CORRECTIONS ===
    @Query("SELECT * FROM corrections ORDER BY createdAt DESC")
    fun getAllCorrections(): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveCorrections(): Flow<List<CorrectionEntity>>

    @Query("SELECT * FROM corrections WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveCorrectionsSnapshot(): List<CorrectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: CorrectionEntity): Long

    @Update
    suspend fun updateCorrection(correction: CorrectionEntity)

    @Delete
    suspend fun deleteCorrection(correction: CorrectionEntity)

    @Query("UPDATE corrections SET isActive = :isActive WHERE id = :id")
    suspend fun setCorrectionActive(id: Long, isActive: Boolean)

    // === TASKS ===
    @Query("SELECT * FROM task_skills ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskSkillEntity>>

    @Query("SELECT * FROM task_skills WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskSkillEntity?

    @Query("SELECT * FROM task_skills")
    suspend fun getAllTasksSnapshot(): List<TaskSkillEntity>

    @Query("""
        SELECT * FROM task_skills 
        WHERE name LIKE '%' || :query || '%' 
           OR purpose LIKE '%' || :query || '%' 
           OR stepMethod LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskSkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskSkillEntity): Long

    @Update
    suspend fun updateTask(task: TaskSkillEntity)

    @Delete
    suspend fun deleteTask(task: TaskSkillEntity)

    // === CHAT MESSAGES ===
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET feedbackRating = :rating WHERE id = :id")
    suspend fun updateMessageFeedback(id: Long, rating: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}
