package com.cardiag.protocol.obd;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.protocol.ProtocolType;
import com.cardiag.protocol.can.CanBus;
import com.cardiag.protocol.can.CanFrame;

import java.io.IOException;
import java.util.Arrays;

/**
 * Standard OBD-II protocol implementation over CAN bus.
 *
 * <p>OBD-II uses a broadcast CAN request address of {@code 0x7DF} and listens
 * for responses from ECU addresses {@code 0x7E8} through {@code 0x7EF}.
 * Each response is a single CAN frame formatted according to ISO 15765-4.</p>
 *
 * <h3>Request Frame Format (Single Frame)</h3>
 * <pre>
 *   Byte 0: number of additional data bytes (PCI)
 *   Byte 1: OBD mode (service) ID
 *   Byte 2: PID
 *   Bytes 3-7: padding (0x55)
 * </pre>
 *
 * <h3>Response Frame Format</h3>
 * <pre>
 *   Byte 0: number of additional data bytes (PCI)
 *   Byte 1: OBD mode + 0x40 (positive response)
 *   Byte 2: PID
 *   Bytes 3+: data values
 * </pre>
 */
public class ObdProtocol extends DiagnosticProtocol {

    /** OBD-II functional (broadcast) request CAN ID. */
    public static final int OBD_BROADCAST_ID = 0x7DF;

    /** First ECU response CAN ID. */
    public static final int OBD_RESPONSE_ID_START = 0x7E8;

    /** Last ECU response CAN ID. */
    public static final int OBD_RESPONSE_ID_END = 0x7EF;

    /** OBD-II padding byte value. */
    private static final byte OBD_PADDING = 0x55;

    private final CanBus canBus;

    /**
     * Creates an OBD-II protocol instance.
     *
     * @param canBus the CAN bus transport
     */
    public ObdProtocol(CanBus canBus) {
        if (canBus == null) {
            throw new IllegalArgumentException("CAN bus must not be null");
        }
        this.canBus = canBus;
    }

    @Override
    public void connect() throws IOException {
        if (!canBus.isOpen()) {
            canBus.open();
        }

        // Accept responses from all standard ECU addresses (0x7E8-0x7EF)
        canBus.addFilter(OBD_RESPONSE_ID_START, 0x7F8);
        connected = true;
    }

    @Override
    public void disconnect() throws IOException {
        connected = false;
        canBus.clearFilters();
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.OBD2;
    }

    /**
     * Sends a raw OBD-II request. The data array should contain the mode
     * and PID bytes (the PCI length byte and padding are added automatically).
     *
     * @param data the request payload (mode + PID + optional additional bytes)
     * @throws IOException if the frame cannot be sent
     */
    @Override
    public void sendRequest(byte[] data) throws IOException {
        ensureConnected();

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Request data must not be null or empty");
        }

        byte[] frame = new byte[8];
        frame[0] = (byte) data.length; // PCI: number of data bytes

        int copyLen = Math.min(data.length, 7);
        System.arraycopy(data, 0, frame, 1, copyLen);

        // Pad remaining bytes
        for (int i = 1 + copyLen; i < 8; i++) {
            frame[i] = OBD_PADDING;
        }

        canBus.sendFrame(new CanFrame(OBD_BROADCAST_ID, frame));
    }

    /**
     * Reads an OBD-II response frame. Returns the data payload starting
     * from the mode response byte (excluding the PCI byte).
     *
     * @return the response bytes (mode + PID + data values)
     * @throws IOException if no response is received within the timeout
     */
    @Override
    public byte[] readResponse() throws IOException {
        ensureConnected();

        CanFrame frame = canBus.receiveFrame(timeoutMs);
        if (frame == null) {
            throw new IOException("Timeout waiting for OBD-II response");
        }

        byte[] frameData = frame.getData();
        if (frameData.length == 0) {
            throw new IOException("Received empty CAN frame");
        }

        int dataLength = frameData[0] & 0xFF;
        if (dataLength == 0 || dataLength > 7) {
            throw new IOException("Invalid OBD-II response PCI length: " + dataLength);
        }

        return Arrays.copyOfRange(frameData, 1, 1 + dataLength);
    }

    /**
     * Queries a single PID in Mode 01 (current data) and returns the raw
     * response bytes (including the mode and PID echo bytes).
     *
     * @param pid the PID to query
     * @return the response bytes
     * @throws IOException if the query fails
     */
    public byte[] queryCurrentData(ObdPid pid) throws IOException {
        return queryPid(ObdMode.CURRENT_DATA, pid.getPid());
    }

    /**
     * Queries a PID in the specified OBD mode and returns the raw response bytes.
     *
     * @param mode the OBD mode
     * @param pid  the PID value
     * @return the response bytes
     * @throws IOException if the query fails
     */
    public byte[] queryPid(ObdMode mode, int pid) throws IOException {
        sendRequest(new byte[]{(byte) mode.getModeId(), (byte) pid});
        return readResponse();
    }

    /**
     * Queries Mode 03 to retrieve stored diagnostic trouble codes.
     *
     * @return the raw response bytes containing the DTC data
     * @throws IOException if the query fails
     */
    public byte[] readStoredDtcs() throws IOException {
        sendRequest(new byte[]{(byte) ObdMode.STORED_DTCS.getModeId()});
        return readResponse();
    }

    /**
     * Sends a Mode 04 request to clear all stored DTCs and reset monitors.
     *
     * @throws IOException if the request fails
     */
    public void clearDtcs() throws IOException {
        sendRequest(new byte[]{(byte) ObdMode.CLEAR_DTCS.getModeId()});
        byte[] response = readResponse();
        int responseSid = response[0] & 0xFF;
        if (responseSid != ObdMode.CLEAR_DTCS.getResponseModeId()) {
            throw new IOException(String.format(
                    "Unexpected response to clear DTCs: 0x%02X", responseSid));
        }
    }

    /**
     * Queries Mode 09 PID 0x02 to retrieve the vehicle identification number (VIN).
     *
     * @return the raw VIN response bytes
     * @throws IOException if the query fails
     */
    public byte[] requestVin() throws IOException {
        return queryPid(ObdMode.VEHICLE_INFO, 0x02);
    }

    /**
     * Checks whether a specific PID is supported by sending a Mode 01 PID 0x00
     * request and examining the bit-encoded response.
     *
     * @param pid the PID to check (0x01-0x20)
     * @return {@code true} if the PID is reported as supported
     * @throws IOException              if the query fails
     * @throws IllegalArgumentException if the PID is outside the 0x01-0x20 range
     */
    public boolean isPidSupported(int pid) throws IOException {
        if (pid < 0x01 || pid > 0x20) {
            throw new IllegalArgumentException(
                    String.format("PID 0x%02X is outside the 0x01-0x20 range "
                            + "checked by PID 0x00. Use the appropriate range PID.", pid));
        }

        byte[] response = queryPid(ObdMode.CURRENT_DATA, ObdPid.SUPPORTED_PIDS.getPid());
        // Response: mode echo, PID echo, then 4 bytes of bit-encoded support
        if (response.length < 6) {
            throw new IOException("Incomplete supported PIDs response");
        }

        // Build the 32-bit support mask from response bytes 2-5
        int supportMask = ((response[2] & 0xFF) << 24)
                | ((response[3] & 0xFF) << 16)
                | ((response[4] & 0xFF) << 8)
                | (response[5] & 0xFF);

        // PID 0x01 corresponds to bit 31 (MSB), PID 0x20 to bit 0
        int bitPosition = 32 - pid;
        return (supportMask & (1 << bitPosition)) != 0;
    }
}
