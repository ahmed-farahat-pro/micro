package com.cardiag.system;

/**
 * Describes a single ECU within a vehicle subsystem.
 * Contains addressing information and metadata used to locate and
 * communicate with the ECU on the diagnostic bus.
 */
public class EcuDefinition {

    private final String ecuId;
    private final String name;
    private final int logicalAddress;
    private final int physicalAddress;

    /**
     * Creates a new ECU definition.
     *
     * @param ecuId           unique identifier for this ECU
     * @param name            human-readable name
     * @param logicalAddress  logical (functional) address on the bus
     * @param physicalAddress physical address on the bus
     */
    public EcuDefinition(String ecuId, String name, int logicalAddress, int physicalAddress) {
        this.ecuId = ecuId;
        this.name = name;
        this.logicalAddress = logicalAddress;
        this.physicalAddress = physicalAddress;
    }

    /** @return the unique ECU identifier */
    public String getEcuId() {
        return ecuId;
    }

    /** @return the human-readable ECU name */
    public String getName() {
        return name;
    }

    /** @return the logical (functional) bus address */
    public int getLogicalAddress() {
        return logicalAddress;
    }

    /** @return the physical bus address */
    public int getPhysicalAddress() {
        return physicalAddress;
    }

    @Override
    public String toString() {
        return name + " [" + ecuId + "] (0x"
                + Integer.toHexString(logicalAddress) + "/0x"
                + Integer.toHexString(physicalAddress) + ")";
    }
}
