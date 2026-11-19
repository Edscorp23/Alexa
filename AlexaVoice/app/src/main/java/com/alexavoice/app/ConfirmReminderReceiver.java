package com.alexavoice.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ConfirmReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (ReminderEngine.ACTION_CONFIRM.equals(intent.getAction())) ReminderEngine.confirmCurrent(context);
    }
}
