package com.cardiag.core.ecu;

import com.cardiag.core.security.SeedKeyAlgorithm;
import com.cardiag.core.security.SecurityLevel;
import com.cardiag.core.session.DiagnosticSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Manages an active diagnostic session to a single ECU.
 *
 * <p>This class is the primary gateway for all diagnostic communication with an ECU.
 * It encapsulates connection state, session management, security access, and
 * raw data read/write operations using UDS services.</p>
 *
 * <p>Typical usage:
 * <pre>{@code
 *   EcuConnection conn = new EcuConnection(ecuDefinition, protocol);
 *   conn.connect();
 *   conn.openSession(DiagnosticSession.EXTENDED);
 *   conn.performSecurityAccess(seedKeyAlgorithm);
 *   byte[] data = conn.readData(0xF190); // read VIN
 *   conn.disconnect();
 * }</pre>
 *
 * <p>This class is <b>not</b> thread-safe. External synchronisation is required
 * when accessed from multiple threads.</p>
 */
public class EcuConnection {

    /** UDS service IDs. */
    private static final int SID_DIAGNOSTIC_SESSION_CONTROL = 0x10;
    private static final int SID_SECURITY_ACCESS = 0x27;
    private static final int SID_READ_DATA_BY_ID = 0x22;
    private static final int SID_WRITE_DATA_BY_ID = 0x2E;
    private static final int SID_ROUTINE_CONTROL = 0x31;
    private static final int ROUTINE_CONTROL_START = 0x01;
    private static final int POSITIVE_RESPONSE_OFFSET = 0x40;

    private final EcuDefinition ecuDefinition;
    private final DiagnosticProtocol protocol;
    private DiagnosticSession sessionType;
    private SecurityLevel securityLevel;
    private boolean connected;

    /**
     * Constructs a new {@code EcuConnection}.
     *
     * @param ecuDefinition the ECU definition describing the target ECU
     * @param protocol      the low-level diagnostic protocol transport
     */
    public EcuConnection(EcuDefinition ecuDefinition, DiagnosticProtocol protocol) {
        this.ecuDefinition = Objects.requireNonNull(ecuDefinition, "ecuDefinition must not be null");
        this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
        this.sessionType = DiagnosticSession.DEFAULT;
        this.securityLevel = SecurityLevel.LOCKED;
        this.connected = false;
    }

    // ── Connection lifecycle ────────────────────────────────────────────

