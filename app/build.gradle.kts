import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
}

// URL del backend: se puede cambiar sin tocar código agregando en local.properties
//   parkspotter.apiUrl=http://192.168.1.15:8080/api/v1/
// (útil para probar en un celular físico). Por defecto apunta al PC desde el emulador.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiBaseUrl: String = localProps.getProperty("parkspotter.apiUrl") ?: "http://10.0.2.2:8080/api/v1/"

android {
    namespace = "com.example.parkspotter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.parkspotter"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0-sprint4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (fechas y horas de reservas y horarios) en Android 7 (minSdk 24)
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric renderiza las pantallas Compose en la JVM (capturas sin emulador)
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("parkspotter.apiUrl", System.getenv("PARKSPOTTER_API") ?: "")
            System.getenv("PARKSPOTTER_SHOTS")?.let { dir -> it.systemProperty("parkspotter.shots", dir) }
        }
    }
}

dependencies {
    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // MapLibre
    implementation(libs.maplibre.android.sdk)
    implementation(libs.maplibre.android.annotation)

    // Navegación (Sprint 1: Splash -> Login -> Register -> Mapa)
    implementation(libs.androidx.navigation.compose)

    // Previsualización de fotos al publicar un garaje (Sprint 2)
    implementation(libs.coil.compose)

    // Íconos extendidos usados en Login/Register/Garajes (Email, Lock, Home, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModels de Compose (AuthViewModel, GarajeViewModel — Sprint 2)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // LocalLifecycleOwner para pasar el ciclo de vida al MapView de MapLibre (Sprint 3)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Cliente HTTP hacia el backend Spring Boot (ApiClient, AuthApi, GarajeApi, ReservaApi)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Disponibilidad en tiempo real por Server-Sent Events (Sprint 3)
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // Tokens cifrados en el dispositivo (Definition of Done: EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.0.0")

    // Ubicación del dispositivo (LocationHelper — botón "mi ubicación" del mapa)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Tests
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
