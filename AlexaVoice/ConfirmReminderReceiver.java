package com.alexavoice.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConfirmReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (AlexaNotificationListener.ACTION_CONFIRM.equals(intent.getAction())) {
            AlexaNotificationListener.confirm(context);
        }
    }
}
