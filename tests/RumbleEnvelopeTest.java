package com.pipino.sm64port;
public final class RumbleEnvelopeTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        RumbleEnvelope rumble = new RumbleEnvelope();
        check(!rumble.plan(0, 80, false, 1000));
        check(rumble.plan(80, 80, false, 1000));
        check(rumble.duration <= 55 && rumble.amplitude <= 96);
        check(!rumble.plan(5, 80, false, 1100));
        check(!rumble.plan(1, 30, true, 1200));
        check(rumble.plan(1, 30, true, 1500));
        check(rumble.duration <= 8);
        for (long time = 1501; time < 1950; time += 16) check(!rumble.plan(1, 30, true, time));
        check(rumble.plan(1, 30, true, 1950));
        rumble.reset();
        check(rumble.plan(3, 50, false, 2000));
        check(rumble.duration >= 12 && rumble.duration <= 55);
        System.out.println("rumble: bounded impacts, quiet contact, cooldown and reset PASS");
    }
}
