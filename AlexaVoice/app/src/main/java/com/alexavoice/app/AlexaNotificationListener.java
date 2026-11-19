package com.alexavoice.app;

import android.app.Notification;
import android.os.Bundle;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Locale;

/** Only accepts reminder-looking notifications originating from Amazon Alexa. */
public final class AlexaNotificationListener extends NotificationListenerService {
    private static final String ALEXA_PACKAGE = "com.amazon.dee.app";

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        ReminderEngine.refreshNotification(this);
        ReminderEngine.scheduleNext(this);
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !ALEXA_PACKAGE.equals(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null || (n.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;
        Bundle e = n.extras;
        if (e == null) return;
        CharSequence t = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence b = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (b == null || b.length() == 0) b = e.getCharSequence(Notification.EXTRA_TEXT);
        String title = t == null ? "" : t.toString().trim();
        String body = b == null ? "" : b.toString().trim();
        String label = (title + " " + body + " " + (Build.VERSION.SDK_INT >= 26 && n.getChannelId() != null ? n.getChannelId() : "")).toLowerCase(Locale.ROOT);
        if (!looksLikeReminder(label)) return;
        String message = body.isEmpty() ? title : body;
        if (message.length() < 3) return;
        if (ReminderEngine.add(this, sbn.getKey(), message)) {
            ReminderEngine.refreshNotification(this);
            ReminderEngine.speakIfDue(this, true, null);
            ReminderEngine.scheduleNext(this);
        }
    }

    private boolean looksLikeReminder(String text) {
        return text.contains("recordatorio") || text.contains("reminder")
            || text.contains("te recuerdo") || text.contains("recuerda")
            || text.contains("es hora de");
    }
}
