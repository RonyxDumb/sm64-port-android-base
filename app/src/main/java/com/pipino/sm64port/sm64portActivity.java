package com.pipino.sm64port;

import org.libsdl.app.SDLActivity;

public class sm64portActivity extends SDLActivity
{
    private LocalDirect direct;
    private LocalMenu localMenu;
    private GameHaptics haptics;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        direct = new LocalDirect(this);
        haptics = new GameHaptics(this);
        localMenu = new LocalMenu(this, mLayout);
    }

    public int updateLocalMenu(int page, int selected, int status, int ready, String detail) {
        return localMenu == null ? 0 : localMenu.update(page, selected, status, ready, detail);
    }

    public int rumbleEnabled() { return haptics != null && haptics.enabled ? 1 : 0; }
    public void setRumbleEnabled(final int enabled) {
        runOnUiThread(() -> {
            if (haptics == null) return;
            haptics.setEnabled(enabled != 0);
            if (enabled != 0) haptics.play(6, 65, false);
        });
    }
    public String localDirectGames() { return direct == null ? "" : direct.gamesSnapshot; }
    public void selectLocalDirectGame(final int index) {
        runOnUiThread(() -> { if (direct != null) direct.selectGame(index); });
    }

    public void rumbleEvent(final int frames, final int strength, final int contact) {
        runOnUiThread(() -> { if (haptics != null) haptics.play(frames, strength, contact != 0); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (haptics != null) haptics.setForeground(true);
    }

    @Override
    protected void onPause() {
        if (haptics != null) haptics.setForeground(false);
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        android.view.InputDevice device = event.getDevice();
        if (haptics != null && device != null &&
                (device.getSources() & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD)
            haptics.useDevice(device.getId());
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        if (haptics != null && (event.getSource() & android.view.InputDevice.SOURCE_JOYSTICK)
                == android.view.InputDevice.SOURCE_JOYSTICK) haptics.useDevice(event.getDeviceId());
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (haptics != null && event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) haptics.useDevice(-1);
        return super.dispatchTouchEvent(event);
    }

    // Called by SDL's native game thread. Android operations stay on the UI thread.
    public int startLocalDirect(final int host) {
        runOnUiThread(() -> { if (direct != null) direct.start(host != 0); });
        return 0;
    }
    public int stopLocalDirect() {
        runOnUiThread(() -> { if (direct != null) direct.stop(); });
        return 0;
    }
    public int localDirectStatus() { return direct == null ? 0 : direct.status; }
    public String localDirectAddress() { return direct == null ? "" : direct.address; }
    public String localDirectDetail() { return direct == null ? "INITIALIZING DIRECT" : direct.detail; }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == LocalDirect.PERMISSIONS && direct != null) direct.permissionResult(results);
    }

    @Override
    protected void onDestroy() {
        if (haptics != null) haptics.stop();
        if (direct != null) direct.stop();
        super.onDestroy();
    }

    @Override
    protected String getMainFunction() {
        return "main";
    }
}
