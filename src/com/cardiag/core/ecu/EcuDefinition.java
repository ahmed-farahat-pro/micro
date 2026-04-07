package com.cardiag.core.ecu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class describing the static definition of an Electronic Control Unit (ECU).
 *
 * <p>Each concrete subclass defines a specific ECU type (e.g. engine, transmission, ABS)
 * including its addressing, supported diagnostic services, coding blocks, and
 * adaptation channels.</p>
 *
 * <p>The {@code protocolType} string indicates the diagnostic protocol variant
 * (e.g. "UDS", "KWP2000", "TP2.0").</p>
 */
public abstract class EcuDefinition {

    private final String name;
    private final EcuAddress ecuAddress;
    private final String protocolType;
    private final List<Integer> supportedServices;
    private final Map<String, byte[]> codingBlocks;
    private final Map<Integer, String> adaptationChannels;

    /**
     * Constructs a new {@code EcuDefinition}.
     *
     * @param name               the human-readable ECU name (e.g. "Engine Control Module")
     * @param ecuAddress          the addressing information for this ECU
     * @param protocolType       the diagnostic protocol identifier (e.g. "UDS")
     * @param supportedServices  list of supported UDS service IDs (e.g. 0x10, 0x22, 0x2E)
     * @param codingBlocks       map of coding block name to default coding bytes
     * @param adaptationChannels map of channel ID to channel description
     */
    protected EcuDefinition(String name,
                            EcuAddress ecuAddress,
                            String protocolType,
                            List<Integer> supportedServices,
                            Map<String, byte[]> codingBlocks,
                            Map<Integer, String> adaptationChannels) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.ecuAddress = Objects.requireNonNull(ecuAddress, "ecuAddress must not be null");
        this.protocolType = Objects.requireNonNull(protocolType, "protocolType must not be null");
        this.supportedServices = List.copyOf(Objects.requireNonNull(supportedServices, "supportedServices must not be null"));
        this.codingBlocks = Map.copyOf(Objects.requireNonNull(codingBlocks, "codingBlocks must not be null"));
        this.adaptationChannels = Map.copyOf(Objects.requireNonNull(adaptationChannels, "adaptationChannels must not be null"));
    }

    // ── Abstract methods ────────────────────────────────────────────────

    /**
     * Returns the default coding map for this ECU definition, describing all
     * coding parameters and their factory-default values.
     *
     * @return an unmodifiable map of coding parameter names to their default integer values
     */
    public abstract Map<String, Integer> getDefaultCodingMap();

    /**
     * Returns the minimum security level required to perform write operations
     * (coding, adaptation, flashing) on this ECU.
     *
     * @return the security level as an integer (e.g. 0x01, 0x03, 0x61)
     */
    public abstract int getSecurityLevel();

    /**
     * Returns a list of Data Identifier (DID) numbers used to read the ECU's
     * identification information (part number, software version, etc.).
     *
     * @return an unmodifiable list of DID values
     */
    public abstract List<Integer> getIdentificationDids();

    // ── Concrete getters ────────────────────────────────────────────────

    /**
     * Returns the human-readable name of this ECU.
     *
     * @return the ECU name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the addressing information for this ECU.
     *
     * @return the {@link EcuAddress}
     */
    public EcuAddress getAddress() {
        return ecuAddress;
    }

    /**
     * Returns the diagnostic protocol type identifier.
     *
     * @return the protocol type string (e.g. "UDS", "KWP2000")
     */
    public String getProtocolType() {
        return protocolType;
    }

    /**
     * Returns the list of UDS service IDs supported by this ECU.
     *
     * @return an unmodifiable list of service IDs
     */
    public List<Integer> getSupportedServices() {
        return supportedServices;
    }

    /**
     * Returns the coding blocks defined for this ECU.
     *
     * @return an unmodifiable map of coding block name to default coding bytes
     */
    public Map<String, byte[]> getCodingBlocks() {
        return codingBlocks;
    }

    /**
     * Returns the adaptation channels defined for this ECU.
     *
     * @return an unmodifiable map of channel ID to description
     */
    public Map<Integer, String> getAdaptationChannels() {
        return adaptationChannels;
    }

    /**
     * Checks whether the ECU supports the given UDS service ID.
     *
     * @param serviceId the service ID to check
     * @return {@code true} if the service is supported
     */
    public boolean supportsService(int serviceId) {
        return supportedServices.contains(serviceId);
    }

    @Override
    public String toString() {
        return String.format("EcuDefinition{name='%s', address=%s, protocol='%s', services=%d, codingBlocks=%d, adaptationChannels=%d}",
                name, ecuAddress, protocolType,
                supportedServices.size(), codingBlocks.size(), adaptationChannels.size());
    }
}
