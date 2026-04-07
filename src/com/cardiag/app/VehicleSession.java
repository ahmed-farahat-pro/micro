package com.cardiag.app;

import com.cardiag.brand.Brand;
import com.cardiag.brand.VehicleProfile;
import com.cardiag.core.ecu.EcuConnection;
import com.cardiag.core.ecu.EcuDefinition;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Represents an active diagnostic session with a vehicle.
 * Manages the brand context, connected ECUs, active subsystems,
 * and session lifecycle.
 */
public class VehicleSession {

    private Brand brand;
    private VehicleProfile vehicleProfile;
    private DiagnosticProtocol protocol;
    private final Map<String, EcuConnection> connectedEcus;
    private final Map<SystemType, VehicleSystem> activeSystems;
    private LocalDateTime sessionStartTime;

    /**
     * Creates a new uninitialized vehicle session.
     */
    public VehicleSession() {
        this.connectedEcus = new LinkedHashMap<>();
        this.activeSystems = new LinkedHashMap<>();
    }

    /**
     * Initializes the session with brand, VIN, and protocol information.
     * Decodes the VIN to create a vehicle profile.
     *
     * @param brand    the vehicle brand
     * @param vin      the 17-character Vehicle Identification Number
     * @param protocol the diagnostic protocol to use
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void initialize(Brand brand, String vin, DiagnosticProtocol protocol) {
        if (brand == null) {
            throw new IllegalArgumentException("Brand must not be null");
        }
        if (vin == null || vin.length() != 17) {
            throw new IllegalArgumentException("VIN must be exactly 17 characters");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("Protocol must not be null");
        }

        this.brand = brand;
        this.protocol = protocol;
        this.vehicleProfile = brand.createVehicleProfile(vin);
        this.sessionStartTime = LocalDateTime.now();
        this.connectedEcus.clear();
        this.activeSystems.clear();
    }

    /**
     * Scans for all known ECUs defined by the current brand and attempts
     * to connect to each one. Successful connections are stored; failures
     * are silently skipped.
     *
     * @return a list of ECU names that responded successfully
     */
    public List<String> scanEcus() {
        if (brand == null || protocol == null) {
            throw new IllegalStateException("Session not initialized");
        }

        List<String> foundEcus = new ArrayList<>();
        Map<String, com.cardiag.core.ecu.EcuDefinition> ecuMap = brand.getEcuMap();

        for (Map.Entry<String, com.cardiag.core.ecu.EcuDefinition> entry : ecuMap.entrySet()) {
            String ecuName = entry.getKey();
            com.cardiag.core.ecu.EcuDefinition ecuDef = entry.getValue();
            try {
                EcuConnection conn = new EcuConnection(ecuDef, createProtocolForEcu());
                conn.connect();
                connectedEcus.put(ecuName, conn);
                foundEcus.add(ecuName);
            } catch (Exception e) {
                // ECU did not respond -- skip
            }
        }

        return foundEcus;
    }

    /**
     * Returns the vehicle system of the given type if it has been activated.
     *
     * @param type the system type to look up
     * @return the active {@link VehicleSystem}, or {@code null} if not active
     */
    public VehicleSystem getSystem(SystemType type) {
        return activeSystems.get(type);
    }

    /**
     * Connects to a specific ECU by name. If the ECU is already connected,
     * the existing connection is returned.
     *
     * @param ecuName the ECU name as defined in the brand's ECU map
     * @return the ECU connection
     * @throws IOException              if the connection fails
     * @throws IllegalArgumentException if the ECU name is unknown
     */
    public EcuConnection connectEcu(String ecuName) throws IOException {
        if (connectedEcus.containsKey(ecuName)) {
            return connectedEcus.get(ecuName);
        }

        if (brand == null) {
            throw new IllegalStateException("Session not initialized");
        }

        Map<String, com.cardiag.core.ecu.EcuDefinition> ecuMap = brand.getEcuMap();
        com.cardiag.core.ecu.EcuDefinition ecuDef = ecuMap.get(ecuName);
        if (ecuDef == null) {
            throw new IllegalArgumentException("Unknown ECU: " + ecuName);
        }

        EcuConnection conn = new EcuConnection(ecuDef, createProtocolForEcu());
        conn.connect();
        connectedEcus.put(ecuName, conn);
        return conn;
    }

