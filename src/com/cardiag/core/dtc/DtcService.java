package com.cardiag.core.dtc;

import com.cardiag.core.ecu.EcuConnection;
import com.cardiag.core.io.DataReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for reading and clearing Diagnostic Trouble Codes (DTCs) from an ECU.
 *
 * <p>Uses UDS services:
 * <ul>
 *   <li>0x19 (ReadDTCInformation) - to read stored and pending DTCs</li>
 *   <li>0x14 (ClearDiagnosticInformation) - to clear all DTCs</li>
 * </ul>
 */
public class DtcService {

    /** UDS service IDs. */
    private static final int SID_READ_DTC_INFO = 0x19;
    private static final int SID_CLEAR_DTC = 0x14;

    /** ReadDTCInformation sub-functions. */
    private static final int SUB_REPORT_DTC_BY_STATUS_MASK = 0x02;
    private static final int SUB_REPORT_DTC_SNAPSHOT_BY_DTC = 0x04;

    /** Status mask for all confirmed DTCs. */
    private static final int STATUS_MASK_ALL = 0xFF;

    /** Status mask for pending DTCs only. */
    private static final int STATUS_MASK_PENDING = 0x04;

    /** DTC group: all DTCs (0xFFFFFF). */
    private static final int DTC_GROUP_ALL = 0xFFFFFF;

    /** Positive response offset. */
    private static final int POSITIVE_RESPONSE_OFFSET = 0x40;

    /**
     * Reads all stored DTCs from the ECU.
     *
     * @param connection the active ECU connection
     * @return a list of all DTCs with their status
     * @throws IOException if communication fails
     */
    public List<DiagnosticTroubleCode> readAllDtcs(EcuConnection connection) throws IOException {
        return readDtcsByStatusMask(connection, STATUS_MASK_ALL);
    }

    /**
     * Reads only pending DTCs from the ECU.
     *
     * @param connection the active ECU connection
     * @return a list of pending DTCs
     * @throws IOException if communication fails
     */
    public List<DiagnosticTroubleCode> readPendingDtcs(EcuConnection connection) throws IOException {
        return readDtcsByStatusMask(connection, STATUS_MASK_PENDING);
    }

    /**
     * Clears all DTCs from the ECU's permanent memory.
     *
     * @param connection the active ECU connection
     * @throws IOException if communication fails or the ECU rejects the clear request
     */
    public void clearAllDtcs(EcuConnection connection) throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");

        byte[] request = new byte[]{
                (byte) SID_CLEAR_DTC,
                (byte) ((DTC_GROUP_ALL >> 16) & 0xFF),
                (byte) ((DTC_GROUP_ALL >> 8) & 0xFF),
                (byte) (DTC_GROUP_ALL & 0xFF)
        };

