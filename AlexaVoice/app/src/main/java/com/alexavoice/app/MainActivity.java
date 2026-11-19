package com.alexavoice.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private TextView currentView;
    private TextView countView;
    private int dp(int px){return (int)(getResources().getDisplayMetrics().density*px+.5f);}
    private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));return d;}
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);return t;}
    private Button button(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTextSize(15);b.setBackground(bg(color));b.setMinHeight(dp(56));return b;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.BLACK);
        ImageView wallpaper=new ImageView(this);wallpaper.setImageResource(R.drawable.anime_background);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        View overlay=new View(this);
        overlay.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xAE000000,0x44000000,0xEF080B15}));
        frame.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(32),dp(22),dp(24)); scroll.addView(root);
        frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));setContentView(frame);
        TextView caption=label("AVENTURA • RECORDATORIOS",12,0xFFFFC979); root.addView(caption);
        TextView title=label("Alexa Voice",34,Color.WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title);
        root.addView(label("Recordatorios hablados, incluso con pantalla bloqueada*",14,Color.WHITE));
        root.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(120)));
        LinearLayout card=new LinearLayout(this);card.setPadding(dp(18),dp(18),dp(18),dp(18));card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(bg(0xE3212630));root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        countView=label("RECORDATORIOS",13,0xFFFFCF85);card.addView(countView);
        currentView=label("Sin recordatorios pendientes",22,Color.WHITE);currentView.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        card.addView(currentView);
        card.addView(label("Reintentos cada 2 minutos mientras Android lo permita.",13,0xFFF2D7AA));
        Button done=button("✓  YA LO HICE",0xFFB5751E);root.addView(done,new LinearLayout.LayoutParams(-1,dp(62)));
        done.setOnClickListener(v->{if(ReminderEngine.count(this)>0){ReminderEngine.confirmCurrent(this);refresh();Toast.makeText(this,"Recordatorio confirmado",Toast.LENGTH_SHORT).show();}});
        Button settings=button("⚙  ACCESO A NOTIFICACIONES",0xFF294966);
        LinearLayout.LayoutParams margins=new LinearLayout.LayoutParams(-1,dp(62));margins.topMargin=dp(12);root.addView(settings,margins);
        settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        Button test=button("🔊  PROBAR CON PANTALLA APAGADA",0xFF21665E);
        LinearLayout.LayoutParams testMargins=new LinearLayout.LayoutParams(-1,dp(62));
        testMargins.topMargin=dp(12);root.addView(test,testMargins);
        test.setOnClickListener(v->scheduleScreenOffTest());
        Button preview=button("↻  ACTUALIZAR ESTADO",0xFF343D4E);
        LinearLayout.LayoutParams margins2=new LinearLayout.LayoutParams(-1,dp(62));margins2.topMargin=dp(12);root.addView(preview,margins2);
        preview.setOnClickListener(v->refresh());
        TextView note=label("*Android y el ahorro de batería de HONOR pueden retrasar la lectura con pantalla apagada. " +
            "El acceso a notificaciones es necesario para detectar los avisos de Amazon Alexa. " +
            "La prueba de pantalla apagada se reproduce una vez después de unos 20 segundos; " +
            "no añade recordatorios. Esta app no utiliza micrófono, Spotify ni conexión a internet.",13,0xFFE9DDC9);
        note.setPadding(0,dp(18),0,0);root.addView(note);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},501);
        refresh();
    }
    private void scheduleScreenOffTest() {
        AlarmManager manager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(manager==null){Toast.makeText(this,"No se pudo programar la prueba",Toast.LENGTH_LONG).show();return;}
        Intent intent=new Intent(this,ScreenOffTestReceiver.class).setAction(ScreenOffTestReceiver.ACTION_TEST);
        PendingIntent pending=PendingIntent.getBroadcast(this,72,intent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(pending);
        long when=SystemClock.elapsedRealtime()+20000L;
        if(Build.VERSION.SDK_INT>=23)
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pending);
        else manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,pending);
        Toast.makeText(this,"Prueba programada. Apaga la pantalla ahora. Sonará en unos 20 segundos (Android puede retrasarla).",Toast.LENGTH_LONG).show();
    }
    @Override protected void onResume(){super.onResume();refresh();}
    private void refresh(){
        int n=ReminderEngine.count(this);
        if(currentView!=null)currentView.setText(n==0?"Sin recordatorios pendientes":ReminderEngine.current(this));
        if(countView!=null)countView.setText(n==0?"TODO AL DÍA":n+" PENDIENTE(S) — PRIMERO EN LA COLA");
    }
}
