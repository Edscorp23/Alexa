# Alexa Voice 1.5 — sin Spotify

Aplicacion Android para HONOR X8a. Basada en la version 1.3 con el fondo anime original y el icono personalizado.

## Funciones
- Leer notificaciones de recordatorios de Amazon Alexa con voz sintetizada en espanol.
- Repetir el recordatorio aproximadamente cada 2 minutos hasta su confirmacion mientras Android mantenga el servicio operativo.
- Confirmar desde el boton de la aplicacion o de la notificacion.
- Responder por voz cuando la aplicacion esta abierta y se concede el permiso de microfono.
- Sin Spotify, sin escucha musical permanente, sin servicio foreground de microfono.

## Permisos
- POST_NOTIFICATIONS: avisos y confirmacion desde la notificacion en Android 13+.
- RECORD_AUDIO: reconocimiento de «Ya lo hice» con la app abierta; se solicita al utilizarlo.
- BIND_NOTIFICATION_LISTENER_SERVICE: permiso especial que el usuario debe otorgar expresamente para detectar notificaciones de Amazon Alexa. Este permiso puede causar un bloqueo de Google Play Protect en APK externos. No se debe desactivar Play Protect para instalarla.

## Compilar
Mantener el workflow existente del repositorio en .github/workflows/build-apk.yml. Subir los archivos de esta carpeta AlexaVoice/ a la misma carpeta AlexaVoice/ del repositorio. GitHub Actions compilara un APK de prueba. Este codigo no se ha compilado ni examinado en un HONOR X8a desde esta entrega.

## Seguridad
No se proporciona garantia de aprobacion por Google Play Protect. No instalar APK si Play Protect lo bloquea. El codigo necesita una auditoria completa antes de una distribucion amplia. Para futuras actualizaciones conviene firmar con una clave de produccion estable.
