package com.labscraft.quest;

import java.util.function.LongSupplier;

/** Mutable test clock (milliseconds). */
final class FakeClock implements LongSupplier {
    private long nowMs;

    FakeClock(long startMs) {
        this.nowMs = startMs;
    }

    void advanceSeconds(long seconds) {
        nowMs += seconds * 1000L;
    }

    void advanceMillis(long millis) {
        nowMs += millis;
    }

    @Override
    public long getAsLong() {
        return nowMs;
    }
}
