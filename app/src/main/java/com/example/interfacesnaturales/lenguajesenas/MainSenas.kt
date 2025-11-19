package com.example.interfacesnaturales.lenguajesenas

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.interfacesnaturales.databinding.ActivityMainSenasBinding
import java.util.Locale

class MainSenas : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var activityMainBinding: ActivityMainSenasBinding
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainSenasBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        tts = TextToSpeech(this, this)
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    public override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}
