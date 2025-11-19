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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.interfacesnaturales.MainActivity;
import com.example.interfacesnaturales.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductActivity extends AppCompatActivity implements RecognitionListener {
    private static final String LOG_TAG = "ProductListenerApp";
    private static final long REPEAT_INTERVAL_MS = 7000;
    private static final String SPANISH_LANGUAGE_TAG = "es-ES";
    private static final Locale SPANISH_LOCALE = new Locale("es", "ES");
    private static final String COMMAND_CHECKOUT = "pagar";
    private static final String ATRAS_COMMAND = "atrás"; // Comando para volver a la pantalla anterior

    private TextView storeTitleTextView;
    private TextView productsTextView;
    private TextView statusTextView;
    private TextView resultTextView;
    private TextView cartTextView;
    private TextView totalTextView;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Handler handler = new Handler();

    private String currentStore;
    private Map<String, Double> availableProducts = new HashMap<>();

    // --- BASE DE DATOS DE PRODUCTOS ---
    // Usamos el Map<String, Double> como Mapa<Nombre del Producto, Precio>
    private static final Map<String, Map<String, Double>> PRODUCT_DB;
    static {
        Map<String, Map<String, Double>> db = new HashMap<>();

        // Mercadona: Las claves son el texto que el usuario DEBE decir.
        Map<String, Double> mercadona = new HashMap<>();
        mercadona.put("leche entera", 1.05);
        mercadona.put("pan de molde", 1.50);
        mercadona.put("huevos grandes", 2.20);
        mercadona.put("yogur natural", 0.70);
        db.put("mercadona", mercadona);

        // McDonald's: Las claves son el texto que el usuario DEBE decir (sin espacios, limpio)
        Map<String, Double> mcdonalds = new HashMap<>();
        mcdonalds.put("big mac", 5.50);
        mcdonalds.put("patatas grandes", 3.00);
        mcdonalds.put("coca cola", 2.50); // Se limpiará de guiones en el handleRecognitionResult
        mcdonalds.put("mcflurry", 3.25);
        db.put("mcdonalds", mcdonalds);

        // Taco Bell
        Map<String, Double> tacobell = new HashMap<>();
        tacobell.put("taco supremo", 4.00);
        tacobell.put("burrito", 6.50);
        tacobell.put("nachos", 3.50);
        tacobell.put("quesadilla", 5.00);
        db.put("tacobell", tacobell);

        PRODUCT_DB = Collections.unmodifiableMap(db);
    }
    // ------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        // 1. Obtener la tienda seleccionada de la MainActivity
        Intent intent = getIntent();
        currentStore = intent.getStringExtra(MainComprarVoz.EXTRA_STORE_NAME);

        // 2. Inicializar Vistas
        storeTitleTextView = findViewById(R.id.textView_store_title);
        productsTextView = findViewById(R.id.textView_products);
        statusTextView = findViewById(R.id.textView_status);
        resultTextView = findViewById(R.id.textView_result);
        cartTextView = findViewById(R.id.textView_cart);
        totalTextView = findViewById(R.id.textView_total);

        // 3. Cargar la UI con los productos de la tienda seleccionada
        loadProductsAndUI();

        // 4. Inicializar TTS y SpeechRecognizer (El permiso ya lo dio en MainActivity)
        initializeTextToSpeech();
        initializeSpeechRecognizer();

        // 5. Iniciar el ciclo de escucha
        handler.post(listeningTask);
    }

    private void loadProductsAndUI() {
        // Carga los productos correctos según el nombre de la tienda (ej: "mcdonalds")
        availableProducts = PRODUCT_DB.getOrDefault(currentStore, new HashMap<>());

        // Construir la lista de productos para mostrar en pantalla
        StringBuilder sb = new StringBuilder();
        sb.append("Menú de: ").append(capitalize(currentStore)).append("\n\n");
        for (Map.Entry<String, Double> entry : availableProducts.entrySet()) {
            sb.append("• ").append(capitalize(entry.getKey())).append(" (€")
                    .append(String.format("%.2f", entry.getValue())).append(")\n");
        }
        productsTextView.setText(sb.toString());

        // Asegurar que el título de la pantalla sea correcto
        storeTitleTextView.setText(capitalize(currentStore));

        updateCartDisplay();
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "El servicio de reconocimiento de voz no está disponible.", Toast.LENGTH_LONG).show();
            statusTextView.setText("Estado: Servicio no disponible.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(SPANISH_LOCALE);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(LOG_TAG, "Idioma no soportado para TTS.");
                }
            } else {
                Log.e(LOG_TAG, "Inicialización de TTS fallida.");
            }
        });
    }

    // Runnable para el ciclo de escucha
    private Runnable listeningTask = new Runnable() {
        @Override
        public void run() {
            startListeningForProduct();
            // Esto programa el siguiente inicio de la escucha después del intervalo
            handler.postDelayed(this, REPEAT_INTERVAL_MS);
        }
    };

    private void startListeningForProduct() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, SPANISH_LANGUAGE_TAG);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

            statusTextView.setText("Estado: Escuchando productos o comandos...");
            resultTextView.setText("Resultado: Esperando entrada...");
            speechRecognizer.startListening(intent);
        }
    }

    private void handleRecognitionResult(ArrayList<String> matches) {
        if (matches != null && !matches.isEmpty()) {
            String recognizedText = matches.get(0).toLowerCase(SPANISH_LOCALE);
            resultTextView.setText("Resultado: \"" + recognizedText + "\"");
            Log.d(LOG_TAG, "Texto reconocido: " + recognizedText);

            // 1. Normalizar el texto reconocido (quitar guiones, apóstrofos, espacios)
            String cleanRecognizedText = recognizedText.replace("-", "").replace("'", "").replace(" ", "");

            // PRIORITY 1: Check for "SALIR" command
            if (recognizedText.contains("salir")) {
                speakResponse("Volviendo al menú principal.");
                goBackToMainMenu();
                return;
            }

            // 2. Verificar comando de ATRÁS
            if (cleanRecognizedText.contains(ATRAS_COMMAND.replace(" ", ""))) {
                statusTextView.setText("Estado: Comando 'ATRAS' detectado. Volviendo a tiendas...");
                speakResponse("Volviendo a la pantalla principal.");
                // Cierra la Activity actual y vuelve a la anterior (MainActivity)
                finish();
                return;
            }

            // 3. Verificar comando de PAGO
            if (cleanRecognizedText.contains(COMMAND_CHECKOUT.replace(" ", ""))) {
                statusTextView.setText("Estado: Comando 'PAGAR' detectado. Finalizando compra...");
                performCheckout();
                return;
            }

            // 4. Verificar PRODUCTO
            for (String product : availableProducts.keySet()) {
                // Normalizar la clave del producto (ej: "coca cola" -> "cocacola")
                String cleanProduct = product.replace(" ", "");

                // Comparamos el texto reconocido limpio con la clave del producto limpia
                if (cleanRecognizedText.contains(cleanProduct)) {
                    double price = availableProducts.get(product);
                    Cart.addItem(product, price);
                    updateCartDisplay();

                    statusTextView.setText("Estado: Producto '" + capitalize(product) + "' añadido.");
                    speakResponse(capitalize(product) + " añadido a la cesta.");
                    return;
                }
            }

            // No es un comando válido ni un producto
            statusTextView.setText("Estado: Fin de escucha. Diga un producto, 'PAGAR' o 'ATRAS'.");

        } else {
            statusTextView.setText("Estado: Fin de escucha. No se detectó voz.");
        }
    }

    private void goBackToMainMenu() {
        handler.removeCallbacks(listeningTask);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }


    private void updateCartDisplay() {
        List<Cart.CartItem> items = Cart.getItems();
        if (items.isEmpty()) {
            cartTextView.setText("Tu carrito está vacío.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Cart.CartItem item : items) {
                sb.append(item.toString()).append("\n");
            }
            cartTextView.setText(sb.toString());
        }

        totalTextView.setText("Total: €" + String.format("%.2f", Cart.getTotal()));
    }

    private void performCheckout() {
        double total = Cart.getTotal();
        if (total > 0) {
            String message = "Compra de " + capitalize(currentStore) + " finalizada por un total de €" + String.format("%.2f", total) + ". ¡Gracias!";
            speakResponse(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            speakResponse("La cesta está vacía. No se ha realizado ninguna compra.");
            Toast.makeText(this, "Cesta vacía. Compra cancelada.", Toast.LENGTH_SHORT).show();
        }

        // LIMPIEZA CRÍTICA: Vaciar el carrito después de la compra
        Cart.clearCart();

        // Volver a la MainActivity
        finish();
    }

    // Función de utilidad para poner en mayúscula la primera letra
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        // La lógica para "mc donalds" (mcdonalds)
        if (text.contains("mc")) {
            return "Mc" + text.substring(2);
        }
        return text.substring(0, 1).toUpperCase(SPANISH_LOCALE) + text.substring(1);
    }

    // --- Métodos de TTS y Listeners ---

    private void speakResponse(String text) {
        if (textToSpeech != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_PRODUCT");
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

        // ¡FIX CRÍTICO!
        // Al eliminar la lógica de re-posteo inmediato, garantizamos que el ciclo
        // de escucha se mantenga estable a intervalos de 7 segundos, eliminando los saltos.
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
            case SpeechRecognizer.ERROR_AUDIO: return "Error de audio";
            case SpeechRecognizer.ERROR_CLIENT: return "Error del cliente";
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