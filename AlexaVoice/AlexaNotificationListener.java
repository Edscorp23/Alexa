package com.alexavoice.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.Locale;

public class AlexaNotificationListener extends NotificationListenerService {
    static final String PREFS = "reminder_state";
    static final String KEY_TEXT = "pending_text";
    static final String KEY_NOTIFICATION = "source_key";
    static final String ACTION_CONFIRM = "com.alexavoice.app.CONFIRM_REMINDER";
    static final String ACTION_SPOKEN = "com.alexavoice.app.REMINDER_SPOKEN";
    static final String EXTRA_START_VOICE = "start_voice_confirmation";
    static final String CHANNEL_ID = "alexa_pending_reminder";
    private static final String ALEXA_PACKAGE = "com.amazon.dee.app";
    private static final long REPEAT_DELAY_MS = 120000L;
    private static final int REMINDER_NOTIFICATION_ID = 751;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ready = false;
    private String pendingText = "";
    private String pendingSourceKey = "";
    private long lastSeenAt = 0;
    private BroadcastReceiver confirmationReceiver;

    private final Runnable repeatTask = new Runnable() {
        @Override public void run() {
            if (pendingText.isEmpty()) return;
            sayPending();
            handler.postDelayed(this, REPEAT_DELAY_MS);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Recordatorios pendientes", NotificationManager.IMPORTANCE_HIGH));

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        pendingText = p.getString(KEY_TEXT, "");
        pendingSourceKey = p.getString(KEY_NOTIFICATION, "");

        confirmationReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (ACTION_CONFIRM.equals(intent.getAction())) confirm();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_CONFIRM);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(confirmationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(confirmationReceiver, filter);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                int result = tts.setLanguage(new Locale("es", "ES"));
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                chooseFemaleSpanishVoice();
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { }
                    @Override public void onError(String utteranceId) { }
                    @Override public void onDone(String utteranceId) {
                        if (utteranceId.startsWith("repeat_")) {
                            Intent finished = new Intent(ACTION_SPOKEN);
                            finished.setPackage(getPackageName());
                            sendBroadcast(finished);
                        }
                    }
                });
            }
            if (!pendingText.isEmpty()) {
                showConfirmNotification();
                handler.removeCallbacks(repeatTask);
                handler.postDelayed(repeatTask, REPEAT_DELAY_MS);
            }
        });
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        if (!pendingText.isEmpty()) {
            showConfirmNotification();
            handler.removeCallbacks(repeatTask);
            handler.postDelayed(repeatTask, REPEAT_DELAY_MS);
        }
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !ALEXA_PACKAGE.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;
        Bundle e = n.extras;
        if (e == null) return;
        CharSequence title = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (body == null || body.length() == 0) body = e.getCharSequence(Notification.EXTRA_TEXT);
        String message = join(title, body);
        if (message.length() < 4 || message.equalsIgnoreCase("alexa")) return;

        // A re-post of the same notification should not reset the reminder or cause a loop.
        String sourceKey = sbn.getKey();
        long now = System.currentTimeMillis();
        if (sourceKey.equals(pendingSourceKey) || (message.equals(pendingText) && now - lastSeenAt < 10000)) return;
        pendingText = message;
        pendingSourceKey = sourceKey;
        lastSeenAt = now;
        persist();
        showConfirmNotification();
        if (ready) sayPending();
        handler.removeCallbacks(repeatTask);
        handler.postDelayed(repeatTask, REPEAT_DELAY_MS);
    }

    private void sayPending() {
        if (ready && tts != null && !pendingText.isEmpty()) {
            tts.speak("Recordatorio de Alexa. " + pendingText + ". Para detener los avisos, pulsa Ya lo hice en Alexa Voice.",
                    TextToSpeech.QUEUE_FLUSH, null, "repeat_" + System.currentTimeMillis());
        }
    }

    private void showConfirmNotification() {
        if (pendingText.isEmpty()) return;
        Intent action = new Intent(this, ConfirmReminderReceiver.class);
        action.setAction(ACTION_CONFIRM);
        PendingIntent confirm = PendingIntent.getBroadcast(this, 11, action, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open = PendingIntent.getActivity(this, 12, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent voiceIntent = new Intent(this, MainActivity.class);
        voiceIntent.putExtra(EXTRA_START_VOICE, true);
        voiceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent voice = PendingIntent.getActivity(this, 13, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alexa Voice: recordatorio pendiente")
                .setContentText(pendingText)
                .setStyle(new Notification.BigTextStyle().bigText(pendingText))
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Ya lo hice", confirm).build())
                .addAction(new Notification.Action.Builder(null, "Responder por voz", voice).build())
                .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(REMINDER_NOTIFICATION_ID, notification);
    }

    static void confirm(Context context) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_TEXT).remove(KEY_NOTIFICATION).apply();
        ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE)).cancel(REMINDER_NOTIFICATION_ID);
        Intent intent = new Intent(ACTION_CONFIRM);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    private void confirm() {
        pendingText = "";
        pendingSourceKey = "";
        handler.removeCallbacks(repeatTask);
        if (tts != null) tts.stop();
    }

    private void persist() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_TEXT, pendingText).putString(KEY_NOTIFICATION, pendingSourceKey).apply();
    }

    private String join(CharSequence a, CharSequence b) {
        String title = a == null ? "" : a.toString().trim();
        String body = b == null ? "" : b.toString().trim();
        if (title.isEmpty()) return body;
        if (body.isEmpty() || title.equals(body)) return title;
        return title + ". " + body;
    }

    private void chooseFemaleSpanishVoice() {
        if (tts == null) return;
        try {
            for (Voice voice : tts.getVoices()) {
                String n = voice.getName().toLowerCase(Locale.ROOT);
                if ("es".equals(voice.getLocale().getLanguage()) &&
                    (n.contains("female") || n.contains("woman") || n.contains("femen"))) {
                    tts.setVoice(voice);
                    return;
                }
            }
        } catch (Exception ignored) { }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(repeatTask);
        if (confirmationReceiver != null) unregisterReceiver(confirmationReceiver);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
