package com.pipino.sm64port;

/** A quiet, finite adaptation of the Shindou 30 Hz motor events. */
final class RumbleEnvelope {
    private long nextEvent, nextContact;
    int duration, amplitude;

    boolean plan(int frames, int strength, boolean contact, long now) {
        if (frames <= 0 || strength <= 0 || now < nextEvent || (contact && now < nextContact)) return false;
        duration = contact ? 8 : Math.max(12, Math.min(55, frames * 5));
        amplitude = contact ? 42 : Math.max(40, Math.min(96, 32 + strength * 4 / 5));
        nextEvent = now + (contact ? 100 : 150);
        // A discrete impact also suppresses the sustained-contact tick.
        nextContact = now + 450;
        return true;
    }

    void reset() { nextEvent = nextContact = 0; }
}
