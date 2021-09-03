package com.example.object_detection_app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class Audio(ctx: Context, countryCode: String, regionCode: String) : TextToSpeech.OnInitListener {

    private val context: Context = ctx
    private val locale: Locale
    private val tts: TextToSpeech


    init {
        locale = Locale(countryCode, regionCode)
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        when (status) {
            TextToSpeech.SUCCESS -> {
                val result = tts.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.i("TTS", "This Language is not supported")
                } else {
                    Log.i("TTS", "Initilization Success!")
                }
            }
            else -> Log.e("TTS", "Initilization Failed!  $status")
        }
    }

    fun speek(text: String) {
        if (!tts.isSpeaking) {
            tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

}
