package com.rodneyhertz.hvactools;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Lê o magnetômetro nativo (µT) na taxa máxima do aparelho e envia x, y, z ao JavaScript. */
@CapacitorPlugin(name = "Magnet")
public class MagnetPlugin extends Plugin implements SensorEventListener {
    private SensorManager sm;
    private Sensor mag;
    private boolean running = false;

    @PluginMethod
    public void start(PluginCall call) {
        sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mag == null) {
            call.reject("Aparelho sem magnetômetro");
            return;
        }
        register();
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        unregister();
        call.resolve();
    }

    private void register() {
        if (sm != null && mag != null && !running) {
            running = sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void unregister() {
        if (sm != null && running) sm.unregisterListener(this);
        running = false;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        JSObject o = new JSObject();
        o.put("x", (double) e.values[0]);
        o.put("y", (double) e.values[1]);
        o.put("z", (double) e.values[2]);
        notifyListeners("reading", o);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void handleOnPause() { boolean was = running; unregister(); wasRunning = was; }

    @Override
    protected void handleOnResume() { if (wasRunning) register(); }

    private boolean wasRunning = false;
}
