package com.rodneyhertz.hvactools;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity implements SensorEventListener {
    private SensorManager sm;
    private Sensor mag;
    private boolean want = false;
    private boolean registered = false;
    private boolean flushPending = false;
    private final StringBuilder sb = new StringBuilder();
    private final Handler h = new Handler(Looper.getMainLooper());

    /** Ponte direta com o JavaScript da página: AndroidMag.start() / stop() / has(). */
    public class MagBridge {
        @JavascriptInterface
        public boolean has() { return mag != null; }

        @JavascriptInterface
        public void start() { h.post(() -> { want = true; registerMag(); }); }

        @JavascriptInterface
        public void stop() { h.post(() -> { want = false; unregisterMag(); }); }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(0xFF121212);
        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(i.left, i.top, i.right, i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        getBridge().getWebView().addJavascriptInterface(new MagBridge(), "AndroidMag");
    }

    private void registerMag() {
        if (want && mag != null && !registered) {
            registered = sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_FASTEST, h);
        }
    }

    private void unregisterMag() {
        if (registered) sm.unregisterListener(this);
        registered = false;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        sb.append('[').append(e.values[0]).append(',').append(e.values[1]).append(',').append(e.values[2]).append("],");
        if (!flushPending) {
            flushPending = true;
            h.postDelayed(() -> {
                String js = "window.onNativeMag&&onNativeMag([" + sb + "])";
                sb.setLength(0);
                flushPending = false;
                getBridge().getWebView().evaluateJavascript(js, null);
            }, 30);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onPause() { super.onPause(); unregisterMag(); }

    @Override
    public void onResume() { super.onResume(); registerMag(); }
}
