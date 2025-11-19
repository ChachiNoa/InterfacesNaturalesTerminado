plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.interfacesnaturales"
    //compileSdk = 36
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.interfacesnaturales"
        /*minSdk = 24
        targetSdk = 36*/
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    // DESPUÉS (Correcto)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// REEMPLAZA TUS DEPENDENCIAS CON ESTE BLOQUE CUIDADOSAMENTE CONSTRUIDO
dependencies {

    implementation("androidx.core:core-ktx:1.9.0") //1.8.0
    implementation("androidx.appcompat:appcompat:1.6.1") //1.5.1
    implementation("com.google.android.material:material:1.10.0") // 1.7.0
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2") //1.5.4

    // --- Navigation ---
    val navVersion = "2.7.7" //2.5.3
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // --- CameraX --- (Mantenemos la versión que estabas usando y que funciona)
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // --- FUNCIONALIDAD 1: MediaPipe (Gestos) ---
    implementation("com.google.mediapipe:tasks-vision:0.10.29")

    // --- FUNCIONALIDAD 2: ML Kit (Reconocimiento de Texto) ---
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // --- FUNCIONALIDAD 3: ML Kit (Detección de Caras y Malla Facial) ---
    // Estas son más antiguas y sensibles. Forzamos una versión compatible.
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.mlkit:face-mesh-detection:16.0.0-beta1")

    // --- WindowManager --- (Mantenemos la que tenías)
    implementation("androidx.window:window:1.1.0-alpha03")

    // --- Dependencias de Testing (Consolidadas) ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") //1.1.3
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") //3.4.0

}