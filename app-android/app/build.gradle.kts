import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// ---------------------------------------------------------------------------
// Firma de la versión que se instala en el celular.
//
// Las claves NUNCA están en el repositorio. Se leen de "firma.properties",
// que está en .gitignore y apunta a un almacén de claves guardado FUERA del
// proyecto. Si el archivo no existe, la compilación de "release" sale sin
// firmar y Android se niega a instalarla: es a propósito.
// ---------------------------------------------------------------------------
val archivoFirma = rootProject.file("firma.properties")
val firma = Properties().apply {
    if (archivoFirma.exists()) archivoFirma.inputStream().use { load(it) }
}

android {
    namespace = "ni.gestionvacaciones.movil"
    // Se compila contra Android 17.2 porque las bibliotecas de AndroidX más
    // nuevas lo exigen. Esto NO limita en qué celulares se instala (eso lo
    // decide minSdk) ni cambia cómo se comporta la app (eso lo decide targetSdk).
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        // Identificador permanente de la app. Si cambia, Android la trata
        // como otra app distinta y no se puede actualizar la instalada.
        applicationId = "ni.gestionvacaciones.movil"
        // Android 8.0 o superior: cubre los celulares de 2017 en adelante.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (archivoFirma.exists()) {
            create("publicacion") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 achica la app y quita el código que no se usa.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (archivoFirma.exists()) {
                signingConfig = signingConfigs.getByName("publicacion")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric necesita los recursos de Android para las pruebas
            // que corren la base de datos real en la computadora.
            isIncludeAndroidResources = true
        }
    }
}

// Room guarda acá una "foto" del esquema de cada versión de la base. Se
// versiona en git: es el equivalente a las migraciones de Flyway de la web.
room {
    schemaDirectory("$projectDir/esquemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
