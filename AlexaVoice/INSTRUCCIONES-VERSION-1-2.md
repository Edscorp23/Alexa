# Alexa Voice 1.2 - confirmar recordatorios por voz o por boton

## Funciones
- El recordatorio se vuelve a leer cada dos minutos mientras el servicio siga activo.
- La notificacion de Alexa Voice muestra **Ya lo hice** (detiene el recordatorio) y **Responder por voz** (abre la app).
- En la app se puede pulsar **Responder por voz**, o decir **Ya lo hice**, **Terminado**, **Listo**, **Completado** mientras el microfono este escuchando.
- Si la app **esta abierta en primer plano**, al finalizar cada recordatorio hablado inicia automaticamente una ventana breve de escucha. No se escucha permanentemente.
- Si la pantalla esta bloqueada, el sistema puede requerir desbloquear antes de responder por voz; el boton **Ya lo hice** en la notificacion no necesita reconocimiento de voz.
- Los permisos son acceso a notificaciones, notificaciones (Android 13+) y microfono. No se envian grabaciones a servidores propios; el servicio de reconocimiento instalado en Android puede procesar audio en la nube segun sus politicas.

## Actualizar GitHub
1. Extraer el ZIP.
2. Subir los archivos de la carpeta AlexaVoice a la carpeta AlexaVoice ya existente en el repositorio, conservando todas las subcarpetas. Reemplazar los archivos con el mismo nombre.
3. Verificar que **.github/workflows/build-apk.yml** esta en la RAIZ del repositorio (NO dentro de AlexaVoice) y que contiene el comando `gradle -p AlexaVoice --no-daemon assembleDebug`.
4. Actions > ultima ejecucion > Artifacts > AlexaVoice-debug-apk y descargar el APK.
5. Instalar como actualizacion sobre la version anterior. El identificador de la app se mantiene.

## Limitaciones
- No hay escucha manos libres continua con el movil bloqueado. Implementarla requeriria un servicio de primer plano permanente, uso declarado del microfono y tratar con restricciones de cada version de Android/HONOR. Esta version evita prometer esa funcion.
- No hay garantia de ejecucion perpetua de las repeticiones si MagicOS detiene el servicio. Permitir actividad en segundo plano en ajustes de bateria si fuera necesario.
- Solo se gestiona un recordatorio pendiente a la vez (como en 1.1); nuevos recordatorios reemplazan el anterior.
- No se ha compilado ni probado en hardware HONOR X8a en este entorno.
