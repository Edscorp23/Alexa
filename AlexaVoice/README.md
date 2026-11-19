# Alexa Voice 1.6 — solo recordatorios, sin micrófono

Aplicación Android para HONOR X8a. Interfaz con la imagen anime aportada por el usuario.

## Funciones
- Lee en español los recordatorios recibidos por las notificaciones de Amazon Alexa.
- Los repite cada 2 minutos mientras el servicio de acceso a notificaciones sigue funcionando.
- Permite confirmar desde el botón «Ya lo hice» en la aplicación o en la notificación.
- Sin Spotify, reconocimiento de voz ni acceso al micrófono.

## Privacidad y limitaciones
- Declara únicamente `POST_NOTIFICATIONS` como permiso runtime y `BIND_NOTIFICATION_LISTENER_SERVICE` como protección del servicio de escucha.
- Android requiere conceder manualmente acceso especial a notificaciones. Ese acceso puede abarcar notificaciones de otras aplicaciones, aunque este código solo procesa las del paquete `com.amazon.dee.app`.
- No envía recordatorios a servidores externos y no declara `INTERNET`.
- Guarda localmente el último recordatorio pendiente. No admite múltiples pendientes simultáneos.
- El servicio puede interrumpirse por las restricciones de batería de MagicOS; no se garantizan repeticiones exactas con la pantalla apagada.
- No se ha compilado ni probado en el dispositivo. Play Protect puede seguir bloqueando el APK por el acceso especial a notificaciones.

## Compilación
Conservar el archivo GitHub Actions `.github/workflows/build-apk.yml` de la raíz del repositorio. Ejecutar `gradle -p AlexaVoice --no-daemon assembleDebug` con Android SDK 35 y JDK 17.
