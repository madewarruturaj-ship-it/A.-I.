package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.LessonDraft
import com.example.ai.TeachingEngine
import com.example.data.model.CorrectionEntity
import com.example.data.model.LessonEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Teaching AI", appName)
  }

  @Test
  fun `teaching engine processes teach input into structured draft`() = runBlocking {
    val engine = TeachingEngine()
    val lessonText = "Always greet clients with Hello [Name]. Keep emails under 100 words. Never use informal slang."
    val result = engine.processTeachInput(lessonText, emptyList())

    assertNotNull(result.lessonDraft)
    val draft = result.lessonDraft!!
    assertTrue(draft.instructions.isNotEmpty() || draft.facts.isNotEmpty())
    assertTrue(draft.constraints.contains("informal slang") || draft.constraints.isNotEmpty())
  }

  @Test
  fun `teaching engine cites matching lessons in do mode`() = runBlocking {
    val engine = TeachingEngine()
    val lesson = LessonEntity(
      id = 1L,
      title = "Client Email Rules",
      subject = "Communication",
      topic = "Email Writing",
      summary = "Professional client emails",
      instructions = "1. Greet politely. 2. Keep under 100 words."
    )
    val correction = CorrectionEntity(
      id = 1L,
      targetTopic = "Email Writing",
      originalMistake = "Used casual goodbye",
      correctedRule = "Always end with Best regards",
      isActive = true
    )

    val doResult = engine.processDoInput(
      userMessage = "Draft an email to client about project delivery",
      lessons = listOf(lesson),
      corrections = listOf(correction),
      tasks = emptyList()
    )

    assertTrue(doResult.referencedLessons.contains("Client Email Rules"))
    assertTrue(doResult.referencedCorrections.any { it.contains("Best regards") })
  }
}
