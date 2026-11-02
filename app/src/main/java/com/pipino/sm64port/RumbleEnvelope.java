package com.pipino.sm64port;

/** A finite adaptation of the Shindou 30 Hz motor events. */
final class RumbleEnvelope {
    private long nextEvent, nextContact;
    int duration, amplitude;

    boolean plan(int frames, int strength, boolean contact, long now) {
        if (frames <= 0 || strength <= 0 || now < nextEvent || (contact && now < nextContact)) return false;
        duration = contact ? 16 : Math.max(24, Math.min(100, frames * 9));
        amplitude = contact ? 90 : Math.max(100, Math.min(200, 60 + strength * 7 / 4));
        nextEvent = now + (contact ? 100 : 150);
        // A discrete impact also suppresses the sustained-contact tick.
        nextContact = now + 450;
        return true;
    }

    void reset() { nextEvent = nextContact = 0; }
}
