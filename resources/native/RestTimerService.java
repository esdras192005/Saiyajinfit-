package com.esdras.saiyajinfit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class RestTimerService extends Service {

    private static final String CHANNEL_ID = "rest_timer_channel_v2";
    private static final int RUNNING_NOTIFICATION_ID = 1001;
    private static final int FINISHED_NOTIFICATION_ID = 1002;

    private CountDownTimer countDownTimer;

    public static final String EXTRA_DURATION_MS = "duration_ms";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long durationMs = 60000L;
        if (intent != null) {
            durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 60000L);
        }

        startForeground(RUNNING_NOTIFICATION_ID, buildRunningNotification(durationMs));

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                NotificationManagerCompat.from(RestTimerService.this)
                    .notify(RUNNING_NOTIFICATION_ID, buildRunningNotification(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                showTimerFinishedNotification();
                stopForeground(true);
                stopSelf();
            }
        }.start();

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Timer de Descanso",
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificações de fim de descanso");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 200, 500});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        channel.setSound(soundUri, audioAttributes);

        manager.createNotificationChannel(channel);
    }

    private Notification buildRunningNotification(long millisRemaining) {
        long secondsRemaining = millisRemaining / 1000;
        String timeText = String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Descansando...")
            .setContentText(timeText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        return builder.build();
    }

    private void showTimerFinishedNotification() {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Descanso finalizado!")
            .setContentText("Hora de voltar pro treino 💪")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(new long[]{0, 500, 200, 500})
            .setOnlyAlertOnce(false)
            .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(FINISHED_NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
