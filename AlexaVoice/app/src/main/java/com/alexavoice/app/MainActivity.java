package com.alexavoice.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 56, 48, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(0xFFF7F9FC);

        TextView title = new TextView(this);
        title.setText("Alexa Voice"); title.setTextSize(30); title.setTextColor(0xFF172033);
        title.setGravity(Gravity.CENTER); title.setPadding(0, 12, 0, 12);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("Lee en voz alta las notificaciones de Alexa con la voz en español instalada en tu HONOR.");
        intro.setTextSize(17); intro.setTextColor(0xFF46536A); intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, 8, 0, 28); root.addView(intro);

        Button access = new Button(this);
        access.setText("1. Activar acceso a notificaciones");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access, fullWidth());

        Button test = new Button(this);
        test.setText("2. Probar voz femenina");
        test.setOnClickListener(v -> {
            if (ttsReady) speak("Hola. Soy Alexa Voice. Los recordatorios de Alexa se leerán aquí.");
            else Toast.makeText(this, "Preparando la voz. Inténtalo de nuevo en unos segundos.", Toast.LENGTH_SHORT).show();
        });
        root.addView(test, fullWidth());

        TextView note = new TextView(this);
        note.setText("Importante: activa también las notificaciones de Amazon Alexa. Para que funcione con la pantalla apagada, permite que Alexa Voice se ejecute en segundo plano si el sistema HONOR lo solicita. La voz depende de las voces españolas instaladas en Android.");
        note.setTextSize(14); note.setTextColor(0xFF596579); note.setPadding(0, 28, 0, 0);
        root.addView(note);
        setContentView(root);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("es", "ES"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                chooseFemaleSpanishVoice();
            }
        });
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 10, 0, 10); return p;
    }

    private void chooseFemaleSpanishVoice() {
        if (tts == null) return;
        try {
            for (Voice voice : tts.getVoices()) {
                String n = voice.getName().toLowerCase(Locale.ROOT);
                if (voice.getLocale().getLanguage().equals("es") &&
                    (n.contains("female") || n.contains("woman") || n.contains("femen"))) {
                    tts.setVoice(voice); return;
                }
            }
        } catch (Exception ignored) { }
        // Android does not expose a standard gender flag for TTS voices; use the system Spanish default if no named female voice exists.
    }

    private void speak(String text) {
        if (tts != null && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alexa_voice_test");
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
