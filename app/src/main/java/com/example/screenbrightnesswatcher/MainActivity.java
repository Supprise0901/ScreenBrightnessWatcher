package com.example.screenbrightnesswatcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "BrightnessWatcher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent intent = new Intent(this, BrightnessService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Log.i(TAG, "已请求启动 BrightnessService");
        } catch (Exception e) {
            Log.e(TAG, "启动 BrightnessService 失败", e);
        }

        finish();
    }
}
