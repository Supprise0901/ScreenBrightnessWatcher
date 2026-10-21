package com.example.screenbrightnesswatcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BrightnessService extends Service {
    private static final String TAG = "BrightnessWatcher";
    private static final String CHANNEL_ID = "brightness_watcher";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL_MS = 5000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            executor.execute(() -> runShellCommand());
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        running = true;
        handler.post(pollTask);
        Log.i(TAG, "Service started; polling every 5 seconds.");
    }

    private void runShellCommand() {
        Process process = null;
        try {
            process = new ProcessBuilder(
                    "/system/bin/sh", "-c",
                    "settings get system screen_brightness"
            ).redirectErrorStream(true).start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            Log.i(TAG, "command exit=" + exitCode + ", output=" + output);
        } catch (Exception e) {
            Log.e(TAG, "Shell command failed", e);
        } finally {
            if (process != null) process.destroy();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background brightness monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the 5-second brightness check running.");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Brightness monitor running")
                .setContentText("Checks system screen brightness every 5 seconds")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            handler.post(pollTask);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(pollTask);
        executor.shutdownNow();
        Log.i(TAG, "Service destroyed.");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
