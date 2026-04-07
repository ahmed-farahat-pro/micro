package com.cardiag.protocol;

/**
 * Enumeration of supported diagnostic protocol types.
 * Each protocol type has a human-readable display name and description.
 */
public enum ProtocolType {

    /** Unified Diagnostic Services (ISO 14229) */
    UDS("UDS", "Unified Diagnostic Services (ISO 14229)"),

    /** Keyword Protocol 2000 (ISO 14230) */
    KWP2000("KWP2000", "Keyword Protocol 2000 (ISO 14230)"),

    /** On-Board Diagnostics II (ISO 15031 / SAE J1979) */
    OBD2("OBD-II", "On-Board Diagnostics II (ISO 15031 / SAE J1979)"),

    /** Raw CAN bus communication */
    CAN_RAW("CAN Raw", "Raw CAN bus communication without application-layer protocol");

    private final String displayName;
    private final String description;

    ProtocolType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name for this protocol type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a description of this protocol type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
