package com.cardiag.system;

/**
 * Enumerates the major vehicle subsystem types supported by the diagnostic tool.
 * Each constant carries a human-readable display name and a short description
 * of the subsystem's role within the vehicle.
 */
public enum SystemType {

    /** Powertrain / engine management. */
    ENGINE("Engine", "Engine management and powertrain control"),

    /** Automatic or manual transmission control. */
    TRANSMISSION("Transmission", "Transmission and gearbox control"),

    /** Body control module functions. */
    BODY_CONTROL("Body Control", "Central body control module functions"),

    /** Comfort and convenience features. */
    COMFORT("Comfort", "Comfort and convenience feature control"),

    /** Interior and exterior lighting. */
    LIGHTING("Lighting", "Interior and exterior lighting control"),

    /** Immobilizer, alarm, and keyless entry. */
    SECURITY("Security", "Vehicle security and immobilizer systems"),

    /** Heating, ventilation, and air-conditioning. */
    CLIMATE("Climate", "Climate control and HVAC systems"),

    /** Active or air suspension. */
    SUSPENSION("Suspension", "Suspension and ride-height control"),

    /** Power steering and active steering. */
    STEERING("Steering", "Steering assist and active steering systems"),

    /** ABS, ESP, and brake management. */
    BRAKES("Brakes", "Brake system and stability control"),

    /** Instrument cluster / driver information display. */
    INSTRUMENT_CLUSTER("Instrument Cluster", "Instrument cluster and driver information display"),

    /** Head unit, navigation, and media. */
    INFOTAINMENT("Infotainment", "Infotainment, navigation, and media systems"),

    /** Advanced driver-assistance systems. */
    ADAS("ADAS", "Advanced driver-assistance systems"),

    /** Charging, battery management, and energy recovery. */
    CHARGING("Charging", "Charging, battery management, and energy recovery");

    private final String displayName;
    private final String description;

    /**
     * Creates a new system type constant.
     *
     * @param displayName short human-readable label
     * @param description brief explanation of the subsystem's purpose
     */
    SystemType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name for this system type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a brief description of this system type's purpose.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName + " - " + description;
    }
}
