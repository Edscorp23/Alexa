package com.alexavoice.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class RepeatReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!ReminderEngine.ACTION_REPEAT.equals(intent.getAction())) return;
        if (ReminderEngine.count(context)==0) return;
        final PendingResult pending=goAsync();
        ReminderEngine.speakIfDue(context,false,pending::finish);
    }
}
