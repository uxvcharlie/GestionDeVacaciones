// Archivo raíz: solo declara qué plugins existen y en qué versión.
// "apply false" = se aplican en el módulo :app, no acá.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