    /**
     * Disconnects all active ECU connections and clears session state.
     */
    public void disconnectAll() {
        for (Map.Entry<String, EcuConnection> entry : connectedEcus.entrySet()) {
            try {
                if (entry.getValue().isConnected()) {
                    entry.getValue().disconnect();
                }
            } catch (IOException e) {
                // Best-effort disconnect
            }
        }
        connectedEcus.clear();
        activeSystems.clear();
    }

    /**
     * Returns a formatted summary of the current session state.
     *
     * @return the session summary string
     */
    public String getSessionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Session Summary ===\n");

        if (brand != null) {
            sb.append("  Brand:          ").append(brand.getName()).append("\n");
        }
        if (vehicleProfile != null) {
            sb.append("  VIN:            ").append(vehicleProfile.getVin()).append("\n");
            sb.append("  Model:          ").append(vehicleProfile.getModel()).append("\n");
            sb.append("  Year:           ").append(vehicleProfile.getYear()).append("\n");
            sb.append("  Platform:       ").append(vehicleProfile.getPlatform()).append("\n");
            sb.append("  Engine:         ").append(vehicleProfile.getEngineCode()).append("\n");
            sb.append("  Transmission:   ").append(vehicleProfile.getTransmissionType()).append("\n");
        }

        if (sessionStartTime != null) {
            sb.append("  Session Start:  ").append(
                    sessionStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            Duration elapsed = Duration.between(sessionStartTime, LocalDateTime.now());
            long minutes = elapsed.toMinutes();
            long seconds = elapsed.getSeconds() % 60;
            sb.append("  Session Time:   ").append(String.format("%dm %ds", minutes, seconds)).append("\n");
        }

        sb.append("  Connected ECUs: ").append(connectedEcus.size()).append("\n");
        if (!connectedEcus.isEmpty()) {
            for (String ecuName : connectedEcus.keySet()) {
                EcuConnection conn = connectedEcus.get(ecuName);
                String status = conn.isConnected() ? "ACTIVE" : "INACTIVE";
                sb.append("    - ").append(ecuName).append(" [").append(status).append("]\n");
            }
        }

        sb.append("  Active Systems: ").append(activeSystems.size()).append("\n");
        if (!activeSystems.isEmpty()) {
            for (SystemType type : activeSystems.keySet()) {
                sb.append("    - ").append(type.getDisplayName()).append("\n");
            }
        }

        return sb.toString();
    }

    // ── Getters ─────────────────────────────────────────────────────────

    public Brand getBrand() {
        return brand;
    }

    public VehicleProfile getVehicleProfile() {
        return vehicleProfile;
    }

    public DiagnosticProtocol getProtocol() {
        return protocol;
    }

    public Map<String, EcuConnection> getConnectedEcus() {
        return Collections.unmodifiableMap(connectedEcus);
    }

    public Map<SystemType, VehicleSystem> getActiveSystems() {
        return Collections.unmodifiableMap(activeSystems);
    }

    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }

    /**
     * Registers an active vehicle system in this session.
     *
     * @param type   the system type
     * @param system the vehicle system instance
     */
    public void addActiveSystem(SystemType type, VehicleSystem system) {
        activeSystems.put(type, system);
    }

    /**
     * Returns whether the session has been initialized.
     *
     * @return {@code true} if initialized
     */
    public boolean isInitialized() {
        return brand != null && vehicleProfile != null && protocol != null;
    }

    // ── Internal ────────────────────────────────────────────────────────

    /**
     * Creates a fresh protocol instance for a specific ECU connection.
     * In a real implementation this would configure addressing per-ECU.
     */
    private com.cardiag.core.ecu.DiagnosticProtocol createProtocolForEcu() {
        return new com.cardiag.core.ecu.DiagnosticProtocol() {
            private boolean connected = false;

            @Override
            public void connect(int targetId, int responseId) throws IOException {
                if (protocol != null) {
                    protocol.connect();
                }
                connected = true;
            }

            @Override
            public void disconnect() throws IOException {
                if (protocol != null && protocol.isConnected()) {
                    protocol.disconnect();
                }
                connected = false;
            }

            @Override
            public byte[] send(byte[] request) throws IOException {
                if (protocol == null) {
                    throw new IOException("No underlying protocol available");
                }
                protocol.sendRequest(request);
                return protocol.readResponse();
            }

            @Override
            public boolean isConnected() {
                return connected;
            }
        };
    }
}
