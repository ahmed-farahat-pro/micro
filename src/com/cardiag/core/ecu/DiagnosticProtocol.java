package com.cardiag.core.ecu;

import java.io.IOException;

/**
 * Abstraction for a low-level diagnostic transport protocol (e.g. ISO-TP over CAN,
 * KWP2000 over K-Line, DoIP over Ethernet).
 *
 * <p>Implementations handle framing, timing, and multi-frame segmentation so that
 * higher-level classes like {@link EcuConnection} can work with complete UDS
 * request/response byte arrays.</p>
 */
public interface DiagnosticProtocol {

    /**
     * Establishes the transport connection to the specified ECU.
     *
     * @param targetId   the target (request) address
     * @param responseId the expected response address
     * @throws IOException if the connection cannot be established
     */
    void connect(int targetId, int responseId) throws IOException;

    /**
     * Closes the transport connection.
     *
     * @throws IOException if an error occurs while disconnecting
     */
    void disconnect() throws IOException;

    /**
     * Sends a raw UDS request and waits for the complete response.
     *
     * @param request the request bytes (starting with the UDS service ID)
     * @return the complete response bytes
     * @throws IOException if communication fails or times out
     */
    byte[] send(byte[] request) throws IOException;

    /**
     * Returns {@code true} if the transport connection is currently open.
     *
     * @return connection status
     */
    boolean isConnected();
}
