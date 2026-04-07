package com.cardiag.core.session;

/**
 * Enumerates the UDS (Unified Diagnostic Services) session types defined by ISO 14229.
 *
 * <p>Each session type unlocks a different set of diagnostic capabilities:
 * <ul>
 *   <li>{@link #DEFAULT} - basic read-only diagnostics available without authentication</li>
 *   <li>{@link #PROGRAMMING} - ECU flashing / reprogramming session</li>
 *   <li>{@link #EXTENDED} - extended diagnostics including coding and adaptation</li>
 * </ul>
 */
public enum DiagnosticSession {

    /** Default diagnostic session (0x01). */
    DEFAULT(0x01),

    /** Programming session (0x02) used for ECU flashing. */
    PROGRAMMING(0x02),

    /** Extended diagnostic session (0x03) for coding, adaptation, and advanced operations. */
    EXTENDED(0x03);

    private final int sessionTypeByte;

    DiagnosticSession(int sessionTypeByte) {
        this.sessionTypeByte = sessionTypeByte;
    }

    /**
     * Returns the UDS sub-function byte for this session type.
     *
     * @return the session type byte as defined by ISO 14229
     */
    public int getSessionTypeByte() {
        return sessionTypeByte;
    }

    /**
     * Resolves a {@code DiagnosticSession} from its UDS byte value.
     *
     * @param value the session type byte
     * @return the matching {@code DiagnosticSession}
     * @throws IllegalArgumentException if no session matches the given byte
     */
    public static DiagnosticSession fromByte(int value) {
        for (DiagnosticSession session : values()) {
            if (session.sessionTypeByte == value) {
                return session;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown diagnostic session type: 0x%02X", value));
    }
}
