package com.cardiag.core.dtc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing a single Diagnostic Trouble Code (DTC) read from an ECU.
 *
 * <p>A DTC follows the SAE J2012 / ISO 15031-6 format (e.g. "P0301" for cylinder 1
 * misfire). Each DTC carries a status byte indicating its current state and may
 * have associated freeze-frame data captured at the time of the fault.</p>
 */
public class DiagnosticTroubleCode {

    /**
     * Category of the DTC based on the first character of its code.
     */
    public enum Category {
        /** Powertrain DTCs (Pxxxx). */
        POWERTRAIN,
        /** Body DTCs (Bxxxx). */
        BODY,
        /** Chassis DTCs (Cxxxx). */
        CHASSIS,
        /** Network/Communication DTCs (Uxxxx). */
        NETWORK
    }

    private final String code;
    private final String description;
    private final int statusByte;
    private final Category category;
    private final Map<String, String> freezeFrameData;

    /**
     * Constructs a new {@code DiagnosticTroubleCode}.
     *
     * @param code            the DTC code string (e.g. "P0301")
     * @param description     a human-readable description of the fault
     * @param statusByte      the raw UDS status byte (ISO 14229 statusOfDTC)
     * @param category        the DTC category
     * @param freezeFrameData freeze-frame data captured at the time of the fault (may be empty)
     */
    public DiagnosticTroubleCode(String code, String description, int statusByte,
                                 Category category, Map<String, String> freezeFrameData) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.statusByte = statusByte & 0xFF;
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.freezeFrameData = freezeFrameData != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(freezeFrameData))
                : Collections.emptyMap();
    }

    /**
     * Convenience constructor without freeze-frame data.
     *
     * @param code        the DTC code string
     * @param description a human-readable description
     * @param statusByte  the raw UDS status byte
     * @param category    the DTC category
     */
    public DiagnosticTroubleCode(String code, String description, int statusByte, Category category) {
        this(code, description, statusByte, category, null);
    }

    /**
     * Determines the DTC category from the first character of a DTC code string.
     *
     * @param code the DTC code (e.g. "P0301")
     * @return the category
     * @throws IllegalArgumentException if the code prefix is not recognized
     */
    public static Category categoryFromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("DTC code must not be null or empty");
        }
        switch (Character.toUpperCase(code.charAt(0))) {
            case 'P': return Category.POWERTRAIN;
            case 'B': return Category.BODY;
            case 'C': return Category.CHASSIS;
            case 'U': return Category.NETWORK;
            default:
                throw new IllegalArgumentException("Unknown DTC category prefix: " + code.charAt(0));
        }
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /** Returns the DTC code string (e.g. "P0301"). */
    public String getCode() {
        return code;
    }

    /** Returns the human-readable description of the fault. */
    public String getDescription() {
        return description;
    }

    /** Returns the raw UDS status byte (ISO 14229 statusOfDTC). */
    public int getStatusByte() {
        return statusByte;
    }

    /** Returns the parsed DTC status flags. */
    public DtcStatus getStatus() {
        return DtcStatus.fromByte(statusByte);
    }

    /** Returns the DTC category (POWERTRAIN, BODY, CHASSIS, or NETWORK). */
    public Category getCategory() {
        return category;
    }

    /** Returns an unmodifiable map of freeze-frame data key-value pairs. */
    public Map<String, String> getFreezeFrameData() {
        return freezeFrameData;
    }

    /**
     * Returns {@code true} if the Malfunction Indicator Lamp (MIL) is requested
     * for this DTC.
     *
     * @return MIL status
     */
    public boolean isMilOn() {
        return getStatus().isWarningIndicatorRequested();
    }

    /**
     * Returns {@code true} if the DTC is currently confirmed (stored).
     *
     * @return confirmed status
     */
    public boolean isConfirmed() {
        return getStatus().isConfirmedDtc();
    }

    @Override
    public String toString() {
        DtcStatus status = getStatus();
        return String.format("DTC{code='%s', desc='%s', status=0x%02X [%s%s%s], category=%s}",
                code, description, statusByte,
                status.isConfirmedDtc() ? "CONFIRMED " : "",
                status.isPendingDtc() ? "PENDING " : "",
                status.isWarningIndicatorRequested() ? "MIL" : "",
                category);
    }
}
