package com.alexavoice.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Explicitly user-started foreground microphone session. Android speech recognition
 * is not a true always-on hotword engine and vendors may suspend it on lock.
 * Never auto-start this service after boot or from a background notification.
 */
public class MusicVoiceService extends Service {
    static final String ACTION_MUSIC_SEARCH = "com.alexavoice.app.MUSIC_SEARCH";
    static final String EXTRA_SEARCH = "search";
    private static final String ACTION_STOP = "com.alexavoice.app.STOP_MUSIC_LISTENING";
    private static final String CHANNEL = "music_voice_listening";
    private static final String SEARCH_CHANNEL = "spotify_search_requests";
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean stopping = false;
    private boolean waitingForCommand = false;
    private int consecutiveErrors = 0;
    private static final int ONGOING_ID = 880;
    private final Runnable restart = this::startRecognition;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Alexa Voice - micrófono activo", NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(SEARCH_CHANNEL, "Abrir búsquedas de Spotify", NotificationManager.IMPORTANCE_HIGH));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        PendingIntent stop = PendingIntent.getService(this, 1,
            new Intent(this, MusicVoiceService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open = PendingIntent.getActivity(this, 2, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Alexa Voice: escucha musical activa")
            .setContentText("Di 'Alexa Voice, pausa la música'. Toca para abrir")
            .setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "Detener micrófono", stop).build())
            .build();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ONGOING_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            else startForeground(ONGOING_ID, n);
        } catch (RuntimeException e) {
            stopSelf(); return START_NOT_STICKY;
        }
        stopping = false;
        main.removeCallbacks(restart);
        main.post(restart);
        return START_NOT_STICKY;
    }

    private void startRecognition() {
        if (stopping) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { stopSelf(); return; }
        try {
            if (recognizer != null) { recognizer.destroy(); recognizer = null; }
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle b) { }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float value) { }
                @Override public void onBufferReceived(byte[] bytes) { }
                @Override public void onEndOfSpeech() { }
                @Override public void onError(int code) {
                    if (code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { stopSelf(); return; }
                    if (code == SpeechRecognizer.ERROR_NO_MATCH || code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        consecutiveErrors = 0;
                        scheduleNext(1200);
                        return;
                    }
                    consecutiveErrors++;
                    if (consecutiveErrors > 25) { stopSelf(); return; }
                    scheduleNext(consecutiveErrors > 3 ? 4500 : 1800);
                }
                @Override public void onResults(Bundle b) {
                    consecutiveErrors = 0;
                    ArrayList<String> text = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (text != null) for (String result : text) {
                        if (process(result)) break;
                    }
                    scheduleNext(1100);
                }
                @Override public void onPartialResults(Bundle b) { }
                @Override public void onEvent(int type, Bundle b) { }
            });
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            recognizer.startListening(i);
        } catch (RuntimeException error) { scheduleNext(3000); }
    }
    private void scheduleNext(long delayMs) {
        main.removeCallbacks(restart);
        if (recognizer != null) { try { recognizer.destroy(); } catch (RuntimeException ignored) { } recognizer = null; }
        if (!stopping) main.postDelayed(restart, delayMs);
    }
    private String norm(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }
    private boolean process(String raw) {
        String input = norm(raw);
        int idx = input.indexOf("alexa voice");
        String command;
        if (idx >= 0) {
            command = input.substring(idx + "alexa voice".length()).trim();
            if (command.startsWith(",")) command = command.substring(1).trim();
            if (command.isEmpty()) { waitingForCommand = true; return true; }
            waitingForCommand = false;
        } else if (waitingForCommand) {
            command = input; waitingForCommand = false;
        } else return false;
        if (command.startsWith("pausa") || command.contains("deten la musica")) return transport("pause");
        if (command.contains("continua") || command.contains("reanuda") || command.contains("sigue la musica")) return transport("play");
        if (command.contains("siguiente")) return transport("next");
        if (command.contains("anterior")) return transport("previous");
        String query = null;
        if (command.startsWith("busca ")) query = command.substring(6);
        else if (command.startsWith("pon musica de ")) query = command.substring(14);
        else if (command.startsWith("pon la cancion ")) query = command.substring(15);
        else if (command.startsWith("reproduce ")) query = command.substring(10);
        if (query != null && !query.trim().isEmpty()) {
            requestSearch(query.trim()); return true;
        }
        return false;
    }
    private boolean transport(String name) {
        boolean ok = SpotifyMusic.transport(this, name);
        if (!ok) postSearchNotification("Spotify no tiene una sesión activa. Ábrelo y reproduce algo primero.", null);
        return true;
    }
    private void requestSearch(String text) {
        // Android restricts activity launches from services in the background.
        // A visible activity can open Spotify immediately, otherwise leave a tappable notification.
        Intent broadcast = new Intent(ACTION_MUSIC_SEARCH).setPackage(getPackageName());
        broadcast.putExtra(EXTRA_SEARCH, text);
        sendBroadcast(broadcast);
        postSearchNotification("Abrir en Spotify: " + text, text);
    }
    private void postSearchNotification(String message, String text) {
        Intent i;
        if (text != null) {
            i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("spotify:search:" + android.net.Uri.encode(text)));
            i.setPackage("com.spotify.music");
        } else i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent tap = PendingIntent.getActivity(this, 3, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, SEARCH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Alexa Voice · Spotify")
            .setContentText(message).setAutoCancel(true).setContentIntent(tap).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(881, n);
    }
    @Override public void onDestroy() {
        stopping = true;
        main.removeCallbacks(restart);
        if (recognizer != null) { try { recognizer.cancel(); recognizer.destroy(); } catch (Exception ignored) { } recognizer = null; }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
