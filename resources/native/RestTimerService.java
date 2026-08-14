package com.esdras.saiyajinfit;

import android.app.Notification;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class RestTimerService extends Service {

    // Mesmo canal já criado pelo RestNotificationPlugin (ensureChannel),
    // pra não duplicar canais e manter tudo consistente.
    private static final String CHANNEL_ID = "rest-progress";
    private static final int NOTIFICATION_ID = 991200; // igual ao REST_PROGRESS_NOTIF_ID do JS

    private Handler handler;
    private Runnable tickRunnable;
    private long endTimeMillis;
    private int totalSeconds;
    private String title = "⏱ Repouso";

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

        totalSeconds = intent != null ? intent.getIntExtra("seconds", 90) : 90;
        String incomingTitle = intent != null ? intent.getStringExtra("title") : null;
        title = incomingTitle != null ? incomingTitle : "⏱ Repouso";

        // Fonte da verdade: relógio real (System.currentTimeMillis()), igual ao
        // Date.now() usado no JS (restEndTime). É isso que garante que a
        // notificação e a tela do app nunca fiquem dessincronizadas.
        endTimeMillis = System.currentTimeMillis() + (totalSeconds * 1000L);

        startForeground(NOTIFICATION_ID, buildNotification(totalSeconds));
        startTicking();

        return START_STICKY;
    }

    private void startTicking() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingMs = endTimeMillis - System.currentTimeMillis();
                int remainingSec = (int) Math.max(0, Math.round(remainingMs / 1000.0));

                if (remainingSec <= 0) {
                    stopTimer();
                    return;
                }

                NotificationManagerCompat.from(RestTimerService.this)
                    .notify(NOTIFICATION_ID, buildNotification(remainingSec));

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(tickRunnable);
    }

    private void stopTimer() {
        if (handler != null && tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification(int remainingSeconds) {
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        String txt = m > 0 ? String.format("%d:%02d", m, s) : s + "s";
        int elapsed = Math.max(0, totalSeconds - remainingSeconds);

        Context ctx = getApplicationContext();
        Intent openIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentIntent = openIntent != null
            ? PendingIntent.getActivity(ctx, 0, openIntent, piFlags)
            : null;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(ctx.getApplicationInfo().icon)
            .setContentTitle(title + " " + txt)
            .setContentText("Hora de voltar ao treino 💪")
            .setProgress(totalSeconds, elapsed, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW);

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }

        return builder.build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Progresso do descanso",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Barra de progresso do descanso entre séries");
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
