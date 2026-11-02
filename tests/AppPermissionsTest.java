package com.pipino.sm64port;

import java.util.Arrays;

final class AppPermissionsTest {
    private static void expect(int sdk, String... expected) {
        String[] actual = AppPermissions.startupPermissions(sdk);
        if (!Arrays.equals(actual, expected))
            throw new AssertionError("API " + sdk + ": " + Arrays.toString(actual));
        if (sdk >= 23 && !Arrays.asList(actual).contains(AppPermissions.wifiPermission(sdk)))
            throw new AssertionError("Required Wi-Fi permission not requested at startup");
    }

    public static void main(String[] args) {
        expect(21); expect(22);
        for (int sdk = 23; sdk <= 32; ++sdk)
            expect(sdk, AppPermissions.COARSE_LOCATION, AppPermissions.FINE_LOCATION);
        for (int sdk = 33; sdk <= 37; ++sdk)
            expect(sdk, AppPermissions.NEARBY_WIFI, AppPermissions.NOTIFICATIONS);
        System.out.println("Startup permissions: legacy location pair, nearby Wi-Fi and notifications PASS");
    }
}
