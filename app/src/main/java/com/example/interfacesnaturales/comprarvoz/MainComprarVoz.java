package com.example.interfacesnaturales.comprarvoz;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.interfacesnaturales.MainActivity;
import com.example.interfacesnaturales.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainComprarVoz extends AppCompatActivity implements RecognitionListener {

    private static final String LOG_TAG = "StoreListenerApp";
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    public static final String EXTRA_STORE_NAME = "com.example.STORE_NAME";

    // Palabras clave que el usuario puede decir.
    private static final List<String> STORES = Arrays.asList("mercadona", "mc donalds", "taco bell");

    private static final long REPEAT_INTERVAL_MS = 7000;
    private static final String SPANISH_LANGUAGE_TAG = "es-ES";
    private static final Locale SPANISH_LOCALE = new Locale("es", "ES");


    private TextView statusTextView;
    private TextView resultTextView;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comprar_voz);

        statusTextView = findViewById(R.id.textView_status);
        resultTextView = findViewById(R.id.textView_result);

        // Limpiar el carrito al iniciar la aplicación (solo la primera vez)
        Cart.clearCart();

        // 1. Verificar y solicitar permisos
        checkPermissions();

        // 2. Inicializar TextToSpeech
        initializeTextToSpeech();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_CODE);
        } else {
            initializeSpeechRecognizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSpeechRecognizer();
            } else {
                Toast.makeText(this, "Permiso de micrófono es necesario.", Toast.LENGTH_LONG).show();
                statusTextView.setText("Estado: Permiso denegado.");
            }
        }
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "El servicio de reconocimiento de voz no está disponible.", Toast.LENGTH_LONG).show();
            statusTextView.setText("Estado: Servicio no disponible.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        // 3. Iniciar el ciclo de escucha repetida
        handler.post(listeningTask);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(SPANISH_LOCALE);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(LOG_TAG, "Idioma no soportado para TTS.");
                    }
                } else {
                    Log.e(LOG_TAG, "Inicialización de TTS fallida.");
                }
            }
        });
    }

    // Runnable para el ciclo de escucha
    private Runnable listeningTask = new Runnable() {
        @Override
        public void run() {
            startListeningForStore();
            handler.postDelayed(this, REPEAT_INTERVAL_MS);
        }
    };

    private void startListeningForStore() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, SPANISH_LANGUAGE_TAG);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

            statusTextView.setText("Estado: Escuchando tienda...");
            resultTextView.setText("Resultado: Esperando entrada...");
            speechRecognizer.startListening(intent);
        }
    }

    private void handleRecognitionResult(ArrayList<String> matches) {
        if (matches != null && !matches.isEmpty()) {
            String recognizedText = matches.get(0).toLowerCase(SPANISH_LOCALE);
            resultTextView.setText("Resultado: \"" + recognizedText + "\"");
            Log.d(LOG_TAG, "Texto reconocido: " + recognizedText);

            // CHECK FOR "SALIR" COMMAND FIRST
            if (recognizedText.contains("salir")) {
                speakResponse("Volviendo al menú principal.");
                goBackToMainMenu();
                return;
            }

            // Normalizar el texto reconocido: quitar apóstrofos y espacios de más
            String normalizedText = recognizedText.replace("'", "").replace(" ", "");

            for (String store : STORES) {
                // Normalizar la tienda (por ejemplo, "mc donalds" -> "mcdonalds")
                String normalizedStore = store.replace(" ", "");

                // 1. Comprobar coincidencia
                if (recognizedText.contains(store) || normalizedText.contains(normalizedStore)) {
                    // Tienda detectada, navegar a la siguiente pantalla
                    statusTextView.setText("Estado: Tienda '" + store.toUpperCase() + "' detectada. Navegando...");
                    speakResponse("Tienda " + store + " seleccionada.");
                    navigateToProducts(store);
                    return;
                }
            }
            // No es una tienda válida
            statusTextView.setText("Estado: Fin de escucha. Diga una de las tiendas listadas.");

        } else {
            statusTextView.setText("Estado: Fin de escucha. No se detectó voz.");
        }
    }

    private void goBackToMainMenu() {
        // Stop the listening loop and clean up resources before changing Activity
        handler.removeCallbacks(listeningTask);

        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        Intent intent = new Intent(MainComprarVoz.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void navigateToProducts(String storeName) {
        // MUY IMPORTANTE: Detener el ciclo de escucha antes de cambiar de Activity
        handler.removeCallbacks(listeningTask);

        // Detener TTS y destruir SpeechRecognizer de forma segura para evitar conflictos de recursos
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        // Usamos la versión limpia sin espacio (ej: "mcdonalds") para la base de datos de productos.
        String cleanStoreName = storeName.replace(" ", "");

        Intent intent = new Intent(MainComprarVoz.this, ProductActivity.class);
        intent.putExtra(EXTRA_STORE_NAME, cleanStoreName);
        startActivity(intent);
    }

    // --- Métodos de TTS y Listeners ---

    private void speakResponse(String text) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_STORE");
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    // --- Implementación de RecognitionListener ---

    @Override public void onReadyForSpeech(Bundle params) { Log.d(LOG_TAG, "onReadyForSpeech"); statusTextView.setText("Estado: Listo para escuchar..."); }
    @Override public void onBeginningOfSpeech() { Log.d(LOG_TAG, "onBeginningOfSpeech"); statusTextView.setText("Estado: Hablando..."); }
    @Override public void onRmsChanged(float rmsdB) { /* Opcional: mostrar medidor de volumen */ }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { Log.d(LOG_TAG, "onEndOfSpeech"); }

    @Override
    public void onError(int error) {
        String errorMessage = getErrorText(error);
        Log.e(LOG_TAG, "ERROR de Reconocimiento de Voz: " + errorMessage);
        statusTextView.setText("Estado: Error (" + errorMessage + ")");
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(LOG_TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        handleRecognitionResult(matches);
    }

    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) { }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Error de grabación de audio";
            case SpeechRecognizer.ERROR_CLIENT: return "Error del lado del cliente";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Permisos insuficientes";
            case SpeechRecognizer.ERROR_NETWORK: return "Error de red";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Tiempo de espera de red agotado";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No se encontró coincidencia";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Reconocedor ocupado";
            case SpeechRecognizer.ERROR_SERVER: return "Error del servidor";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No se detectó entrada de voz";
            default: return "Error desconocido";
        }
    }

    // --- Métodos del Ciclo de Vida para Limpieza ---

    @Override
    protected void onPause() {
        super.onPause();
        // Detener la tarea periódica y destruir el reconocedor
        handler.removeCallbacks(listeningTask);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reiniciar la escucha
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (speechRecognizer == null) {
                initializeSpeechRecognizer(); // Esto también postea el listeningTask
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(listeningTask);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}