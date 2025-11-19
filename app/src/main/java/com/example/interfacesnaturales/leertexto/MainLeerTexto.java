package com.example.interfacesnaturales.leertexto;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.interfacesnaturales.MainActivity;
import com.example.interfacesnaturales.R;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Locale;

public class MainLeerTexto extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private ImageView imageView;
    private TextView textoReconocido;
    private TextRecognizer textRecognizer;
    private TextToSpeech textToSpeech;
    private Button btnTomarFoto;
    private Button btnParar;
    private Button btnReleer;

    // ActivityResultLauncher para manejar el resultado de la cámara
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(imageBitmap);
                    procesarImagenParaTexto(imageBitmap);
                } else {
                    // Si el usuario cancela la cámara, volvemos al menú principal.
                    Intent intent = new Intent(MainLeerTexto.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leer_texto);

        imageView = findViewById(R.id.imageView);
        textoReconocido = findViewById(R.id.textoReconocido);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnParar = findViewById(R.id.btnParar);
        btnReleer = findViewById(R.id.btnReleer);

        btnTomarFoto.setOnClickListener(v -> abrirCamara());

        btnParar.setOnClickListener(v -> {
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
        });
        btnReleer.setOnClickListener(v -> {
            String texto = textoReconocido.getText().toString();
            if (!texto.isEmpty()) {
                leerTextoEnVozAlta(texto);
            }
        });

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        textToSpeech = new TextToSpeech(this, this);

        // Al iniciar, verificar permisos y abrir la cámara directamente
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            abrirCamara();
        }
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void procesarImagenParaTexto(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "No se pudo obtener la imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    extraerTexto(visionText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainLeerTexto.this, "Error al procesar texto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void extraerTexto(Text result) {
        String texto = result.getText();
        if (texto.isEmpty()) {
            textoReconocido.setText("No se encontró texto.");
        } else {
            textoReconocido.setText(texto);
            leerTextoEnVozAlta(texto);
        }
    }

    private void leerTextoEnVozAlta(String texto) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado. La aplicación se cerrará.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "El idioma español no es compatible.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error al inicializar TextToSpeech.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
