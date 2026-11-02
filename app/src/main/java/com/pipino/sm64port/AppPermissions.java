package com.pipino.sm64port;

/** Runtime permissions for the API 33 target; kept independent of Android for tests. */
final class AppPermissions {
    static final int STARTUP = 6402;
    static final String COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    static final String FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    static final String NEARBY_WIFI = "android.permission.NEARBY_WIFI_DEVICES";
    static final String NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    private AppPermissions() {}

    static String wifiPermission(int sdk) {
        return sdk >= 33 ? NEARBY_WIFI : FINE_LOCATION;
    }

    static String[] startupPermissions(int sdk) {
        if (sdk < 23) return new String[0];
        if (sdk >= 33) return new String[] { NEARBY_WIFI, NOTIFICATIONS };
        // Android 12 requires coarse and fine to be requested together.
        return new String[] { COARSE_LOCATION, FINE_LOCATION };
    }
}
