package com.cardiag.protocol.can;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for CAN bus adapters.
 * Manages common state such as bitrate, open/closed status, and acceptance filters.
 * Subclasses must implement the actual I/O operations for their specific hardware.
 */
public abstract class CanAdapter implements CanBus {

    /** Default CAN bitrate: 500 kbps. */
    protected static final int DEFAULT_BITRATE = 500000;

    /** The configured CAN bus bitrate in bits per second. */
    protected int bitrate = DEFAULT_BITRATE;

    /** Whether the adapter is currently open. */
    protected boolean open = false;

    /** List of active acceptance filters as (id, mask) pairs. */
    protected final List<int[]> filters = new ArrayList<>();

    /**
     * Ensures the adapter is open before performing I/O.
     *
     * @throws IllegalStateException if the adapter is not open
     */
    protected void ensureOpen() {
        if (!open) {
            throw new IllegalStateException("CAN adapter is not open");
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void setBitrate(int bitrate) throws IOException {
        if (bitrate <= 0) {
            throw new IllegalArgumentException("Bitrate must be positive, got: " + bitrate);
        }
        this.bitrate = bitrate;
    }

    @Override
    public void addFilter(int id, int mask) throws IOException {
        filters.add(new int[]{id, mask});
    }

    @Override
    public void clearFilters() throws IOException {
        filters.clear();
    }

    /**
     * Returns the current bitrate.
     *
     * @return the bitrate in bits per second
     */
    public int getBitrate() {
        return bitrate;
    }

    /**
     * Tests whether a frame passes the currently configured acceptance filters.
     * If no filters are configured, all frames are accepted.
     *
     * @param frame the frame to test
     * @return {@code true} if the frame is accepted
     */
    protected boolean passesFilter(CanFrame frame) {
        if (filters.isEmpty()) {
            return true;
        }
        for (int[] filter : filters) {
            if (frame.matchesFilter(filter[0], filter[1])) {
                return true;
            }
        }
        return false;
    }
}
