package com.alexavoice.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** A one-shot screen-off audio test. Does not create or confirm a reminder. */
public final class ScreenOffTestReceiver extends BroadcastReceiver {
    static final String ACTION_TEST = "com.alexavoice.app.TEST_SCREEN_OFF";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_TEST.equals(intent.getAction())) return;
        final PendingResult pending = goAsync();
        final Handler handler = new Handler(Looper.getMainLooper());
        final PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wake = power == null ? null :
            power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlexaVoice:ScreenOffTest");
        if (wake != null) wake.acquire(25000L);
        final AtomicBoolean finished = new AtomicBoolean(false);
        final TextToSpeech[] engine = new TextToSpeech[1];
        final Runnable cleanup = () -> {
            if (!finished.compareAndSet(false, true)) return;
            try { if (engine[0] != null) engine[0].shutdown(); } catch (Exception ignored) {}
            try { if (wake != null && wake.isHeld()) wake.release(); } catch (Exception ignored) {}
            pending.finish();
        };
        handler.postDelayed(cleanup, 23000L);
        try {
            engine[0] = new TextToSpeech(context.getApplicationContext(), status -> {
                if (finished.get()) return;
                TextToSpeech tts = engine[0];
                if (status != TextToSpeech.SUCCESS || tts == null) { cleanup.run(); return; }
                int language = tts.setLanguage(new Locale("es", "ES"));
                if (language == TextToSpeech.LANG_MISSING_DATA ||
                    language == TextToSpeech.LANG_NOT_SUPPORTED) { cleanup.run(); return; }
                try {
                    for (Voice voice : tts.getVoices()) {
                        String name = voice.getName().toLowerCase(Locale.ROOT);
                        if ("es".equals(voice.getLocale().getLanguage()) &&
                            (name.contains("female") || name.contains("femen"))) {
                            tts.setVoice(voice); break;
                        }
                    }
                } catch (Exception ignored) {}
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) { handler.post(cleanup); }
                    @Override public void onError(String id) { handler.post(cleanup); }
                });
                String message = "Prueba de Alexa Voice. Tu pantalla puede estar apagada. " +
                    "Si escuchas este mensaje, el audio de prueba funciona con el telefono bloqueado.";
                if (tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "screen_off_test") ==
                    TextToSpeech.ERROR) cleanup.run();
            });
        } catch (Exception e) { cleanup.run(); }
    }
}
