# Actualizar GitHub sin archivos antiguos

1. Usa la rama `main` del repositorio `Edscorp23/Alexa`.
2. Sube el contenido de esta carpeta `AlexaVoice/` dentro de la carpeta de GitHub `AlexaVoice/` conservando rutas. No subas el ZIP sin extraer.
3. Importante: GitHub no borra archivos al subir otros. Elimina cualquier `MusicVoiceService.java`, `SpotifyMusic.java` y archivos Java o `AndroidManifest.xml` que accidentalmente quedaron en la raíz `AlexaVoice/`. El código válido está dentro de `AlexaVoice/app/src/main/`.
4. Conserva `.github/workflows/build-apk.yml` en la raíz del repositorio.
5. Después del commit, revisa Actions. Si termina en verde, descarga el artifact y comprueba el APK. No evites un bloqueo de Play Protect.
6. Al instalar, habilita las notificaciones de Amazon Alexa y concede acceso a notificaciones a Alexa Voice solo si confías en la aplicación y comprendes que Android concede acceso amplio.