        connection.sendAndValidate(request, SID_CLEAR_DTC);
    }

    /**
     * Reads the freeze-frame data associated with a specific DTC.
     *
     * @param connection the active ECU connection
     * @param dtc        the DTC whose freeze-frame data should be read
     * @return a new {@code DiagnosticTroubleCode} with the freeze-frame data populated
     * @throws IOException if communication fails
     */
    public DiagnosticTroubleCode readFreezeFrame(EcuConnection connection,
                                                  DiagnosticTroubleCode dtc) throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(dtc, "dtc must not be null");

        int dtcNumber = parseDtcCodeToNumber(dtc.getCode());

        byte[] request = new byte[]{
                (byte) SID_READ_DTC_INFO,
                (byte) SUB_REPORT_DTC_SNAPSHOT_BY_DTC,
                (byte) ((dtcNumber >> 16) & 0xFF),
                (byte) ((dtcNumber >> 8) & 0xFF),
                (byte) (dtcNumber & 0xFF),
                0x01  // snapshot record number (first record)
        };

        byte[] response = connection.sendAndValidate(request, SID_READ_DTC_INFO);

        Map<String, String> freezeFrame = parseFreezeFrameData(response);

        return new DiagnosticTroubleCode(
                dtc.getCode(),
                dtc.getDescription(),
                dtc.getStatusByte(),
                dtc.getCategory(),
                freezeFrame
        );
    }

    // ── Internal helpers ────────────────────────────────────────────────

    /**
     * Reads DTCs filtered by a status mask.
     */
    private List<DiagnosticTroubleCode> readDtcsByStatusMask(EcuConnection connection, int statusMask)
            throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");

        byte[] request = new byte[]{
                (byte) SID_READ_DTC_INFO,
                (byte) SUB_REPORT_DTC_BY_STATUS_MASK,
                (byte) (statusMask & 0xFF)
        };

        byte[] response = connection.sendAndValidate(request, SID_READ_DTC_INFO);

        List<DiagnosticTroubleCode> dtcList = new ArrayList<>();

        // Response format: [positive SID] [sub-function] [DTC status availability mask]
        //                  [DTC high] [DTC mid] [DTC low] [status] ... (repeating)
        if (response.length < 3) {
            return dtcList;
        }

        int offset = 3; // skip SID + sub-function + availability mask
        while (offset + 3 < response.length) {
            int dtcHighByte = DataReader.readByte(response, offset);
            int dtcMidByte = DataReader.readByte(response, offset + 1);
            int dtcLowByte = DataReader.readByte(response, offset + 2);
            int dtcStatus = DataReader.readByte(response, offset + 3);

            int dtcNumber = (dtcHighByte << 16) | (dtcMidByte << 8) | dtcLowByte;
            String dtcCode = formatDtcCode(dtcNumber);
            DiagnosticTroubleCode.Category category = DiagnosticTroubleCode.categoryFromCode(dtcCode);

            dtcList.add(new DiagnosticTroubleCode(dtcCode, "", dtcStatus, category));
            offset += 4;
        }

        return dtcList;
    }

    /**
     * Formats a 3-byte DTC number into the standard SAE J2012 string format.
     *
     * <p>The first two bits of the high byte determine the prefix letter:
     * <ul>
     *   <li>00 = P (Powertrain)</li>
     *   <li>01 = C (Chassis)</li>
     *   <li>10 = B (Body)</li>
     *   <li>11 = U (Network)</li>
     * </ul>
     *
     * @param dtcNumber the 24-bit DTC number
     * @return the formatted code (e.g. "P0301")
     */
    static String formatDtcCode(int dtcNumber) {
        int highByte = (dtcNumber >> 8) & 0xFF;
        int lowByte = dtcNumber & 0xFF;

        char prefix;
        int categoryBits = (highByte >> 6) & 0x03;
        switch (categoryBits) {
            case 0: prefix = 'P'; break;
            case 1: prefix = 'C'; break;
            case 2: prefix = 'B'; break;
            case 3: prefix = 'U'; break;
            default: prefix = 'P'; break;
        }

        int numericPart = ((highByte & 0x3F) << 8) | lowByte;
        return String.format("%c%04X", prefix, numericPart);
    }

    /**
     * Parses a DTC code string back to its 24-bit numeric representation.
     */
    static int parseDtcCodeToNumber(String code) {
        if (code == null || code.length() < 5) {
            throw new IllegalArgumentException("Invalid DTC code: " + code);
        }

        int categoryBits;
        switch (Character.toUpperCase(code.charAt(0))) {
            case 'P': categoryBits = 0; break;
            case 'C': categoryBits = 1; break;
            case 'B': categoryBits = 2; break;
            case 'U': categoryBits = 3; break;
            default:
                throw new IllegalArgumentException("Unknown DTC prefix: " + code.charAt(0));
        }

        int numericPart = Integer.parseInt(code.substring(1), 16);
        int highByte = (categoryBits << 6) | ((numericPart >> 8) & 0x3F);
        int lowByte = numericPart & 0xFF;

        return (highByte << 8) | lowByte;
    }

    /**
     * Parses freeze-frame data from a ReadDTCInformation sub-04 response.
     * The data is returned as key-value pairs of DID names to hex values.
     */
    private Map<String, String> parseFreezeFrameData(byte[] response) {
        Map<String, String> data = new LinkedHashMap<>();

        // Response format after header: [DTC bytes (3)] [status (1)] [record number (1)]
        // then repeating: [DID high] [DID low] [data length implied by DID] [data...]
        // Simplified: parse DID + 2-byte value pairs
        int offset = 8; // skip SID + sub + DTC(3) + status + record number
        while (offset + 3 < response.length) {
            int did = DataReader.readShort(response, offset);
            offset += 2;

            // Read remaining bytes for this DID (assume 2 bytes per value as common case)
            int valueLen = Math.min(2, response.length - offset);
            String hexValue = DataReader.bytesToHex(response, offset, valueLen);
            data.put(String.format("DID_0x%04X", did), hexValue);
            offset += valueLen;
        }

        return data;
    }
}
