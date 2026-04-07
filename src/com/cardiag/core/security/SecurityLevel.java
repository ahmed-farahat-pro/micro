package com.cardiag.core.security;

/**
 * Represents the UDS security access levels used during the seed-key authentication
 * handshake (service 0x27).
 *
 * <p>Each level corresponds to a specific access-level byte that is sent as the
 * sub-function in the SecurityAccess request. Odd values are seed requests;
 * even values (odd + 1) are key responses.
 */
public enum SecurityLevel {

    /** ECU is locked; no security access has been granted. */
    LOCKED(0x00),

    /** Standard security level 1 (seed request 0x01, key send 0x02). */
    LEVEL_1(0x01),

    /** Standard security level 2 (seed request 0x03, key send 0x04). */
    LEVEL_2(0x03),

    /** Standard security level 3 (seed request 0x05, key send 0x06). */
    LEVEL_3(0x05),

    /** Engineering / development security level (seed request 0x61, key send 0x62). */
    ENGINEERING(0x61);

    private final int accessLevel;

    SecurityLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    /**
     * Returns the access-level byte used in the UDS SecurityAccess seed request.
     *
     * @return the access level byte
     */
    public int getAccessLevel() {
        return accessLevel;
    }

    /**
     * Returns the sub-function byte used to send the computed key back to the ECU
     * (always {@code accessLevel + 1}).
     *
     * @return the key-send sub-function byte
     * @throws UnsupportedOperationException if the level is {@link #LOCKED}
     */
    public int getKeySendByte() {
        if (this == LOCKED) {
            throw new UnsupportedOperationException("LOCKED level has no key-send byte");
        }
        return accessLevel + 1;
    }

    /**
     * Resolves a {@code SecurityLevel} from its access-level byte value.
     *
     * @param value the access level byte
     * @return the matching {@code SecurityLevel}
     * @throws IllegalArgumentException if no level matches the given byte
     */
    public static SecurityLevel fromAccessLevel(int value) {
        for (SecurityLevel level : values()) {
            if (level.accessLevel == value) {
                return level;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown security access level: 0x%02X", value));
    }
}
