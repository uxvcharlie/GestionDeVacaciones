// ===========================================================================
//  Proyecto Android de Gestión de Vacaciones.
//  Vive en su propia carpeta (app-android/) y no toca la aplicación web, que
//  sigue intacta en la raíz del repositorio.
// ===========================================================================
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Ningún módulo puede declarar repositorios por su cuenta: todos salen de acá.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GestionDeVacacionesMovil"
include(":app")
