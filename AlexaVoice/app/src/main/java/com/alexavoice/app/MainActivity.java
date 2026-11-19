package com.alexavoice.app;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.Space;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int AUDIO_REQUEST = 403;
    private TextToSpeech tts;
    private boolean ttsReady;
    private SpeechRecognizer recognizer;
    private boolean listening;
    private boolean visible;
    private boolean automaticVoice = true;
    private TextView stateText;
    private TextView pendingTextView;
    private BroadcastReceiver reminderSpokenReceiver;
    private BroadcastReceiver musicCommandReceiver;
    private TextView musicStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Full-screen artwork, tinted for legible high-contrast controls.
        FrameLayout canvas = new FrameLayout(this);
        canvas.setBackgroundColor(Color.BLACK);
        ImageView wallpaper = new ImageView(this);
        wallpaper.setImageResource(R.drawable.anime_background);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        canvas.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB9000000, 0x3A000000, 0xE8000000}));
        canvas.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(32), dp(22), dp(32));
        scroll.addView(root);
        canvas.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(canvas);

        TextView eyebrow = label("AVENTURA • RECORDATORIOS", 12, 0xFFFFD589, true);
        eyebrow.setLetterSpacing(.17f);
        root.addView(eyebrow, fullWidth());

        TextView heading = label("Alexa Voice", 34, Color.WHITE, true);
        heading.setPadding(0, dp(4), 0, 0);
        root.addView(heading, fullWidth());

        TextView intro = label("Tu voz, tus recordatorios, tu aventura.", 15, 0xFFE6E2DD, false);
        root.addView(intro, fullWidth());

        Space space = new Space(this);
        root.addView(space, new LinearLayout.LayoutParams(1, dp(105)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setBackground(panel(0xE8231D22, 24, 0x55FFE0AD));
        LinearLayout.LayoutParams cardParams = fullWidth();
        cardParams.setMargins(0, dp(8), 0, dp(14));
        root.addView(card, cardParams);

        TextView tag = label("●  RECORDATORIO ACTUAL", 12, 0xFFFFCE80, true);
        card.addView(tag, fullWidth());

        pendingTextView = label("Sin recordatorios pendientes", 21, Color.WHITE, true);
        pendingTextView.setPadding(0, dp(9), 0, dp(6));
        card.addView(pendingTextView, fullWidth());

        stateText = label("Listo para recibir notificaciones", 13, 0xFFE8DCCD, false);
        card.addView(stateText, fullWidth());

        TextView interval = label("↻  Repetición cada 2 minutos hasta confirmar", 13, 0xFFFFD793, false);
        interval.setPadding(0, dp(8), 0, 0);
        card.addView(interval, fullWidth());

        Button confirm = actionButton("✓  YA LO HICE", 0xFFFFB749, 0xFF1C140F);
        confirm.setOnClickListener(v -> confirmReminder());
        root.addView(confirm, fullWidth());

        Button microphone = actionButton("🎙  RESPONDER POR VOZ", 0xFF244867, Color.WHITE);
        microphone.setOnClickListener(v -> startVoiceWithPermission());
        root.addView(microphone, fullWidth());

        LinearLayout musicCard = new LinearLayout(this);
        musicCard.setOrientation(LinearLayout.VERTICAL);
        musicCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        musicCard.setBackground(panel(0xE8231D22, 24, 0x55FFE0AD));
        LinearLayout.LayoutParams musicCardParams = fullWidth();
        musicCardParams.setMargins(0, dp(14), 0, dp(14));
        root.addView(musicCard, musicCardParams);
        musicCard.addView(label("♫  SPOTIFY · CONTROL POR VOZ", 12, 0xFFFFCE80, true), fullWidth());
        musicStatus = label("Activa el micrófono y di: Alexa Voice, pausa la música", 15, Color.WHITE, false);
        musicCard.addView(musicStatus, fullWidth());
        Button activateMusic = actionButton("🎙  ACTIVAR ESCUCHA MUSICAL", 0xFF244867, Color.WHITE);
        activateMusic.setOnClickListener(v -> startMusicWithPermission());
        musicCard.addView(activateMusic, fullWidth());
        Button stopMusic = actionButton("■  DETENER ESCUCHA", 0xB81C1D26, Color.WHITE);
        stopMusic.setOnClickListener(v -> {
            stopService(new Intent(this, MusicVoiceService.class));
            musicStatus.setText("Escucha musical detenida");
        });
        musicCard.addView(stopMusic, fullWidth());
        Button openSpotify = actionButton("♫  ABRIR SPOTIFY", 0xB81C1D26, Color.WHITE);
        openSpotify.setOnClickListener(v -> SpotifyMusic.openSpotify(this));
        musicCard.addView(openSpotify, fullWidth());
        musicCard.addView(label("Comandos: Alexa Voice, busca rock; pausa; continúa; siguiente canción. " +
            "La búsqueda abre Spotify, no garantiza reproducción automática. " +
            "La escucha requiere permiso de micrófono y una notificación permanente.", 12, 0xFFE6E2DD, false), fullWidth());

        Button test = actionButton("♪  PROBAR VOZ", 0xB81C1D26, Color.WHITE);
        test.setOnClickListener(v -> {
            if (ttsReady) tts.speak("Hola. Soy Alexa Voice. Puedes responder ya lo hice.", TextToSpeech.QUEUE_FLUSH, null, "voice_test");
            else Toast.makeText(this, "La voz no está lista", Toast.LENGTH_SHORT).show();
        });
        root.addView(test, fullWidth());

        Button access = actionButton("⚙  ACCESO A NOTIFICACIONES", 0xB81C1D26, Color.WHITE);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access, fullWidth());

        Button automatic = actionButton("Escuchar respuesta: ACTIVADO", 0xB81C1D26, Color.WHITE);
        automatic.setOnClickListener(v -> {
            automaticVoice = !automaticVoice;
            automatic.setText("Escuchar respuesta: " + (automaticVoice ? "ACTIVADO" : "DESACTIVADO"));
            Toast.makeText(this, "La escucha automática necesita la pantalla abierta", Toast.LENGTH_LONG).show();
        });
        root.addView(automatic, fullWidth());

        TextView foot = label("Puedes decir: «Ya lo hice», «Listo» o «Terminado». " +
            "Con la pantalla bloqueada, utiliza los botones de la notificación. " +
            "La escucha automática funciona solo con esta pantalla abierta.", 13, 0xFFE6E2DD, false);
        foot.setPadding(dp(2), dp(18), dp(2), dp(6));
        root.addView(foot, fullWidth());

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 501);
        }
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && tts != null) {
                int lang = tts.setLanguage(new Locale("es", "ES"));
                ttsReady = lang != TextToSpeech.LANG_NOT_SUPPORTED && lang != TextToSpeech.LANG_MISSING_DATA;
                chooseSpanishVoice();
            }
        });
        reminderSpokenReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (visible) refreshPending();
                if (visible && automaticVoice && hasPendingReminder() && !listening) startVoiceWithPermission();
            }
        };
        IntentFilter filter = new IntentFilter(AlexaNotificationListener.ACTION_SPOKEN);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(reminderSpokenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(reminderSpokenReceiver, filter);
        if (getIntent().getBooleanExtra(AlexaNotificationListener.EXTRA_START_VOICE, false)) {
            root.post(this::startVoiceWithPermission);
        }
        musicCommandReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!visible) return;
                String search = intent.getStringExtra(MusicVoiceService.EXTRA_SEARCH);
                if (search != null && !search.isEmpty()) SpotifyMusic.searchSpotify(MainActivity.this, search);
            }
        };
        IntentFilter musicFilter = new IntentFilter(MusicVoiceService.ACTION_MUSIC_SEARCH);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(musicCommandReceiver, musicFilter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(musicCommandReceiver, musicFilter);
        refreshPending();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra(AlexaNotificationListener.EXTRA_START_VOICE, false)) startVoiceWithPermission();
    }

    @Override protected void onResume() { super.onResume(); visible = true; refreshPending(); }
    @Override protected void onPause() {
        visible = false;
        stopListening();
        super.onPause();
    }

    private boolean hasPendingReminder() {
        return !getSharedPreferences(AlexaNotificationListener.PREFS, MODE_PRIVATE)
            .getString(AlexaNotificationListener.KEY_TEXT, "").isEmpty();
    }

    private void refreshPending() {
        String pending = getSharedPreferences(AlexaNotificationListener.PREFS, MODE_PRIVATE)
                .getString(AlexaNotificationListener.KEY_TEXT, "");
        if (pendingTextView != null) pendingTextView.setText(pending.isEmpty() ? "Sin recordatorios pendientes" : pending);
        updateStatus(pending.isEmpty() ? "Todo al día. Esperando notificaciones de Alexa" : "Pendiente de confirmación");
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView label(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private GradientDrawable panel(int color, int radius, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), border);
        return d;
    }
    private Button actionButton(String text, int background, int foreground) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(foreground);
        button.setAllCaps(false);
        button.setBackground(panel(background, 17, 0x66FFFFFF));
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setMinHeight(dp(54));
        return button;
    }
    private void updateStatus(String text) { if (stateText != null) stateText.setText(text); }

    private void startVoiceWithPermission() {
        if (!hasPendingReminder()) {
            updateStatus("No hay un recordatorio pendiente");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST);
            return;
        }
        startVoiceRecognition();
    }

    private static final int MUSIC_AUDIO_REQUEST = 404;

    private void startMusicWithPermission() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MUSIC_AUDIO_REQUEST);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            musicStatus.setText("Necesitas activar el reconocimiento de voz de Android");
            return;
        }
        try {
            Intent service = new Intent(this, MusicVoiceService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            musicStatus.setText("Escucha activa. Di: Alexa Voice, pausa la música");
        } catch (Exception e) {
            musicStatus.setText("No se pudo iniciar la escucha: " + e.getClass().getSimpleName());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == MUSIC_AUDIO_REQUEST) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startMusicWithPermission();
            else if (musicStatus != null) musicStatus.setText("Necesitas permitir el micrófono para activar la escucha");
        }
        if (requestCode == AUDIO_REQUEST) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startVoiceRecognition();
            else updateStatus("Permite el microfono para responder por voz");
        }
    }

    private void startVoiceRecognition() {
        if (!visible || listening) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateStatus("El dispositivo necesita un servicio de reconocimiento de voz instalado");
            return;
        }
        stopListening();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { updateStatus("Te escucho. Di: Ya lo hice"); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int error) { stopListening(); updateStatus("No pude escucharte. Pulsa el microfono para probar otra vez"); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> candidates = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                stopListening();
                if (candidates == null || candidates.isEmpty()) { updateStatus("No entendi la respuesta"); return; }
                String spoken = normalize(candidates.get(0));
                if (spoken.contains("ya lo hice") || spoken.contains("ya esta hecho") || spoken.equals("hecho") ||
                    spoken.equals("listo") || spoken.equals("terminado") || spoken.contains("completado")) {
                    confirmReminder();
                } else if (spoken.contains("despues") || spoken.contains("mas tarde") || spoken.contains("repite")) {
                    updateStatus("De acuerdo. Volvere a recordartelo en 2 minutos");
                } else {
                    updateStatus("Escuche: " + candidates.get(0) + ". Pulsa el microfono para reintentar");
                }
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        listening = true;
        updateStatus("Activando microfono...");
        try { recognizer.startListening(intent); }
        catch (Exception ex) { stopListening(); updateStatus("No se pudo abrir el reconocimiento de voz"); }
    }

    private String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }

    private void confirmReminder() {
        stopListening();
        AlexaNotificationListener.confirm(this);
        refreshPending();
        updateStatus("Recordatorio confirmado. Repeticiones detenidas");
        if (ttsReady) tts.speak("Perfecto, recordatorio completado.", TextToSpeech.QUEUE_FLUSH, null, "confirmed");
    }

    private void stopListening() {
        listening = false;
        if (recognizer != null) {
            try { recognizer.cancel(); recognizer.destroy(); } catch (Exception ignored) { }
            recognizer = null;
        }
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 9, 0, 9);
        return p;
    }

    private void chooseSpanishVoice() {
        if (tts == null) return;
        try {
            for (Voice voice : tts.getVoices()) {
                String name = voice.getName().toLowerCase(Locale.ROOT);
                if (voice.getLocale().getLanguage().equals("es") &&
                    (name.contains("female") || name.contains("woman") || name.contains("femen"))) {
                    tts.setVoice(voice);
                    return;
                }
            }
        } catch (Exception ignored) { }
    }

    @Override protected void onDestroy() {
        stopListening();
        if (reminderSpokenReceiver != null) unregisterReceiver(reminderSpokenReceiver);
        if (musicCommandReceiver != null) unregisterReceiver(musicCommandReceiver);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
