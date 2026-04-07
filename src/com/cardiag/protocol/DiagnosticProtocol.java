package com.cardiag.protocol;

import java.io.IOException;

/**
 * Abstract base class for vehicle diagnostic protocols.
 * Provides the common interface and shared state for all protocol implementations
 * such as UDS, KWP2000, and OBD-II.
 */
public abstract class DiagnosticProtocol {

    /** Default timeout in milliseconds for send/receive operations. */
    protected static final int DEFAULT_TIMEOUT_MS = 5000;

    /** Current timeout in milliseconds. */
    protected int timeoutMs = DEFAULT_TIMEOUT_MS;

    /** Whether the protocol session is currently connected. */
    protected boolean connected = false;

    /**
     * Establishes a connection to the target ECU using this protocol.
     *
     * @throws IOException if the connection cannot be established
     */
    public abstract void connect() throws IOException;

    /**
     * Disconnects from the target ECU and releases resources.
     *
     * @throws IOException if an error occurs during disconnection
     */
    public abstract void disconnect() throws IOException;

    /**
     * Sends a raw diagnostic request to the ECU.
     *
     * @param data the request payload bytes
     * @throws IOException if the request cannot be sent
     * @throws IllegalStateException if the protocol is not connected
     */
    public abstract void sendRequest(byte[] data) throws IOException;

    /**
     * Reads a response from the ECU. Blocks up to the configured timeout.
     *
     * @return the response payload bytes
     * @throws IOException if a response cannot be read or the operation times out
     * @throws IllegalStateException if the protocol is not connected
     */
    public abstract byte[] readResponse() throws IOException;

    /**
     * Returns whether a protocol session is currently active.
     *
     * @return {@code true} if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns the type of this diagnostic protocol.
     *
     * @return the protocol type
     */
    public abstract ProtocolType getProtocolType();

    /**
     * Sets the timeout for send and receive operations.
     *
     * @param ms timeout in milliseconds; must be positive
     * @throws IllegalArgumentException if {@code ms} is not positive
     */
    public void setTimeout(int ms) {
        if (ms <= 0) {
            throw new IllegalArgumentException("Timeout must be positive, got: " + ms);
        }
        this.timeoutMs = ms;
    }

    /**
     * Returns the current timeout in milliseconds.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeoutMs;
    }

    /**
     * Checks that the protocol is connected and throws if it is not.
     *
     * @throws IllegalStateException if the protocol is not connected
     */
    protected void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("Protocol is not connected");
        }
    }
}
