package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed class SpeechState {
    object Idle : SpeechState()
    data class Listening(val rmsDb: Float = 0f) : SpeechState()
    object Processing : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class SpeechManager(private val context: Context) : RecognitionListener {

    private val TAG = "SpeechManager"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onRecognitionCompleteCallback: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.language = Locale.US
                    }
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                    isTtsReady = true
                    Log.d(TAG, "TextToSpeech initialized successfully")
                }
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status $status")
            }
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        // Stop any active TTS before listening
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value = SpeechState.Error("Speech recognition is not available on this device. You can type your instructions directly.")
            return
        }

        onRecognitionCompleteCallback = onResult

        try {
            destroyRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechManager)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.Listening(0f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            _speechState.value = SpeechState.Error(e.localizedMessage ?: "Could not start microphone")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _speechState.value = SpeechState.Processing
        } catch (e: Exception) {
            _speechState.value = SpeechState.Idle
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // ignore
        } finally {
            _speechState.value = SpeechState.Idle
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        // Clean any markdown formatting for cleaner audio speech
        val cleaned = text
            .replace(Regex("[#*`_\\[\\]()]"), "")
            .replace(Regex("⚠️|🎓|📚|⚡|✅|❌"), "")
            .trim()

        if (cleaned.isBlank()) return

        stopSpeaking()
        val utteranceId = UUID.randomUUID().toString()
        _isSpeaking.value = true
        textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        if (textToSpeech != null && _isSpeaking.value) {
            textToSpeech?.stop()
            _isSpeaking.value = false
        }
    }

    // RecognitionListener callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _speechState.value = SpeechState.Listening(0f)
    }

    override fun onBeginningOfSpeech() {
        _speechState.value = SpeechState.Listening(2f)
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (_speechState.value is SpeechState.Listening) {
            _speechState.value = SpeechState.Listening(rmsdB.coerceAtLeast(0f))
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _speechState.value = SpeechState.Processing
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client speech error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network issue for voice recognition"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice network timed out"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error code: $error"
        }
        _speechState.value = SpeechState.Error(message)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        _speechState.value = SpeechState.Idle
        if (text.isNotBlank()) {
            onRecognitionCompleteCallback?.invoke(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Can be used for live transcription if desired
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // ignore
        }
    }

    fun destroy() {
        destroyRecognizer()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
