package com.example.interfacesnaturales.llamarmalla;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.interfacesnaturales.MainActivity;
import com.example.interfacesnaturales.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainMallaFacial extends AppCompatActivity {

    private static final String TAG = "FaceInteractionApp";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int RECORD_AUDIO_PERMISSION_CODE = 101;

    private PreviewView viewFinder;
    private GridLayout numberGridLayout;
    private TextView statusText;
    private TextView dialedNumbersDisplay;
    private View nosePointer; // The red dot

    private FaceMeshDetector meshDetector;
    private FaceDetector classificationDetector;
    private ExecutorService cameraExecutor;

    private int selectedCellId = -1;
    private final StringBuilder dialedNumbers = new StringBuilder();
    private final float SELECTION_CLOSE_THRESHOLD = 0.3f;
    private boolean isBlinkSelecting = false;

    private final float BASE_SENSITIVITY_H = 5f;
    private final float ACCELERATION_FACTOR_H = 400.0f;
    private final float BASE_SENSITIVITY_V = 10.0f;
    private final float ACCELERATION_FACTOR_V = 800.0f;
    private final float NORMALIZED_HORIZONTAL_OFFSET = -0.13f;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.malla_facial);

        viewFinder = findViewById(R.id.viewFinder);
        numberGridLayout = findViewById(R.id.number_grid_layout);
        statusText = findViewById(R.id.statusText);
        dialedNumbersDisplay = findViewById(R.id.dialed_numbers_display);
        nosePointer = findViewById(R.id.nosePointer);

        viewFinder.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        }

        setupSpeechRecognizer();
        checkAndRequestAudioPermission();

        FaceMeshDetectorOptions meshOptions = new FaceMeshDetectorOptions.Builder()
                .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                .build();
        meshDetector = FaceMeshDetection.getClient(meshOptions);

        FaceDetectorOptions classificationOptions = new FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        classificationDetector = FaceDetection.getClient(classificationOptions);
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        if (match.toLowerCase().contains("salir")) {
                            goBackToMainMenu();
                            return; // Exit after command is found
                        }
                    }
                }
                // Restart listening if "salir" not found
                startListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        if (match.toLowerCase().contains("salir")) {
                            goBackToMainMenu();
                            return; // Exit after command is found
                        }
                    }
                }
            }

            @Override
            public void onEndOfSpeech() {
                // The user has stopped speaking, restart listening for the next command
                 startListening();
            }

            @Override
            public void onError(int error) {
                // Errors can be frequent, especially with background noise.
                // We restart listening unless it's a critical error.
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    // No speech was recognized, just restart
                    startListening();
                } else if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    Log.e(TAG, "Speech Recognizer Error: " + error);
                }
            }
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_CODE);
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private void goBackToMainMenu() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied. Cannot run face detection.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Audio permission denied. Voice commands disabled.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new FaceFrameAnalyzer(this));

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private class FaceFrameAnalyzer implements ImageAnalysis.Analyzer {
        private final Context context;
        private final int[] viewFinderScreenLocation = new int[2];
        private final int[] cellScreenLocation = new int[2];
        private final Rect cellRect = new Rect();

        public FaceFrameAnalyzer(Context context) {
            this.context = context;
        }

        private float[] mapToViewCoordinates(float x, float y, int imageWidth, int imageHeight) {
            float viewWidth = viewFinder.getWidth();
            float viewHeight = viewFinder.getHeight();
            if (viewWidth == 0 || viewHeight == 0) return new float[]{0, 0};
            float scaleFactor = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
            float offsetX = (viewWidth - imageWidth * scaleFactor) / 2.0f;
            float offsetY = (viewHeight - imageHeight * scaleFactor) / 2.0f;
            float mirroredX = imageWidth - x;
            float mappedX = mirroredX * scaleFactor + offsetX;
            float mappedY = y * scaleFactor + offsetY;
            return new float[]{mappedX, mappedY};
        }

        @androidx.camera.core.ExperimentalGetImage
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (imageProxy.getImage() == null) {
                imageProxy.close();
                return;
            }
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            meshDetector.process(image)
                    .addOnSuccessListener(faceMeshes -> {
                        if (!faceMeshes.isEmpty()) {
                            handleNoseTracking(faceMeshes.get(0), image);
                        } else {
                            runOnUiThread(() -> {
                                statusText.setText(getString(R.string.status_no_face));
                                highlightCell(-1);
                                nosePointer.setVisibility(View.GONE);
                            });
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Face Mesh detection failed: ", e))
                    .addOnCompleteListener(task -> classificationDetector.process(image)
                            .addOnSuccessListener(faces -> {
                                if (!faces.isEmpty()) {
                                    handleEyebrowSelection(faces.get(0));
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Face Classification failed: ", e))
                            .addOnCompleteListener(task2 -> imageProxy.close()));
        }

        private void handleNoseTracking(FaceMesh faceMesh, InputImage image) {
            FaceMeshPoint noseBridgePoint = faceMesh.getAllPoints().get(168);
            PointF3D nosePoint3D = noseBridgePoint.getPosition();

            float[] layoutCoords = mapToViewCoordinates(nosePoint3D.getX(), nosePoint3D.getY(), image.getWidth(), image.getHeight());
            float layoutX = layoutCoords[0];
            float layoutY = layoutCoords[1];

            float viewWidth = viewFinder.getWidth();
            float viewHeight = viewFinder.getHeight();
            if (viewWidth == 0 || viewHeight == 0) return;

            float normalizedX = layoutX / viewWidth;
            float normalizedY = layoutY / viewHeight;

            normalizedX += NORMALIZED_HORIZONTAL_OFFSET;

            float centerX = 0.5f;
            float centerY = 0.5f;

            float distFromCenterX = normalizedX - centerX;
            float distFromCenterY = normalizedY - centerY;

            // --- AGGRESSIVE CUBIC ACCELERATION CURVE ---
            float gainH = BASE_SENSITIVITY_H + (ACCELERATION_FACTOR_H * distFromCenterX * distFromCenterX);
            float gainV = BASE_SENSITIVITY_V + (ACCELERATION_FACTOR_V * distFromCenterY * distFromCenterY);

            float finalDistX = distFromCenterX * gainH;
            float finalDistY = distFromCenterY * gainV;

            normalizedX = centerX + finalDistX;
            normalizedY = centerY + finalDistY;

            // Clamp values to prevent the pointer from going off-screen
            normalizedX = Math.max(0.0f, Math.min(1.0f, normalizedX));
            normalizedY = Math.max(0.0f, Math.min(1.0f, normalizedY));

            final float calibratedLayoutX = normalizedX * viewWidth;
            final float calibratedLayoutY = normalizedY * viewHeight;

            runOnUiThread(() -> {
                nosePointer.setX(calibratedLayoutX - (nosePointer.getWidth() / 2f));
                nosePointer.setY(calibratedLayoutY - (nosePointer.getHeight() / 2f));
                nosePointer.setVisibility(View.VISIBLE);

                viewFinder.getLocationOnScreen(viewFinderScreenLocation);
                final float pointerScreenX = viewFinderScreenLocation[0] + calibratedLayoutX;
                final float pointerScreenY = viewFinderScreenLocation[1] + calibratedLayoutY;

                int newSelectedCellId = -1;
                for (int i = 0; i < numberGridLayout.getChildCount(); i++) {
                    View cell = numberGridLayout.getChildAt(i);
                    if (cell != null && cell.isShown()) {
                        cell.getLocationOnScreen(cellScreenLocation);
                        cellRect.set(
                                cellScreenLocation[0],
                                cellScreenLocation[1],
                                cellScreenLocation[0] + cell.getWidth(),
                                cellScreenLocation[1] + cell.getHeight()
                        );

                        if (cellRect.contains((int) pointerScreenX, (int) pointerScreenY)) {
                            newSelectedCellId = i + 1;
                            break;
                        }
                    }
                }

                if (newSelectedCellId != selectedCellId) {
                    selectedCellId = newSelectedCellId;
                    highlightCell(selectedCellId);
                }
            });
        }

        private void handleEyebrowSelection(Face face) {
            Float leftEyeOpenProb = face.getLeftEyeOpenProbability();
            Float rightEyeOpenProb = face.getRightEyeOpenProbability();

            boolean eyesClosed = (leftEyeOpenProb != null && leftEyeOpenProb < SELECTION_CLOSE_THRESHOLD) ||
                    (rightEyeOpenProb != null && rightEyeOpenProb < SELECTION_CLOSE_THRESHOLD);

            if (eyesClosed) {
                if (!isBlinkSelecting && selectedCellId != -1) {
                    isBlinkSelecting = true;
                    runOnUiThread(() -> {
                        View currentCellView = numberGridLayout.getChildAt(selectedCellId - 1);
                        if (!(currentCellView instanceof TextView)) return;
                        TextView currentCell = (TextView) currentCellView;
                        String selectedText = currentCell.getText().toString();

                        if (selectedCellId == 10) { // DELETE
                            if (dialedNumbers.length() > 0) {
                                dialedNumbers.deleteCharAt(dialedNumbers.length() - 1);
                                dialedNumbersDisplay.setText(dialedNumbers.toString());
                                statusText.setText("Deleted");
                            }
                        } else if (selectedCellId == 12) { // CALL
                            if (dialedNumbers.length() > 0) {
                                String numberToCall = dialedNumbers.toString();
                                Toast.makeText(context, "Calling: " + numberToCall, Toast.LENGTH_LONG).show();
                                statusText.setText("Calling...");
                                dialedNumbers.setLength(0);
                                dialedNumbersDisplay.setText("");
                            }
                        } else { // NUMBER
                            dialedNumbers.append(selectedText);
                            dialedNumbersDisplay.setText(dialedNumbers.toString());
                            statusText.setText(getString(R.string.status_selected, selectedText));
                        }
                        currentCell.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light));
                    });
                }
            } else {
                isBlinkSelecting = false;
                runOnUiThread(() -> {
                    if (selectedCellId != -1) {
                        View cell = numberGridLayout.getChildAt(selectedCellId - 1);
                        if (cell instanceof TextView) {
                            statusText.setText(getString(R.string.status_aiming, ((TextView) cell).getText().toString()));
                        }
                    }
                    highlightCell(selectedCellId); // Keep highlighting the current cell
                });
            }
        }

        private void highlightCell(int id) {
            for (int i = 0; i < numberGridLayout.getChildCount(); i++) {
                View cell = numberGridLayout.getChildAt(i);
                if (cell != null) {
                    cell.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }

            if (id >= 1 && id <= numberGridLayout.getChildCount()) {
                View cellToHighlight = numberGridLayout.getChildAt(id - 1);
                if (cellToHighlight != null) {
                    cellToHighlight.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_light));
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (meshDetector != null) {
            meshDetector.close();
        }
        if (classificationDetector != null) {
            classificationDetector.close();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}