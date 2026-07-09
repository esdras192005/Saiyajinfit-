package com.esdras.saiyajinfit;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "RestNotification")
public class RestNotificationPlugin extends Plugin {

    private static final String CHANNEL_ID = "rest-progress";

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Progresso do descanso",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Barra de progresso do descanso entre séries");
                nm.createNotificationChannel(channel);
            }
        }
    }

    @PluginMethod
    public void update(PluginCall call) {
        ensureChannel();
        int id = call.getInt("id", 991200);
        String title = call.getString("title", "Descanso");
        String text = call.getString("text", "");
        int max = call.getInt("max", 100);
        int progress = call.getInt("progress", 0);
        Context ctx = getContext();

        Intent openIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, openIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(ctx.getApplicationInfo().icon)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            call.resolve();
            return;
        }
        NotificationManagerCompat.from(ctx).notify(id, builder.build());
        call.resolve();
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        NotificationManagerCompat.from(getContext()).cancel(call.getInt("id", 991200));
        call.resolve();
    }
}
