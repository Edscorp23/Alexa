# Compilar Alexa Voice online (sin instalar Android Studio)

Este proyecto incluye un flujo de GitHub Actions para generar un APK de prueba en la nube.

1. Crea/inicia sesión en una cuenta de GitHub.
2. Crea un repositorio nuevo (puede ser privado).
3. Sube el contenido de esta carpeta `AlexaVoice` al repositorio, conservando la carpeta `.github/workflows`.
4. En GitHub abre **Actions** y selecciona **Build Alexa Voice APK**.
5. Pulsa **Run workflow**. También se ejecuta al subir cambios a la rama `main`.
6. Cuando termine correctamente, abre la ejecución y descarga el artefacto **AlexaVoice-debug-apk**.
7. Descomprime el artefacto y encontrarás `app-debug.apk`; pásalo al HONOR X8a e instálalo.

El APK `debug` queda firmado automáticamente para pruebas. No es una versión de publicación en Google Play. No compartas contraseñas ni claves privadas. El repositorio puede mantenerse privado.

Nota: la compilación en la nube solo genera el APK; todavía hay que instalarlo y probar en el teléfono que Alexa realmente publique los recordatorios como notificaciones.


**Versión 1.4**: véase INSTRUCCIONES-VERSION-1-4.md.
