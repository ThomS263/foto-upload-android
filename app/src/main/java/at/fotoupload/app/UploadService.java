package at.fotoupload.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class UploadService extends Service {
    private static final String CHANNEL = "foto_uploads";
    private static final int ACTIVE = 7301;

    @Override public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Foto-Uploads",
            NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Zeigt laufende und abgeschlossene Foto-Uploads an.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        boolean active = intent != null && intent.getBooleanExtra("active", false);
        int count = intent == null ? 0 : intent.getIntExtra("count", 0);
        if (active) {
            startForeground(ACTIVE, notification("Upload läuft", count == 1
                ? "Eine Datei wird gespeichert." : count + " Dateien werden gespeichert.", true));
            return START_STICKY;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        getSystemService(NotificationManager.class).notify(ACTIVE + 1,
            notification("Upload abgeschlossen", "Alle ausgewählten Dateien wurden gespeichert.", false));
        stopSelf();
        return START_NOT_STICKY;
    }

    private Notification notification(String title, String text, boolean ongoing) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title).setContentText(text).setContentIntent(pending)
            .setOngoing(ongoing).setAutoCancel(!ongoing).build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