    /**
     * Establishes the low-level transport connection to the ECU.
     *
     * @throws IOException  if the transport layer fails to connect
     * @throws IllegalStateException if already connected
     */
    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("Already connected to " + ecuDefinition.getName());
        }
        EcuAddress address = ecuDefinition.getAddress();
        protocol.connect(address.getPhysicalId(), address.getResponseId());
        connected = true;
        sessionType = DiagnosticSession.DEFAULT;
        securityLevel = SecurityLevel.LOCKED;
    }

    /**
     * Closes the transport connection and resets session state.
     *
     * @throws IOException if the transport layer fails to disconnect
     */
    public void disconnect() throws IOException {
        if (!connected) {
            return;
        }
        try {
            protocol.disconnect();
        } finally {
            connected = false;
            sessionType = DiagnosticSession.DEFAULT;
            securityLevel = SecurityLevel.LOCKED;
        }
    }

    // ── Session management ──────────────────────────────────────────────

    /**
     * Requests a diagnostic session change on the ECU using UDS service 0x10.
     *
     * @param session the target diagnostic session
     * @throws IOException           if communication fails
     * @throws DiagnosticException   if the ECU returns a negative response
     * @throws IllegalStateException if not connected
     */
    public void openSession(DiagnosticSession session) throws IOException {
        ensureConnected();
        Objects.requireNonNull(session, "session must not be null");

        byte[] request = new byte[]{
                (byte) SID_DIAGNOSTIC_SESSION_CONTROL,
                (byte) session.getSessionTypeByte()
        };
        byte[] response = sendAndValidate(request, SID_DIAGNOSTIC_SESSION_CONTROL);
        this.sessionType = session;
    }

    // ── Security access ─────────────────────────────────────────────────

    /**
     * Performs a UDS SecurityAccess (0x27) seed-key handshake to unlock the ECU
     * at the level defined in the {@link EcuDefinition}.
     *
     * @param algorithm the seed-key algorithm implementation
     * @throws IOException           if communication fails
     * @throws DiagnosticException   if the ECU returns a negative response or the key is rejected
     * @throws IllegalStateException if not connected
     */
    public void performSecurityAccess(SeedKeyAlgorithm algorithm) throws IOException {
        ensureConnected();
        Objects.requireNonNull(algorithm, "algorithm must not be null");

        int level = ecuDefinition.getSecurityLevel();
        SecurityLevel targetLevel = SecurityLevel.fromAccessLevel(level);

        // Step 1: request seed
        byte[] seedRequest = new byte[]{
                (byte) SID_SECURITY_ACCESS,
                (byte) targetLevel.getAccessLevel()
        };
        byte[] seedResponse = sendAndValidate(seedRequest, SID_SECURITY_ACCESS);

        // Extract seed bytes (skip service ID + sub-function echo)
        byte[] seed = Arrays.copyOfRange(seedResponse, 2, seedResponse.length);

        // Check for zero seed (already unlocked)
        boolean allZero = true;
        for (byte b : seed) {
            if (b != 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            this.securityLevel = targetLevel;
            return;
        }

        // Step 2: compute key and send
        byte[] key = algorithm.calculateKey(seed, targetLevel.getAccessLevel());

        byte[] keyRequest = new byte[key.length + 2];
        keyRequest[0] = (byte) SID_SECURITY_ACCESS;
        keyRequest[1] = (byte) targetLevel.getKeySendByte();
        System.arraycopy(key, 0, keyRequest, 2, key.length);

        sendAndValidate(keyRequest, SID_SECURITY_ACCESS);
        this.securityLevel = targetLevel;
    }

    // ── Data services ───────────────────────────────────────────────────

    /**
     * Reads data from the ECU by Data Identifier using UDS service 0x22.
     *
     * @param did the 16-bit Data Identifier
     * @return the data bytes returned by the ECU (excluding the service ID and DID echo)
     * @throws IOException           if communication fails
     * @throws DiagnosticException   if the ECU returns a negative response
     * @throws IllegalStateException if not connected
     */
    public byte[] readData(int did) throws IOException {
        ensureConnected();

        byte[] request = new byte[]{
                (byte) SID_READ_DATA_BY_ID,
                (byte) ((did >> 8) & 0xFF),
                (byte) (did & 0xFF)
        };
        byte[] response = sendAndValidate(request, SID_READ_DATA_BY_ID);

        // Response format: [positive SID] [DID high] [DID low] [data...]
        if (response.length < 3) {
            throw new DiagnosticException("ReadDataByIdentifier response too short for DID 0x"
                    + Integer.toHexString(did));
        }
        return Arrays.copyOfRange(response, 3, response.length);
    }

    /**
     * Writes data to the ECU by Data Identifier using UDS service 0x2E.
     *
     * @param did  the 16-bit Data Identifier
     * @param data the data bytes to write
     * @throws IOException           if communication fails
     * @throws DiagnosticException   if the ECU returns a negative response
     * @throws IllegalStateException if not connected
     */
    public void writeData(int did, byte[] data) throws IOException {
        ensureConnected();
        Objects.requireNonNull(data, "data must not be null");

        byte[] request = new byte[3 + data.length];
        request[0] = (byte) SID_WRITE_DATA_BY_ID;
        request[1] = (byte) ((did >> 8) & 0xFF);
        request[2] = (byte) (did & 0xFF);
        System.arraycopy(data, 0, request, 3, data.length);

        sendAndValidate(request, SID_WRITE_DATA_BY_ID);
    }

    /**
     * Executes a routine on the ECU using UDS service 0x31 (RoutineControl - Start).
     *
     * @param routineId     the 16-bit routine identifier
     * @param routineOption optional parameter data (may be empty, must not be null)
     * @return the routine result data bytes (excluding service ID and routine ID echo)
     * @throws IOException           if communication fails
     * @throws DiagnosticException   if the ECU returns a negative response
     * @throws IllegalStateException if not connected
     */
    public byte[] executeRoutine(int routineId, byte[] routineOption) throws IOException {
        ensureConnected();
        Objects.requireNonNull(routineOption, "routineOption must not be null");

        byte[] request = new byte[4 + routineOption.length];
        request[0] = (byte) SID_ROUTINE_CONTROL;
        request[1] = (byte) ROUTINE_CONTROL_START;
        request[2] = (byte) ((routineId >> 8) & 0xFF);
        request[3] = (byte) (routineId & 0xFF);
        System.arraycopy(routineOption, 0, request, 4, routineOption.length);

        byte[] response = sendAndValidate(request, SID_ROUTINE_CONTROL);

        // Response format: [positive SID] [sub-function] [routineId high] [routineId low] [results...]
        if (response.length < 4) {
            throw new DiagnosticException("RoutineControl response too short for routine 0x"
                    + Integer.toHexString(routineId));
        }
        return Arrays.copyOfRange(response, 4, response.length);
    }

    // ── Internal helpers ────────────────────────────────────────────────

    /**
     * Sends a raw UDS request via the protocol and validates the response.
     *
     * @param request   the raw request bytes (starting with the service ID)
     * @param serviceId the expected service ID for positive-response validation
     * @return the full positive response bytes
     * @throws IOException         if communication fails
     * @throws DiagnosticException if the ECU returns a negative response (0x7F)
     */
    public byte[] sendAndValidate(byte[] request, int serviceId) throws IOException {
        byte[] response = protocol.send(request);

        if (response == null || response.length == 0) {
            throw new DiagnosticException("No response received from ECU");
        }

        // Check for Negative Response (0x7F)
        if ((response[0] & 0xFF) == 0x7F) {
            int rejectedService = response.length > 1 ? (response[1] & 0xFF) : 0;
            int nrc = response.length > 2 ? (response[2] & 0xFF) : 0;
            throw new DiagnosticException(String.format(
                    "Negative response for service 0x%02X: NRC=0x%02X", rejectedService, nrc));
        }

        // Verify positive response SID
        int expectedPositive = serviceId + POSITIVE_RESPONSE_OFFSET;
        if ((response[0] & 0xFF) != expectedPositive) {
            throw new DiagnosticException(String.format(
                    "Unexpected response SID: expected 0x%02X, got 0x%02X",
                    expectedPositive, response[0] & 0xFF));
        }

        return response;
    }

    private void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("Not connected to ECU " + ecuDefinition.getName());
        }
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /** Returns the ECU definition for this connection. */
    public EcuDefinition getEcuDefinition() {
        return ecuDefinition;
    }

    /** Returns the underlying diagnostic protocol transport. */
    public DiagnosticProtocol getProtocol() {
        return protocol;
    }

    /** Returns the current diagnostic session type. */
    public DiagnosticSession getSessionType() {
        return sessionType;
    }

    /** Returns the current security access level. */
    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    /** Returns {@code true} if the transport connection is currently open. */
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String toString() {
        return String.format("EcuConnection{ecu='%s', session=%s, security=%s, connected=%s}",
                ecuDefinition.getName(), sessionType, securityLevel, connected);
    }
}
