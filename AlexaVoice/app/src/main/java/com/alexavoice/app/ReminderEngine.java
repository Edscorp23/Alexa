package com.alexavoice.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persistence and best-effort scheduled playback without microphone or network access. */
public final class ReminderEngine {
    static final String PREFS = "pending_reminders_17";
    static final String ACTION_CONFIRM = "com.alexavoice.app.CONFIRM_REMINDER";
    static final String ACTION_REPEAT = "com.alexavoice.app.REPEAT_REMINDER";
    private static final String CHANNEL = "pending_reminders_v17";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_LAST_SPOKEN = "last_spoken";
    private static final String KEY_COMPLETED = "completed_keys";
    private static final int NOTIFICATION_ID = 751;
    private static final long INTERVAL = 120000L;
    private static final Object LOCK = new Object();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Runnable NO_OP = () -> {};
    private ReminderEngine() {}

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private static JSONArray load(Context c) {
        try { return new JSONArray(prefs(c).getString(KEY_ITEMS, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    private static void store(Context c, JSONArray a) { prefs(c).edit().putString(KEY_ITEMS,a.toString()).apply(); }
    public static int count(Context c) { synchronized (LOCK) { return load(c).length(); } }
    public static String current(Context c) {
        synchronized (LOCK) { return load(c).optJSONObject(0) == null ? "" : load(c).optJSONObject(0).optString("text", ""); }
    }
    public static boolean add(Context c, String sourceKey, String text) {
        synchronized (LOCK) {
            JSONArray a = load(c);
            String completed = prefs(c).getString(KEY_COMPLETED,"|");
            if(completed.contains("|"+sourceKey+"|")) return false;
            for (int i=0;i<a.length();i++) {
                JSONObject item=a.optJSONObject(i);
                if (item == null) continue;
                if (sourceKey.equals(item.optString("key")) || text.equals(item.optString("text"))) return false;
            }
            if (a.length() >= 20) return false; // avoid unlimited private data storage
            try {
                JSONObject item=new JSONObject(); item.put("key",sourceKey); item.put("text",text); a.put(item);
                store(c,a); return true;
            } catch(Exception ignored) { return false; }
        }
    }
    public static void confirmCurrent(Context c) {
        synchronized (LOCK) {
            JSONArray items=load(c), remaining=new JSONArray();
            JSONObject first=items.optJSONObject(0);
            if(first!=null) {
                String key=first.optString("key", "");
                if(!key.isEmpty()) {
                    String old=prefs(c).getString(KEY_COMPLETED,"|");
                    String next=old+key+"|";
                    if(next.length()>3000) next=next.substring(Math.max(0,next.length()-2500));
                    prefs(c).edit().putString(KEY_COMPLETED,next).apply();
                }
            }
            for(int i=1;i<items.length();i++) remaining.put(items.opt(i));
            store(c,remaining);
            prefs(c).edit().putLong(KEY_LAST_SPOKEN,0).apply();
        }
        refreshNotification(c);
        scheduleNext(c);
    }
    private static PendingIntent alarmIntent(Context c) {
        Intent intent=new Intent(c,RepeatReminderReceiver.class).setAction(ACTION_REPEAT);
        return PendingIntent.getBroadcast(c,31,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void scheduleNext(Context c) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if (am==null) return;
        PendingIntent alarm=alarmIntent(c);
        am.cancel(alarm);
        if (count(c)==0) return;
        long last=prefs(c).getLong(KEY_LAST_SPOKEN,0);
        long trigger=Math.max(System.currentTimeMillis()+5000L,last+INTERVAL);
        // No exact alarm permission. Doze can defer delivery; this is a best-effort fallback.
        if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime()+Math.max(5000L, trigger-System.currentTimeMillis()),alarm);
        else am.set(AlarmManager.RTC_WAKEUP,trigger,alarm);
    }
    public static void refreshNotification(Context c) {
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Recordatorios por confirmar",NotificationManager.IMPORTANCE_HIGH));
        }
        String current=current(c);
        if(current.isEmpty()){ nm.cancel(NOTIFICATION_ID);return; }
        Intent ack=new Intent(c,ConfirmReminderReceiver.class).setAction(ACTION_CONFIRM);
        PendingIntent done=PendingIntent.getBroadcast(c,11,ack,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(c,12,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26 ? new Notification.Builder(c,CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
         .setContentTitle("Alexa Voice: " + count(c) + " pendiente(s)")
         .setContentText(current).setStyle(new Notification.BigTextStyle().bigText(current))
         .setContentIntent(open).setOngoing(true)
         .addAction(new Notification.Action.Builder(null,"Ya lo hice",done).build());
        nm.notify(NOTIFICATION_ID,b.build());
    }
    public static void speakIfDue(Context c, boolean immediate, Runnable finished) {
        String message;
        synchronized(LOCK){
            message=current(c);
            if (message.isEmpty()) { if(finished!=null)finished.run();return; }
            long now=System.currentTimeMillis(), last=prefs(c).getLong(KEY_LAST_SPOKEN,0);
            if (!immediate && now-last < INTERVAL-10000L) {
                scheduleNext(c);
                if(finished!=null)finished.run(); return;
            }
            prefs(c).edit().putLong(KEY_LAST_SPOKEN,now).apply();
        }
        scheduleNext(c);
        final PowerManager pm=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock lock=pm==null?null:pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"AlexaVoice:ReadReminder");
        if(lock!=null)lock.acquire(25000L);
        final AtomicBoolean completed=new AtomicBoolean(false);
        final TextToSpeech[] ref=new TextToSpeech[1];
        final Runnable cleanup=()->{
            if(!completed.compareAndSet(false,true))return;
            try{if(ref[0]!=null){ref[0].stop();ref[0].shutdown();}}catch(Exception ignored){}
            try{if(lock!=null&&lock.isHeld())lock.release();}catch(Exception ignored){}
            if(finished!=null)finished.run();
        };
        MAIN.postDelayed(cleanup,23000L);
        try {
            ref[0]=new TextToSpeech(c.getApplicationContext(), status -> {
                if(completed.get())return;
                if(status != TextToSpeech.SUCCESS || ref[0]==null){cleanup.run();return;}
                TextToSpeech tts=ref[0];
                int lang=tts.setLanguage(new Locale("es","ES"));
                if(lang==TextToSpeech.LANG_MISSING_DATA || lang==TextToSpeech.LANG_NOT_SUPPORTED){cleanup.run();return;}
                try { for(Voice v:tts.getVoices()) {
                    String name=v.getName().toLowerCase(Locale.ROOT);
                    if("es".equals(v.getLocale().getLanguage()) && (name.contains("female")||name.contains("femen"))){ tts.setVoice(v);break; }
                }} catch(Exception ignored){}
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String id){}
                    @Override public void onDone(String id){MAIN.post(cleanup);}
                    @Override public void onError(String id){MAIN.post(cleanup);}
                });
                String spoken="Recordatorio de Alexa. " + message + ". Pulsa Ya lo hice para confirmar.";
                if(tts.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,"reminder")==TextToSpeech.ERROR) cleanup.run();
            });
        } catch(Exception ex){cleanup.run();}
    }
}
