package com.pipino.sm64port;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.InputDevice;

/** Runs on the UI thread. Never queues a repeating or unbounded vibration. */
final class GameHaptics {
    private final SharedPreferences preferences;
    private final Vibrator phone;
    private final RumbleEnvelope envelope = new RumbleEnvelope();
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    private Vibrator playing;
    private int controller = -1;
    private boolean foreground;
    volatile boolean enabled;

    GameHaptics(Context context) {
        preferences = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE);
        enabled = preferences.getBoolean("rumble", true);
        phone = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    void setEnabled(boolean value) {
        enabled = value;
        preferences.edit().putBoolean("rumble", value).apply();
        if (!value) stop();
    }

    void setForeground(boolean value) { foreground = value; if (!value) stop(); }
    void useDevice(int device) { controller = device; }

    void play(int frames, int strength, boolean contact) {
        if (frames <= 0) { stop(); return; }
        if (!enabled || !foreground || !envelope.plan(frames, strength, contact, SystemClock.uptimeMillis())) return;
        Vibrator target = phone;
        InputDevice device = InputDevice.getDevice(controller);
        if (device != null && device.getVibrator().hasVibrator()) target = device.getVibrator();
        if (target == null || !target.hasVibrator()) return;
        try {
            if (playing != null && playing != target) playing.cancel();
            playing = target;
            if (Build.VERSION.SDK_INT >= 26 && target.hasAmplitudeControl()) {
                int a = envelope.amplitude;
                target.vibrate(VibrationEffect.createWaveform(
                        new long[] { 0, 3, Math.max(2, envelope.duration - 6), 3 },
                        new int[] { 0, a * 3 / 4, a, a / 2 }, -1), attributes);
            } else {
                // Motors without amplitude control receive a short tap.
                target.vibrate(Math.min(envelope.duration, 24), attributes);
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
            // A missing/removed controller must never interrupt gameplay.
        }
    }

    void stop() {
        if (playing != null) {
            try { playing.cancel(); } catch (SecurityException ignored) {}
            playing = null;
        }
        envelope.reset();
    }
}
