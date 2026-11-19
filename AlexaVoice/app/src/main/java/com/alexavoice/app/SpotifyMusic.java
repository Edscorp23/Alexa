package com.alexavoice.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.widget.Toast;
import java.util.List;

/** Spotify stays in charge of playback. Search intents do not automatically play tracks. */
public final class SpotifyMusic {
    private SpotifyMusic() { }
    static void openSpotify(Activity activity) {
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (intent != null) activity.startActivity(intent);
        else Toast.makeText(activity, "Instala Spotify para usar esta función", Toast.LENGTH_LONG).show();
    }
    static void searchSpotify(Activity activity, String text) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(text)));
            i.setPackage("com.spotify.music");
            activity.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(activity, "Abre Spotify e introduce: " + text, Toast.LENGTH_LONG).show();
        }
    }
    /** Returns false when Spotify doesn't have an active Android media session. */
    static boolean transport(Context context, String command) {
        try {
            MediaSessionManager manager = (MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            ComponentName listener = new ComponentName(context, AlexaNotificationListener.class);
            List<MediaController> sessions = manager.getActiveSessions(listener);
            if (sessions == null) return false;
            for (MediaController controller : sessions) {
                if (!"com.spotify.music".equals(controller.getPackageName())) continue;
                MediaController.TransportControls controls = controller.getTransportControls();
                if ("pause".equals(command)) controls.pause();
                else if ("play".equals(command)) controls.play();
                else if ("next".equals(command)) controls.skipToNext();
                else if ("previous".equals(command)) controls.skipToPrevious();
                else return false;
                return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }
}
