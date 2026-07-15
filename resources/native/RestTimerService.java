package com.esdras.saiyajinfit;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class RestTimerService extends Service {

    public static final String CHANNEL_ID = "rest-progress";
    public static final int NOTIF_ID = 991200;

    private Handler handler;
    private Runnable tickRunnable;
    private long endTimeMillis;
    private int totalSeconds;
    private String title;

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Progresso do descanso", NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Barra de progresso do descanso entre séries");
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopTimer();
            return START_NOT_STICKY;
        }

        totalSeconds = intent.getIntExtra("seconds", 90);
        title = intent.getStringExtra("title");
        if (title == null) title = "Descanso";
        endTimeMillis = System.currentTimeMillis() + (totalSeconds * 1000L);

        startForeground(NOTIF_ID, buildNotification(totalSeconds, totalSeconds));
        scheduleTick();
        return START_STICKY;
    }

    private void scheduleTick() {
        if (tickRunnable != null) handler.removeCallbacks(tickRunnable);
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingMs = endTimeMillis - System.currentTimeMillis();
                int remainingSec = (int) Math.max(0, remainingMs / 1000);

                NotificationManagerCompat.from(RestTimerService.this)
                    .notify(NOTIF_ID, buildNotification(totalSeconds, remainingSec));

                if (remainingSec <= 0) {
                    finishTimer();
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(tickRunnable);
    }

    private NotificationCompat.Builder baseBuilder() {
        Intent openIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(getApplicationInfo().icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private android.app.Notification buildNotification(int max, int remaining) {
        int elapsed = max - remaining;
        String text = formatTime(remaining) + " restantes";
        return baseBuilder()
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(max, elapsed, false)
            .build();
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private void finishTimer() {
        NotificationManagerCompat.from(this).notify(NOTIF_ID, baseBuilder()
            .setContentTitle(title)
            .setContentText("Hora de voltar ao treino \uD83D\uDCAA")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .build());
        stopForeground(Service.STOP_FOREGROUND_DETACH);
        stopSelf();
    }

    private void stopTimer() {
        if (handler != null && tickRunnable != null) handler.removeCallbacks(tickRunnable);
        NotificationManagerCompat.from(this).cancel(NOTIF_ID);
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (handler != null && tickRunnable != null) handler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
