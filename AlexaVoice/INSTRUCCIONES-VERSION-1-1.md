# Alexa Voice 1.1 - repeticion hasta confirmar

El aviso recibido de Amazon Alexa se lee una vez y, mientras siga pendiente, vuelve a leerse cada 2 minutos. Para detenerlo, pulsar **Ya lo hice** en la notificacion persistente o en la aplicacion. Los textos se guardan localmente en el telefono. Una nueva notificacion reemplaza el recordatorio activo.

IMPORTANTE: solo detecta las notificaciones de la aplicacion Amazon Alexa, no las palabras habladas por el Echo. No confirma ni cancela el recordatorio en el Echo. El reconocimiento de mensajes sigue siendo amplio y puede leer otras notificaciones de Alexa. Probar antes de usar para avisos importantes.

La repeticion utiliza el servicio NotificationListenerService y puede interrumpirse si HONOR/Android lo suspende por ahorro de bateria. Autorizar notificaciones de Alexa Voice y permitir ejecucion en segundo plano (Ajustes > Aplicaciones > Inicio de aplicaciones, segun MagicOS). No se garantiza exactitud absoluta con la pantalla apagada.

## Actualizacion en GitHub

El repositorio actual guarda el proyecto en la carpeta `AlexaVoice/`. Extraer el ZIP y subir los siguientes ficheros al MISMO lugar en el repositorio, reemplazandolos:
- `AlexaVoice/app/src/main/java/com/alexavoice/app/AlexaNotificationListener.java`
- `AlexaVoice/app/src/main/java/com/alexavoice/app/ConfirmReminderReceiver.java` (nuevo)
- `AlexaVoice/app/src/main/java/com/alexavoice/app/MainActivity.java`
- `AlexaVoice/app/src/main/AndroidManifest.xml`
- `AlexaVoice/app/build.gradle`

El workflow existente del repositorio debe seguir en `.github/workflows/build-apk.yml`, y debe construir con `gradle -p AlexaVoice --no-daemon assembleDebug`.

## Instalacion

GitHub Actions > ejecucion verde > Artifacts > AlexaVoice-debug-apk. El APK de depuracion puede estar firmado con una clave distinta en cada ejecucion; si Android no permite instalarlo como actualizacion, desinstalar la version anterior y luego instalar el nuevo APK. Desinstalar borra los datos locales y exige volver a permitir acceso a notificaciones.
