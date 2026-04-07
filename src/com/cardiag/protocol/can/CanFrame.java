package com.cardiag.protocol.can;

import java.util.Arrays;

/**
 * Represents a single CAN (Controller Area Network) frame.
 * Supports both standard (11-bit) and extended (29-bit) identifiers
 * as well as remote transmission request (RTR) frames.
 */
public class CanFrame {

    /** Maximum data length for a classical CAN frame. */
    public static final int MAX_DLC = 8;

    /** Maximum standard CAN identifier (11 bits). */
    public static final int MAX_STANDARD_ID = 0x7FF;

    /** Maximum extended CAN identifier (29 bits). */
    public static final int MAX_EXTENDED_ID = 0x1FFFFFFF;

    private final int id;
    private final byte[] data;
    private final int dlc;
    private final boolean extended;
    private final boolean rtr;

    /**
     * Constructs a CAN frame.
     *
     * @param id       the CAN identifier
     * @param data     the payload data (up to 8 bytes); may be {@code null} for RTR frames
     * @param dlc      the data length code (0-8)
     * @param extended {@code true} for a 29-bit extended identifier
     * @param rtr      {@code true} for a remote transmission request frame
     * @throws IllegalArgumentException if parameters are out of range
     */
    public CanFrame(int id, byte[] data, int dlc, boolean extended, boolean rtr) {
        if (dlc < 0 || dlc > MAX_DLC) {
            throw new IllegalArgumentException("DLC must be 0-8, got: " + dlc);
        }
        int maxId = extended ? MAX_EXTENDED_ID : MAX_STANDARD_ID;
        if (id < 0 || id > maxId) {
            throw new IllegalArgumentException(
                    "ID 0x" + Integer.toHexString(id).toUpperCase()
                            + " out of range for " + (extended ? "extended" : "standard") + " frame");
        }

        this.id = id;
        this.dlc = dlc;
        this.extended = extended;
        this.rtr = rtr;

        if (data != null) {
            this.data = Arrays.copyOf(data, Math.min(data.length, MAX_DLC));
        } else {
            this.data = new byte[0];
        }
    }

    /**
     * Convenience constructor for a standard data frame.
     *
     * @param id   the 11-bit CAN identifier
     * @param data the payload data
     */
    public CanFrame(int id, byte[] data) {
        this(id, data, data != null ? Math.min(data.length, MAX_DLC) : 0, false, false);
    }

    /**
     * Creates a standard data frame.
     *
     * @param id   the CAN identifier
     * @param data the payload bytes
     * @return a new CAN frame
     */
    public static CanFrame standard(int id, byte[] data) {
        return new CanFrame(id, data, data.length, false, false);
    }

    /**
     * Creates an extended data frame.
     *
     * @param id   the 29-bit CAN identifier
     * @param data the payload bytes
     * @return a new CAN frame
     */
    public static CanFrame extended(int id, byte[] data) {
        return new CanFrame(id, data, data.length, true, false);
    }

    /**
     * Creates a remote transmission request frame.
     *
     * @param id       the CAN identifier
     * @param dlc      the requested data length
     * @param extended whether to use a 29-bit identifier
     * @return a new RTR frame
     */
    public static CanFrame rtr(int id, int dlc, boolean extended) {
        return new CanFrame(id, null, dlc, extended, true);
    }

    /**
     * Returns the CAN identifier.
     *
     * @return the frame ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns a copy of the frame data.
     *
     * @return the payload bytes
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Returns the data byte at the specified index.
     *
     * @param index the byte index (0-based)
     * @return the data byte
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     */
    public byte getDataByte(int index) {
        return data[index];
    }

    /**
     * Returns the data length code.
     *
     * @return the DLC (0-8)
     */
    public int getDlc() {
        return dlc;
    }

    /**
     * Returns whether this frame uses an extended (29-bit) identifier.
     *
     * @return {@code true} if extended
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * Returns whether this is a remote transmission request frame.
     *
     * @return {@code true} if RTR
     */
    public boolean isRtr() {
        return rtr;
    }

    /**
     * Returns the actual number of data bytes in this frame.
     *
     * @return the data length
     */
    public int getDataLength() {
        return data.length;
    }

    /**
     * Checks whether this frame's ID matches the given ID and mask.
     *
     * @param filterId   the filter identifier
     * @param filterMask the filter mask (1-bits must match)
     * @return {@code true} if the frame passes the filter
     */
    public boolean matchesFilter(int filterId, int filterMask) {
        return (id & filterMask) == (filterId & filterMask);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(extended ? "X" : "S");
        sb.append(rtr ? "R" : "D");
        sb.append(String.format(" %08X", id));
        sb.append(String.format(" [%d]", dlc));
        if (!rtr && data.length > 0) {
            sb.append(" ");
            for (int i = 0; i < data.length; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(String.format("%02X", data[i] & 0xFF));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CanFrame)) return false;
        CanFrame other = (CanFrame) obj;
        return id == other.id
                && dlc == other.dlc
                && extended == other.extended
                && rtr == other.rtr
                && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(id);
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + Integer.hashCode(dlc);
        result = 31 * result + Boolean.hashCode(extended);
        result = 31 * result + Boolean.hashCode(rtr);
        return result;
    }
}
