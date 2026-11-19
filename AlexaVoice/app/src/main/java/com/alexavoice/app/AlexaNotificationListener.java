package com.alexavoice.app;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;

public class AlexaNotificationListener extends NotificationListenerService {
    // Amazon Alexa's commonly used Android package ID. If Amazon changes it, update this constant.
    private static final String ALEXA_PACKAGE = "com.amazon.dee.app";
    private TextToSpeech tts;
    private boolean ready = false;
    private String lastKey = "";
    private long lastSpokenAt = 0;

    @Override public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("es", "ES"));
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                chooseFemaleSpanishVoice();
            }
        });
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !ALEXA_PACKAGE.equals(sbn.getPackageName()) || !ready || tts == null) return;
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;
        Bundle e = n.extras;
        if (e == null) return;
        CharSequence title = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (text == null || text.length() == 0) text = e.getCharSequence(Notification.EXTRA_TEXT);
        String message = join(title, text);
        if (message.isEmpty()) return;
        String lower = message.toLowerCase(Locale.ROOT);
        // Avoid reading generic Alexa status notifications; reminder wording varies by app language.
        if (lower.length() < 4 || lower.equals("alexa")) return;
        String key = sbn.getKey() + ":" + sbn.getPostTime();
        long now = System.currentTimeMillis();
        if (key.equals(lastKey) || now - lastSpokenAt < 1800) return;
        lastKey = key; lastSpokenAt = now;
        tts.speak(message, TextToSpeech.QUEUE_ADD, null, "alexa_reminder_" + now);
    }

    private String join(CharSequence title, CharSequence body) {
        String a = title == null ? "" : title.toString().trim();
        String b = body == null ? "" : body.toString().trim();
        if (a.isEmpty()) return b;
        if (b.isEmpty() || b.equals(a)) return a;
        return a + ". " + b;
    }

    private void chooseFemaleSpanishVoice() {
        try {
            for (Voice voice : tts.getVoices()) {
                String n = voice.getName().toLowerCase(Locale.ROOT);
                if (voice.getLocale().getLanguage().equals("es") &&
                    (n.contains("female") || n.contains("woman") || n.contains("femen"))) {
                    tts.setVoice(voice); return;
                }
            }
        } catch (Exception ignored) { }
    }

    @Override public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
