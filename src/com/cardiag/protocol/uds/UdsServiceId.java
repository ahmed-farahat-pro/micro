package com.cardiag.protocol.uds;

/**
 * Enumeration of UDS (Unified Diagnostic Services) service identifiers
 * as defined in ISO 14229-1.
 *
 * <p>Each constant carries the one-byte service ID used in UDS request
 * messages. The positive response ID is always {@code serviceId + 0x40}.</p>
 */
public enum UdsServiceId {

    /** DiagnosticSessionControl (0x10). */
    DIAGNOSTIC_SESSION_CONTROL(0x10, "DiagnosticSessionControl"),

    /** ECUReset (0x11). */
    ECU_RESET(0x11, "ECUReset"),

    /** ClearDiagnosticInformation (0x14). */
    CLEAR_DTC(0x14, "ClearDiagnosticInformation"),

    /** ReadDTCInformation (0x19). */
    READ_DTC(0x19, "ReadDTCInformation"),

    /** ReadDataByIdentifier (0x22). */
    READ_DATA_BY_ID(0x22, "ReadDataByIdentifier"),

    /** ReadMemoryByAddress (0x23). */
    READ_MEMORY(0x23, "ReadMemoryByAddress"),

    /** SecurityAccess (0x27). */
    SECURITY_ACCESS(0x27, "SecurityAccess"),

    /** CommunicationControl (0x28). */
    COMMUNICATION_CONTROL(0x28, "CommunicationControl"),

    /** WriteDataByIdentifier (0x2E). */
    WRITE_DATA_BY_ID(0x2E, "WriteDataByIdentifier"),

    /** InputOutputControlByIdentifier (0x2F). */
    IO_CONTROL(0x2F, "InputOutputControlByIdentifier"),

    /** RoutineControl (0x31). */
    ROUTINE_CONTROL(0x31, "RoutineControl"),

    /** RequestDownload (0x34). */
    REQUEST_DOWNLOAD(0x34, "RequestDownload"),

    /** RequestUpload (0x35). */
    REQUEST_UPLOAD(0x35, "RequestUpload"),

    /** TransferData (0x36). */
    TRANSFER_DATA(0x36, "TransferData"),

    /** RequestTransferExit (0x37). */
    TRANSFER_EXIT(0x37, "RequestTransferExit"),

    /** WriteMemoryByAddress (0x3D). */
    WRITE_MEMORY(0x3D, "WriteMemoryByAddress"),

    /** TesterPresent (0x3E). */
    TESTER_PRESENT(0x3E, "TesterPresent"),

    /** ControlDTCSetting (0x85). */
    CONTROL_DTC_SETTING(0x85, "ControlDTCSetting");

    private final int id;
    private final String serviceName;

    UdsServiceId(int id, String serviceName) {
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
     * Looks up a {@code UdsServiceId} by its numeric value.
     *
     * @param id the service ID byte value
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static UdsServiceId fromId(int id) {
        for (UdsServiceId sid : values()) {
            if (sid.id == id) {
                return sid;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown UDS service ID: 0x%02X", id));
    }

    @Override
    public String toString() {
        return String.format("%s (0x%02X)", serviceName, id);
    }
}
