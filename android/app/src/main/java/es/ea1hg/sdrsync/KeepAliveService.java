package es.ea1hg.sdrsync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/** Mantiene la app viva con la pantalla apagada (aviso fijo en la barra) para que el audio no se corte. */
public class KeepAliveService extends Service {
    private static final String CHANNEL = "keepalive";
    private PowerManager.WakeLock cpu;
    private WifiManager.WifiLock wifi;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Conexión con el PC", NotificationManager.IMPORTANCE_LOW));

        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent exit = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_EXIT, true);
        PendingIntent pExit = PendingIntent.getActivity(this, 1, exit, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle("EA1HG SDR")
                .setContentText("Conectado con el PC")
                .setContentIntent(pOpen)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Salir", pExit).build())
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
        acquireLocks();
        return START_STICKY;
    }

    private void acquireLocks() {
        try {
            if (cpu == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ea1hg:cpu");
                cpu.acquire();
            }
            if (wifi == null) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifi = wm.createWifiLock(Build.VERSION.SDK_INT >= 29 ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                        : WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ea1hg:wifi");
                wifi.acquire();
            }
        } catch (RuntimeException ignored) {
            // sin permiso o sin Wi-Fi: no pasa nada
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (cpu != null && cpu.isHeld()) {
                cpu.release();
            }
            if (wifi != null && wifi.isHeld()) {
                wifi.release();
            }
        } catch (RuntimeException ignored) {
            // ya liberados
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
