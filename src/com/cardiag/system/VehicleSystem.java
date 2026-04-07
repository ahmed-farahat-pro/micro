package com.cardiag.system;

import com.cardiag.protocol.DiagnosticProtocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for every vehicle subsystem that can be diagnosed
 * or programmed.  Concrete subsystem classes (engine, transmission, etc.)
 * extend this class and implement the subsystem-specific operations.
 *
 * <p>Each {@code VehicleSystem} holds a list of {@link EcuDefinition}s that
 * belong to it and manages the corresponding {@link EcuConnection}s.</p>
 */
public abstract class VehicleSystem {

    /** The type of vehicle subsystem represented by this instance. */
    protected SystemType systemType;

    /** ECU definitions that belong to this subsystem. */
    protected final List<EcuDefinition> ecuDefinitions;

    /** Active connections keyed by ECU identifier. */
    protected final Map<String, EcuConnection> connections;

    /**
     * Creates a new vehicle system with empty ECU lists and connections.
     */
    protected VehicleSystem() {
        this.ecuDefinitions = new ArrayList<>();
        this.connections = new HashMap<>();
    }

    // ------------------------------------------------------------------
    // Abstract methods
    // ------------------------------------------------------------------

    /**
     * Initializes the subsystem using the given diagnostic protocol.
     * Implementations should set up ECU definitions and perform any
     * protocol-specific handshake or session establishment.
     *
     * @param protocol the diagnostic protocol to use
     * @throws IOException if initialization fails
     */
    public abstract void initialize(DiagnosticProtocol protocol) throws IOException;

    /**
     * Returns the list of capabilities supported by this subsystem.
     *
     * @return an unmodifiable list of capability descriptions
     */
    public abstract List<String> getCapabilities();

    /**
     * Returns a human-readable summary of the subsystem's current state.
     *
     * @return system information string
     */
    public abstract String getSystemInfo();

    // ------------------------------------------------------------------
    // Concrete methods
    // ------------------------------------------------------------------

    /**
     * Registers an ECU definition with this subsystem.
     *
     * @param ecu the ECU definition to add
     * @throws IllegalArgumentException if {@code ecu} is {@code null}
     */
    public void addEcu(EcuDefinition ecu) {
        if (ecu == null) {
            throw new IllegalArgumentException("ECU definition must not be null");
        }
        ecuDefinitions.add(ecu);
    }

    /**
     * Opens connections to all registered ECUs using the supplied protocol.
     *
     * @param protocol the diagnostic protocol to use for each connection
     * @throws IOException if any connection fails
     */
    public void connectAll(DiagnosticProtocol protocol) throws IOException {
        for (EcuDefinition ecu : ecuDefinitions) {
            EcuConnection conn = new EcuConnection(ecu, protocol);
            conn.connect();
            connections.put(ecu.getEcuId(), conn);
        }
    }

    /**
     * Closes all active ECU connections and clears the connection map.
     *
     * @throws IOException if any disconnection fails
     */
    public void disconnectAll() throws IOException {
        for (EcuConnection conn : connections.values()) {
            if (conn.isActive()) {
                conn.disconnect();
            }
        }
        connections.clear();
    }

    /**
     * Returns the system type of this vehicle subsystem.
     *
     * @return the {@link SystemType}
     */
    public SystemType getSystemType() {
        return systemType;
    }
}
