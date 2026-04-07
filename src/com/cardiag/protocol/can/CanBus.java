package com.cardiag.protocol.can;

import java.io.IOException;

/**
 * Interface for CAN bus communication.
 * Implementations provide the physical transport layer for sending and receiving
 * CAN frames through various hardware adapters.
 */
public interface CanBus {

    /**
     * Sends a CAN frame on the bus.
     *
     * @param frame the frame to send
     * @throws IOException if the frame cannot be sent
     * @throws IllegalStateException if the bus is not open
     */
    void sendFrame(CanFrame frame) throws IOException;

    /**
     * Receives the next CAN frame from the bus, blocking up to the specified timeout.
     *
     * @param timeoutMs maximum time to wait in milliseconds
     * @return the received frame, or {@code null} if the timeout expires
     * @throws IOException if a communication error occurs
     * @throws IllegalStateException if the bus is not open
     */
    CanFrame receiveFrame(int timeoutMs) throws IOException;

    /**
     * Adds a hardware acceptance filter. Only frames matching at least one
     * registered filter will be received. If no filters are set, all frames
     * are accepted.
     *
     * @param id   the filter identifier
     * @param mask the filter mask (bits set to 1 must match)
     * @throws IOException if the filter cannot be configured
     */
    void addFilter(int id, int mask) throws IOException;

    /**
     * Removes all acceptance filters, reverting to accepting all frames.
     *
     * @throws IOException if the filters cannot be cleared
     */
    void clearFilters() throws IOException;

    /**
     * Sets the CAN bus bitrate.
     *
     * @param bitrate the bitrate in bits per second (e.g. 500000 for 500 kbps)
     * @throws IOException if the bitrate cannot be set
     * @throws IllegalArgumentException if the bitrate is not supported
     */
    void setBitrate(int bitrate) throws IOException;

    /**
     * Returns whether the CAN bus interface is currently open and ready.
     *
     * @return {@code true} if the bus is open
     */
    boolean isOpen();

    /**
     * Opens the CAN bus interface for communication.
     *
     * @throws IOException if the interface cannot be opened
     * @throws IllegalStateException if the interface is already open
     */
    void open() throws IOException;

    /**
     * Closes the CAN bus interface and releases associated resources.
     *
     * @throws IOException if an error occurs during close
     */
    void close() throws IOException;
}
