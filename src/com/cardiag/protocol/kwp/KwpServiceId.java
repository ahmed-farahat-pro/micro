package com.cardiag.protocol.kwp;

/**
 * Enumeration of KWP2000 (Keyword Protocol 2000) service identifiers
 * as defined in ISO 14230.
 *
 * <p>Each constant holds the one-byte service ID used in KWP2000 request
 * messages. The positive response ID is always {@code serviceId + 0x40}.</p>
 */
public enum KwpServiceId {

    /** StartCommunication (0x81). */
    START_COMMUNICATION(0x81, "StartCommunication"),

    /** StopCommunication (0x82). */
    STOP_COMMUNICATION(0x82, "StopCommunication"),

    /** AccessTimingParameters (0x83). */
    ACCESS_TIMING(0x83, "AccessTimingParameters"),

    /** StartDiagnosticSession (0x10). */
    START_DIAGNOSTIC(0x10, "StartDiagnosticSession"),

    /** StopDiagnosticSession (0x20). */
    STOP_DIAGNOSTIC(0x20, "StopDiagnosticSession"),

    /** ECUReset (0x11). */
    ECU_RESET(0x11, "ECUReset"),

    /** ReadDTCByStatus (0x18). */
    READ_DTC_BY_STATUS(0x18, "ReadDTCByStatus"),

    /** ReadECUIdentification (0x1A). */
    READ_ECU_ID(0x1A, "ReadECUIdentification"),

    /** ReadDataByLocalIdentifier (0x21). */
    READ_DATA_BY_LOCAL_ID(0x21, "ReadDataByLocalIdentifier"),

    /** ReadDataByCommonIdentifier (0x22). */
    READ_DATA_BY_COMMON_ID(0x22, "ReadDataByCommonIdentifier"),

    /** ReadMemoryByAddress (0x23). */
    READ_MEMORY_BY_ADDRESS(0x23, "ReadMemoryByAddress"),

    /** SecurityAccess (0x27). */
    SECURITY_ACCESS(0x27, "SecurityAccess"),

    /** WriteDataByLocalIdentifier (0x2C). */
    WRITE_DATA_BY_LOCAL_ID(0x2C, "WriteDataByLocalIdentifier"),

    /** WriteDataByCommonIdentifier (0x2E). */
    WRITE_DATA_BY_COMMON_ID(0x2E, "WriteDataByCommonIdentifier"),

    /** ClearDiagnosticInformation (0x14). */
    CLEAR_DTC(0x14, "ClearDiagnosticInformation"),

    /** InputOutputControlByLocalIdentifier (0x30). */
    INPUT_OUTPUT_CONTROL(0x30, "InputOutputControlByLocalIdentifier"),

    /** StartRoutineByLocalIdentifier (0x31). */
    START_ROUTINE(0x31, "StartRoutineByLocalIdentifier"),

    /** StopRoutineByLocalIdentifier (0x32). */
    STOP_ROUTINE(0x32, "StopRoutineByLocalIdentifier"),

    /** RequestDownload (0x34). */
    REQUEST_DOWNLOAD(0x34, "RequestDownload"),

    /** RequestUpload (0x35). */
    REQUEST_UPLOAD(0x35, "RequestUpload"),

    /** TransferData (0x36). */
    TRANSFER_DATA(0x36, "TransferData"),

    /** RequestTransferExit (0x37). */
    TRANSFER_EXIT(0x37, "RequestTransferExit"),

    /** WriteMemoryByAddress (0x3D). */
    WRITE_MEMORY_BY_ADDRESS(0x3D, "WriteMemoryByAddress"),

    /** TesterPresent (0x3E). */
    TESTER_PRESENT(0x3E, "TesterPresent");

    private final int id;
    private final String serviceName;

    KwpServiceId(int id, String serviceName) {
        this.id = id;
        this.serviceName = serviceName;
    }

    /**
     * Returns the numeric service identifier.
     *
     * @return the service ID byte value
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the human-readable service name from the ISO specification.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the positive response service ID ({@code serviceId + 0x40}).
     *
     * @return the positive response ID
     */
    public int getResponseId() {
        return id + 0x40;
    }

    /**
     * Looks up a {@code KwpServiceId} by its numeric value.
     *
     * @param id the service ID byte value
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static KwpServiceId fromId(int id) {
        for (KwpServiceId sid : values()) {
            if (sid.id == id) {
                return sid;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown KWP2000 service ID: 0x%02X", id));
    }

    @Override
    public String toString() {
        return String.format("%s (0x%02X)", serviceName, id);
    }
}
