# Alexa Voice (Android)

Proyecto Android inicial para HONOR X8a. Escucha las notificaciones de la app Amazon Alexa y lee el texto recibido mediante Android Text-to-Speech en español.

## Requisitos
- Android Studio (versión reciente)
- Android SDK 35
- En el teléfono: Amazon Alexa instalada y sus notificaciones habilitadas

## Abrir y compilar
1. Descomprime el proyecto.
2. En Android Studio, selecciona **Open** y elige la carpeta `AlexaVoice`.
3. Espera a que Gradle sincronice y descargue el plugin Android Gradle.
4. Conecta el HONOR X8a por USB con depuración USB habilitada, o genera un APK desde **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Instala el APK.

## Configurar en HONOR X8a
1. Abre Alexa Voice.
2. Pulsa **Activar acceso a notificaciones**.
3. En Acceso a notificaciones, activa Alexa Voice y confirma el aviso del sistema.
4. En Amazon Alexa, activa las notificaciones y los recordatorios en este dispositivo.
5. En Ajustes de HONOR, permite que Alexa Voice se ejecute en segundo plano / desactiva su optimización de batería si MagicOS la detiene.
6. Pulsa **Probar voz femenina**.

## Limitaciones conocidas
- El proyecto solo puede leer notificaciones que Android entregue a NotificationListenerService. Si Alexa no publica el recordatorio como notificación en el teléfono, no hay texto que esta app pueda leer.
- El identificador de paquete usado es `com.amazon.dee.app`, habitual para Amazon Alexa. Puede cambiar según la distribución/región.
- Android no ofrece un indicador estándar y fiable de género de voz. El código busca una voz española cuyo nombre incluya `female`, `woman` o `femen`; si no existe, utiliza la voz española predeterminada instalada en el teléfono. Se puede instalar/cambiar la voz en los ajustes de Texto a voz de Android.
- La app no fue compilada ni probada en un HONOR X8a en este entorno. Es un proyecto fuente inicial; requiere compilarlo en Android Studio y probarlo en el dispositivo.
- No incluye integración con la cuenta de Amazon ni lee datos privados de Alexa.
