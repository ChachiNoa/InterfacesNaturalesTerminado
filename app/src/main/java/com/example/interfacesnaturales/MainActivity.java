package com.example.interfacesnaturales;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.interfacesnaturales.comprarvoz.MainComprarVoz;
import com.example.interfacesnaturales.llamarmalla.MainMallaFacial;
import com.example.interfacesnaturales.lenguajesenas.MainSenas;
import com.example.interfacesnaturales.leertexto.MainLeerTexto;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnComprarVoz = findViewById(R.id.btnComprarVoz);
        Button btnLenguajeSenas = findViewById(R.id.btnLenguajeSenas);
        Button btnLeerTexto = findViewById(R.id.btnLeerTexto);
        Button btnReconocimientoFacial = findViewById(R.id.btnReconocimientoFacial);

        btnComprarVoz.setOnClickListener(this);
        btnLenguajeSenas.setOnClickListener(this);
        btnLeerTexto.setOnClickListener(this);
        btnReconocimientoFacial.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        startListening();
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    boolean commandHandled = false;
                    if (matches != null && !matches.isEmpty()) {
                        String command = matches.get(0).toLowerCase(Locale.getDefault()).trim();
                        commandHandled = handleCommand(command);
                    }

                    if (!commandHandled) {
                        startListening();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Toast.makeText(this, "Speech recognition is not available.", Toast.LENGTH_LONG).show();
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            speechRecognizer.startListening(intent);
        }
    }

    private boolean handleCommand(String command) {
        if (command.contains("comprar")) {
            Intent intent = new Intent(this, MainComprarVoz.class);
            startActivity(intent);
            return true;
        } else if (command.contains("llamar")) {
            Intent intent = new Intent(this, MainMallaFacial.class);
            startActivity(intent);
            return true;
        } else if (command.contains("cerrar")) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        int id = v.getId();
        if (id == R.id.btnComprarVoz) {
            intent = new Intent(this, MainComprarVoz.class);
            startActivity(intent);
        } else if (id == R.id.btnLenguajeSenas) {
            intent = new Intent(this, MainSenas.class);
            startActivity(intent);
        } else if (id == R.id.btnLeerTexto) {
            intent = new Intent(this, MainLeerTexto.class);
            startActivity(intent);
        } else if (id == R.id.btnReconocimientoFacial) {
            intent = new Intent(this, MainMallaFacial.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. onResume will handle the initialization.
            } else {
                Toast.makeText(this, "Audio permission is required for voice commands.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            initializeSpeechRecognizer();
            startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
