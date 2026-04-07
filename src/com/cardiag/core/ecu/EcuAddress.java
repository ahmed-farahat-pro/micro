package com.cardiag.core.ecu;

import java.util.Objects;

/**
 * Represents the addressing information for an Electronic Control Unit (ECU).
 *
 * <p>Each ECU on a diagnostic bus is identified by three IDs:
 * <ul>
 *   <li><b>physicalId</b> - the unique address used for point-to-point (physical) requests</li>
 *   <li><b>functionalId</b> - the group/broadcast address used for functional requests</li>
 *   <li><b>responseId</b> - the CAN ID on which the ECU sends its responses</li>
 * </ul>
 */
public final class EcuAddress {

    private final int physicalId;
    private final int functionalId;
    private final int responseId;
    private final String name;

    /**
     * Constructs a new {@code EcuAddress}.
     *
     * @param physicalId   the physical (point-to-point) request ID
     * @param functionalId the functional (broadcast) request ID
     * @param responseId   the response ID
     * @param name         a human-readable name for this address (e.g. "Engine ECU")
     */
    public EcuAddress(int physicalId, int functionalId, int responseId, String name) {
        this.physicalId = physicalId;
        this.functionalId = functionalId;
        this.responseId = responseId;
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    // ---- Static factory methods ----

    /**
     * Creates an {@code EcuAddress} with the standard OBD-II offset convention where
     * the response ID is {@code physicalId + 0x08}.
     *
     * @param physicalId   the physical request ID
     * @param functionalId the functional request ID
     * @param name         a human-readable name
     * @return a new {@code EcuAddress}
     */
    public static EcuAddress withStandardOffset(int physicalId, int functionalId, String name) {
        return new EcuAddress(physicalId, functionalId, physicalId + 0x08, name);
    }

    /**
     * Creates an {@code EcuAddress} using the standard OBD-II functional ID {@code 0x7DF}.
     *
     * @param physicalId the physical request ID
     * @param name       a human-readable name
     * @return a new {@code EcuAddress}
     */
    public static EcuAddress ofStandardObd(int physicalId, String name) {
        return withStandardOffset(physicalId, 0x7DF, name);
    }

    /**
     * Creates an {@code EcuAddress} for an extended (29-bit) CAN identifier set.
     *
     * @param physicalId   the physical request ID (29-bit)
     * @param functionalId the functional request ID (29-bit)
     * @param responseId   the response ID (29-bit)
     * @param name         a human-readable name
     * @return a new {@code EcuAddress}
     */
    public static EcuAddress ofExtended(int physicalId, int functionalId, int responseId, String name) {
        return new EcuAddress(physicalId, functionalId, responseId, name);
    }

    /**
     * Creates an {@code EcuAddress} with only a physical ID and response ID
     * (no functional addressing).
     *
     * @param physicalId the physical request ID
     * @param responseId the response ID
     * @param name       a human-readable name
     * @return a new {@code EcuAddress}
     */
    public static EcuAddress ofPhysicalOnly(int physicalId, int responseId, String name) {
        return new EcuAddress(physicalId, 0x0000, responseId, name);
    }

    // ---- Getters ----

    /** Returns the physical (point-to-point) request ID. */
    public int getPhysicalId() {
        return physicalId;
    }

    /** Returns the functional (broadcast) request ID. */
    public int getFunctionalId() {
        return functionalId;
    }

    /** Returns the response ID. */
    public int getResponseId() {
        return responseId;
    }

    /** Returns the human-readable name for this address. */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EcuAddress)) return false;
        EcuAddress that = (EcuAddress) o;
        return physicalId == that.physicalId
                && functionalId == that.functionalId
                && responseId == that.responseId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicalId, functionalId, responseId);
    }

    @Override
    public String toString() {
        return String.format("EcuAddress{name='%s', physical=0x%04X, functional=0x%04X, response=0x%04X}",
                name, physicalId, functionalId, responseId);
    }
}
