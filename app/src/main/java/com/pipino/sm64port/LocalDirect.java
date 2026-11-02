package com.pipino.sm64port;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Handler;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/** Wi-Fi Direct discovery only advertises this game's service, never arbitrary peers.
 * Called on UI thread; the SDL thread reads immutable volatile snapshots only. */
final class LocalDirect {
    static final int PERMISSIONS = 6402;
    private static final String SERVICE = "sm64-local-v3";
    private final Activity activity;
    private final Handler ui = new Handler();
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private final LinkedHashMap<String, WifiP2pDevice> games = new LinkedHashMap<>();
    volatile String gamesSnapshot = "";
    private final ArrayList<WifiP2pDevice> gameRows = new ArrayList<>();
    private int scanRetries;
    private Runnable pendingScan;
    private boolean active, host, connecting, ownsGroup;
    private int generation, cleanupPending;
    volatile int status; // 0 waiting, 1 group ready, -1 failure
    volatile String address = "", detail = "WIFI DIRECT";

    LocalDirect(Activity activity) { this.activity = activity; }

    void start(boolean asHost) {
        stop();
        active = true;
        host = asHost;
        status = 0;
        detail = "ALLOW WIFI PERMISSION";
        // targetSdk 28 uses the legacy location permission even on Android 13+.
        // If the target is upgraded, select the new permission at runtime too.
        String permission = Build.VERSION.SDK_INT >= 33
                && activity.getApplicationInfo().targetSdkVersion >= 33
                ? "android.permission.NEARBY_WIFI_DEVICES" : Manifest.permission.ACCESS_FINE_LOCATION;
        if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[] { permission }, PERMISSIONS);
        } else begin();
    }

    void permissionResult(int[] results) {
        if (!active) return;
        if (results.length == 1 && results[0] == PackageManager.PERMISSION_GRANTED) begin();
        else fail("WIFI PERMISSION DENIED");
    }

    private WifiP2pManager.ActionListener listener(final int gen, final String error, final Runnable next) {
        return new WifiP2pManager.ActionListener() {
            public void onSuccess() { if (active && gen == generation && status >= 0 && next != null) next.run(); }
            public void onFailure(int reason) { if (active && gen == generation) fail(error + " " + reason); }
        };
    }

    private void fail(String text) { status = -1; detail = text; }

    private void begin() {
        if (!active) return;
        if (!activity.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
            fail("WIFI DIRECT NOT SUPPORTED"); return;
        }
        final int gen = generation;
        if (cleanupPending != 0) {
            ui.postDelayed(() -> { if (active && gen == generation) begin(); }, 100);
            return;
        }
        manager = (WifiP2pManager)activity.getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null) { fail("WIFI DIRECT UNAVAILABLE"); return; }
        channel = manager.initialize(activity, activity.getMainLooper(), () -> {
            if (active && gen == generation) fail("WIFI DIRECT CHANNEL LOST");
        });
        if (channel == null) { fail("WIFI DIRECT CHANNEL ERROR"); return; }
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (!active || gen != generation) return;
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    if (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) != WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                        fail("ENABLE WIFI IN SETTINGS");
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    NetworkInfo network = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (network != null && network.isConnected()) connectionInfo(gen);
                    else if (status == 1) fail("WIFI DIRECT DISCONNECTED");
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        activity.registerReceiver(receiver, filter);
        try {
            manager.requestGroupInfo(channel, group -> {
                if (!active || gen != generation) return;
                if (group != null) { fail("LEAVE EXISTING DIRECT GROUP"); return; }
                try { discover(gen); } catch (SecurityException exception) { fail("WIFI PERMISSION REQUIRED"); }
            });
        } catch (SecurityException exception) { fail("WIFI PERMISSION REQUIRED"); }
        ui.postDelayed(() -> {
            if (active && gen == generation && status == 0) fail("DIRECT TIMEOUT  TRY AGAIN");
        }, 120000);
    }

    private void discover(final int gen) {
        detail = host ? "CREATING DIRECT GROUP" : "ENABLE LOCATION AND WIFI";
        if (host) {
            HashMap<String, String> info = new HashMap<>();
            info.put("protocol", "3");
            WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(SERVICE, "_sm64._udp", info);
            manager.addLocalService(channel, service, listener(gen, "ADVERTISE ERROR", () -> {
                ownsGroup = true; // include pending creation in cancellation cleanup
                manager.createGroup(channel, listener(gen, "CREATE GROUP ERROR", () -> {
                    ownsGroup = true;
                    connectionInfo(gen);
                }));
            }));
        } else {
            manager.setDnsSdResponseListeners(channel, (instance, type, device) -> {
                if (!active || gen != generation || host || connecting || status != 0 || !SERVICE.equals(instance)) return;
                if (!games.containsKey(device.deviceAddress) && games.size() >= 8) return;
                games.put(device.deviceAddress, device);
                gameRows.clear(); gameRows.addAll(games.values());
                StringBuilder names = new StringBuilder();
                for (WifiP2pDevice item : gameRows) {
                    String name = item.deviceName == null ? "Host SM64" : item.deviceName.replaceAll("[^A-Za-z0-9 _-]", "").trim();
                    if (name.isEmpty()) name = "Host SM64";
                    if (names.length() > 0) names.append('\n');
                    names.append(name.substring(0, Math.min(20, name.length())));
                }
                gamesSnapshot = names.toString();
                detail = "CHOOSE NEARBY HOST";
            }, (domain, record, device) -> {});
            manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(),
                    listener(gen, "SERVICE REQUEST ERROR", () -> scan(gen)));
        }
    }

    void selectGame(int index) {
        if (!active || host || connecting || status != 0 || index < 0 || index >= gameRows.size()) return;
        final int gen = generation;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = gameRows.get(index).deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        connecting = ownsGroup = true;
        gamesSnapshot = "";
        detail = "ACCEPT ON HOST PHONE";
        try {
            manager.stopPeerDiscovery(channel, null);
            manager.connect(channel, config, listener(gen, "DIRECT CONNECT ERROR", null));
        } catch (SecurityException exception) { fail("WIFI PERMISSION REQUIRED"); }
    }

    private void scan(final int gen) {
        if (!active || gen != generation || connecting || status != 0) return;
        try {
            manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                public void onSuccess() { scanRetries = 0; }
                public void onFailure(int reason) {
                    if (!active || gen != generation || connecting || status != 0) return;
                    // Android briefly reports BUSY while prior discovery stops.
                    if (reason != WifiP2pManager.BUSY || ++scanRetries > 5) fail("DISCOVERY ERROR " + reason);
                }
            });
            if (pendingScan != null) ui.removeCallbacks(pendingScan);
            pendingScan = () -> scan(gen);
            ui.postDelayed(pendingScan, 8000);
        } catch (SecurityException exception) { fail("WIFI PERMISSION REQUIRED"); }
    }

    private void connectionInfo(final int gen) {
        try {
            manager.requestConnectionInfo(channel, info -> {
                if (!active || gen != generation || status < 0 || !info.groupFormed || info.groupOwnerAddress == null) return;
                if (info.isGroupOwner != host) { fail("DIRECT ROLE MISMATCH"); return; }
                address = info.groupOwnerAddress.getHostAddress();
                detail = host ? "WAITING FOR PLAYER TWO" : "DIRECT GROUP CONNECTED";
                status = 1;
            });
        } catch (SecurityException exception) { fail("WIFI PERMISSION REQUIRED"); }
    }

    void stop() {
        active = false;
        ++generation;
        ui.removeCallbacksAndMessages(null);
        pendingScan = null;
        games.clear(); gameRows.clear(); gamesSnapshot = ""; scanRetries = 0;
        if (receiver != null) { activity.unregisterReceiver(receiver); receiver = null; }
        if (manager != null && channel != null) {
            try {
                manager.stopPeerDiscovery(channel, null);
                manager.clearServiceRequests(channel, null);
                manager.clearLocalServices(channel, null);
                if (connecting) manager.cancelConnect(channel, null);
                if (ownsGroup || connecting) {
                    final WifiP2pManager.Channel oldChannel = channel;
                    ++cleanupPending;
                    WifiP2pManager.ActionListener cleanup = new WifiP2pManager.ActionListener() {
                        private boolean finished;
                        private void finish() {
                            if (finished) return;
                            finished = true;
                            --cleanupPending;
                            if (Build.VERSION.SDK_INT >= 27) oldChannel.close();
                        }
                        public void onSuccess() { finish(); }
                        public void onFailure(int reason) { finish(); }
                    };
                    // Framework callbacks can be lost after a channel closes.
                    // A separate handler survives stop() clearing discovery work.
                    new Handler(activity.getMainLooper()).postDelayed(() -> cleanup.onFailure(0), 3000);
                    try { manager.removeGroup(channel, cleanup); }
                    catch (SecurityException exception) { cleanup.onFailure(0); }
                } else if (Build.VERSION.SDK_INT >= 27) channel.close();
            } catch (SecurityException ignored) {
                if (Build.VERSION.SDK_INT >= 27) channel.close();
            }
        }
        manager = null; channel = null;
        connecting = ownsGroup = false;
        status = 0; address = ""; detail = "WIFI DIRECT";
    }
}
