package com.pipino.sm64port;

import org.libsdl.app.SDLActivity;

public class sm64portActivity extends SDLActivity
{
    private LocalDirect direct;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        direct = new LocalDirect(this);
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
        if (direct != null) direct.stop();
        super.onDestroy();
    }

    @Override
    protected String getMainFunction() {
        return "main";
    }
}
