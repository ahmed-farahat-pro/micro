package com.cardiag.system;

import com.cardiag.protocol.DiagnosticProtocol;

import java.io.IOException;

/**
 * Represents an active diagnostic connection to a single ECU.
 * Wraps a {@link DiagnosticProtocol} together with the ECU's
 * definition so that higher-level systems can send targeted requests.
 */
public class EcuConnection {

    private final EcuDefinition ecuDefinition;
    private final DiagnosticProtocol protocol;
    private boolean active;

    /**
     * Creates a new ECU connection.
     *
     * @param ecuDefinition the ECU to connect to
     * @param protocol      the protocol used for communication
     */
    public EcuConnection(EcuDefinition ecuDefinition, DiagnosticProtocol protocol) {
        this.ecuDefinition = ecuDefinition;
        this.protocol = protocol;
        this.active = false;
    }

    /**
     * Opens the connection to the ECU.
     *
     * @throws IOException if the connection cannot be established
     */
    public void connect() throws IOException {
        protocol.connect();
        active = true;
    }

    /**
     * Closes the connection to the ECU.
     *
     * @throws IOException if an error occurs during disconnection
     */
    public void disconnect() throws IOException {
        protocol.disconnect();
        active = false;
    }

    /** @return {@code true} if the connection is active */
    public boolean isActive() {
        return active;
    }

    /** @return the ECU definition associated with this connection */
    public EcuDefinition getEcuDefinition() {
        return ecuDefinition;
    }

    /** @return the underlying diagnostic protocol */
    public DiagnosticProtocol getProtocol() {
        return protocol;
    }
}
