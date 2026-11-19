package com.alexavoice.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView pendingView;
    private TextView stateView;
    private TextToSpeech tts;
    private boolean ttsReady;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        FrameLayout canvas = new FrameLayout(this);
        canvas.setBackgroundColor(Color.BLACK);
        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.anime_background);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        canvas.addView(background, new FrameLayout.LayoutParams(-1, -1));
        View overlay = new View(this);
        overlay.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xC5000000, 0x40000000, 0xED000000}));
        canvas.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroller = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(dp(22), dp(32), dp(22), dp(32));
        root.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(root);
        canvas.addView(scroller, new FrameLayout.LayoutParams(-1, -1));
        setContentView(canvas);

        root.addView(text("AVENTURA · RECORDATORIOS", 12, 0xFFFFD28A, true));
        root.addView(text("Alexa Voice", 34, Color.WHITE, true));
        root.addView(text("Recordatorios hablados, sin micrófono.", 15, 0xFFE8DCD0, false));
        root.addView(new Space(this), new LinearLayout.LayoutParams(1, dp(110)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(19), dp(18), dp(19), dp(18));
        card.setBackground(panel(0xE8231D22, 24));
        root.addView(card, fullWidth());
        card.addView(text("RECORDATORIO PENDIENTE", 12, 0xFFFFD28A, true));
        pendingView = text("Sin recordatorios pendientes", 21, Color.WHITE, true);
        pendingView.setPadding(0, dp(8), 0, dp(6));
        card.addView(pendingView);
        stateView = text("Listo para recibir recordatorios", 14, 0xFFE8DCD0, false);
        card.addView(stateView);
        card.addView(text("Se repite cada 2 minutos hasta confirmar.", 13, 0xFFFFD28A, false));

        Button done = button("✓  YA LO HICE", 0xFFFFB749, 0xFF21160F);
        done.setOnClickListener(v -> {
            AlexaNotificationListener.confirm(this);
            updatePending();
            stateView.setText("Confirmado. Repeticiones detenidas.");
        });
        root.addView(done, fullWidth());
        Button test = button("▶  PROBAR VOZ", 0xCC252532, Color.WHITE);
        test.setOnClickListener(v -> {
            if (ttsReady) tts.speak("Hola. Soy Alexa Voice. Leeré tus recordatorios de Alexa.",
                    TextToSpeech.QUEUE_FLUSH, null, "test");
            else Toast.makeText(this, "La voz aún no está disponible", Toast.LENGTH_SHORT).show();
        });
        root.addView(test, fullWidth());
        Button access = button("⚙  ACTIVAR ACCESO A NOTIFICACIONES", 0xCC252532, Color.WHITE);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access, fullWidth());
        root.addView(text("Solo se procesan notificaciones de la aplicación Amazon Alexa. " +
                "Android concede acceso amplio a las notificaciones al habilitar este servicio. " +
                "El micrófono está desactivado: esta versión no solicita permiso de audio.",
                13, 0xFFE8DCD0, false));

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 501);
        }
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                int result = tts.setLanguage(new Locale("es", "ES"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED;
                selectSpanishVoice();
            }
        });
        updatePending();
    }

    @Override protected void onResume() { super.onResume(); updatePending(); }

    private void updatePending() {
        if (pendingView == null || stateView == null) return;
        String pending = getSharedPreferences(AlexaNotificationListener.PREFS, MODE_PRIVATE)
                .getString(AlexaNotificationListener.KEY_TEXT, "");
        pendingView.setText(pending.isEmpty() ? "Sin recordatorios pendientes" : pending);
        stateView.setText(pending.isEmpty() ? "Esperando recordatorios de Amazon Alexa" :
                "Pendiente de confirmación");
    }

    private void selectSpanishVoice() {
        if (tts == null) return;
        try {
            for (Voice v : tts.getVoices()) {
                String name = v.getName().toLowerCase(Locale.ROOT);
                if ("es".equals(v.getLocale().getLanguage()) &&
                    (name.contains("female") || name.contains("woman") || name.contains("femen"))) {
                    tts.setVoice(v); break;
                }
            }
        } catch (Exception ignored) { }
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String content, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(content);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }
    private GradientDrawable panel(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private Button button(String title, int background, int color) {
        Button b = new Button(this);
        b.setText(title); b.setTextColor(color); b.setAllCaps(false);
        b.setTextSize(15); b.setBackground(panel(background, 16));
        b.setMinHeight(dp(54)); return b;
    }
    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, dp(8)); return params;
    }
    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
