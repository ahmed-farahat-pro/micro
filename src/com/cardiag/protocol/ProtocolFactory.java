package com.cardiag.protocol;

import com.cardiag.protocol.can.CanBus;
import com.cardiag.protocol.kwp.Kwp2000Protocol;
import com.cardiag.protocol.obd.ObdProtocol;
import com.cardiag.protocol.uds.UdsProtocol;

/**
 * Factory for creating {@link DiagnosticProtocol} instances based on the
 * requested {@link ProtocolType}.
 */
public final class ProtocolFactory {

    /** Default UDS transmit CAN ID. */
    private static final int DEFAULT_UDS_TX_ID = 0x7E0;

    /** Default UDS receive CAN ID. */
    private static final int DEFAULT_UDS_RX_ID = 0x7E8;

    private ProtocolFactory() {
        // utility class
    }

    /**
     * Creates a {@link DiagnosticProtocol} for the specified type using the
     * provided CAN bus for transport.
     *
     * @param type   the desired protocol type
     * @param canBus the CAN bus transport to use
     * @return a new protocol instance
     * @throws IllegalArgumentException if the type is {@code null} or unsupported
     */
    public static DiagnosticProtocol createProtocol(ProtocolType type, CanBus canBus) {
        if (type == null) {
            throw new IllegalArgumentException("Protocol type must not be null");
        }
        if (canBus == null) {
            throw new IllegalArgumentException("CAN bus must not be null");
        }

        switch (type) {
            case UDS:
                return new UdsProtocol(canBus, DEFAULT_UDS_TX_ID, DEFAULT_UDS_RX_ID);
            case KWP2000:
                return new Kwp2000Protocol(canBus);
            case OBD2:
                return new ObdProtocol(canBus);
            case CAN_RAW:
                throw new IllegalArgumentException(
                        "CAN_RAW does not have an application-layer protocol. "
                                + "Use the CanBus interface directly.");
            default:
                throw new IllegalArgumentException("Unsupported protocol type: " + type);
        }
    }
}
