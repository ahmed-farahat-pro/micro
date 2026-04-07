package com.cardiag.protocol.obd;

/**
 * Enumeration of OBD-II diagnostic modes (services) as defined in
 * SAE J1979 / ISO 15031-5.
 *
 * <p>Each mode represents a category of diagnostic data or control
 * operation available through the standard OBD-II interface.</p>
 */
public enum ObdMode {

    /** Mode 01 – Show current (real-time) powertrain data. */
    CURRENT_DATA(0x01, "Current Data", "Show current powertrain data"),

    /** Mode 02 – Show freeze frame data captured at the time of a DTC. */
    FREEZE_FRAME(0x02, "Freeze Frame Data", "Show freeze frame data"),

    /** Mode 03 – Show stored (confirmed) diagnostic trouble codes. */
    STORED_DTCS(0x03, "Stored DTCs", "Show stored diagnostic trouble codes"),

    /** Mode 04 – Clear diagnostic trouble codes and stored values. */
    CLEAR_DTCS(0x04, "Clear DTCs", "Clear diagnostic trouble codes and stored values"),

    /** Mode 05 – Oxygen sensor monitoring test results. */
    O2_MONITORING(0x05, "O2 Sensor Monitoring", "Oxygen sensor monitoring test results"),

    /** Mode 06 – Non-continuous monitoring system test results. */
    NON_CONTINUOUS_MONITORING(0x06, "Non-Continuous Monitoring",
            "On-board monitoring test results for non-continuously monitored systems"),

    /** Mode 07 – Show pending (current driving cycle) diagnostic trouble codes. */
    PENDING_DTCS(0x07, "Pending DTCs", "Show pending diagnostic trouble codes"),

    /** Mode 08 – Control on-board system operations. */
    CONTROL_OPERATIONS(0x08, "Control Operations", "Control operation of on-board systems"),

    /** Mode 09 – Request vehicle information (VIN, calibration ID, etc.). */
    VEHICLE_INFO(0x09, "Vehicle Information", "Request vehicle information"),

    /** Mode 0A – Show permanent (emission-related) diagnostic trouble codes. */
    PERMANENT_DTCS(0x0A, "Permanent DTCs", "Show permanent diagnostic trouble codes");

    private final int modeId;
    private final String name;
    private final String description;

    ObdMode(int modeId, String name, String description) {
        this.modeId = modeId;
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the numeric mode identifier.
     *
     * @return the mode ID byte value (0x01-0x0A)
     */
    public int getModeId() {
        return modeId;
    }

    /**
     * Returns the human-readable name of this mode.
     *
     * @return the mode name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a description of what this mode does.
     *
     * @return the mode description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the positive response mode ID ({@code modeId + 0x40}).
     *
     * @return the response mode ID
     */
    public int getResponseModeId() {
        return modeId + 0x40;
    }

    /**
     * Looks up an {@code ObdMode} by its numeric value.
     *
     * @param modeId the mode ID byte value
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static ObdMode fromId(int modeId) {
        for (ObdMode mode : values()) {
            if (mode.modeId == modeId) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown OBD-II mode: 0x%02X", modeId));
    }

    @Override
    public String toString() {
        return String.format("Mode %02X: %s", modeId, name);
    }
}
