# Actualizacion 1.5: sin Spotify

1. En GitHub, seleccionar la rama `main` (no un commit antiguo).
2. Abrir `AlexaVoice/` y subir los archivos extraidos de este paquete conservando las rutas interiores, sustituyendo los antiguos. Alternativamente usar Git para copiar toda la carpeta sobre la existente.
3. En `AlexaVoice/app/src/main/java/com/alexavoice/app/`, borrar `MusicVoiceService.java` y `SpotifyMusic.java` si quedaron de la 1.4. Este ZIP no los contiene, pero subir el ZIP no elimina archivos antiguos del repositorio.
4. Conservar `.github/workflows/build-apk.yml` que ya funciona en la RAIZ del repositorio (no dentro de AlexaVoice/).
5. Confirmar los cambios y revisar el resultado en Actions; descargar `app-debug.apk` del artifact solo si la compilacion termina con marca verde.
6. No eludir ningun bloqueo de Google Play Protect. Si vuelve a bloquear, hay que revisar la deteccion/solicitar una evaluacion, no forzar la instalacion.

Permisos esperados: RECORD_AUDIO, POST_NOTIFICATIONS, y acceso especial a NotificationListener (concesion manual). No solicita INTERNET, FOREGROUND_SERVICE o FOREGROUND_SERVICE_MICROPHONE.
