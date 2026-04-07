package com.cardiag.brand;

import com.cardiag.core.ecu.EcuDefinition;
import com.cardiag.core.security.SeedKeyAlgorithm;
import com.cardiag.system.SystemType;

import java.util.List;
import java.util.Map;

/**
 * Represents a vehicle brand/manufacturer within the diagnostic tool.
 * Each brand provides its own ECU map, supported systems, security algorithms,
 * and VIN-based vehicle profile creation.
 */
public interface Brand {

    /**
     * Returns the brand name (e.g. "BMW", "Mercedes-Benz").
     *
     * @return the brand name
     */
    String getName();

    /**
     * Returns the list of vehicle subsystem types supported by this brand.
     *
     * @return an unmodifiable list of supported system types
     */
    List<SystemType> getSupportedSystems();

    /**
     * Returns a map of ECU names to their definitions for this brand.
     *
     * @return an unmodifiable map of ECU name to {@link EcuDefinition}
     */
    Map<String, EcuDefinition> getEcuMap();

    /**
     * Returns the seed/key algorithm used by this brand for security access.
     *
     * @return the {@link SeedKeyAlgorithm} implementation
     */
    SeedKeyAlgorithm getSeedKeyAlgorithm();

    /**
     * Creates a vehicle profile by decoding the given VIN.
     *
     * @param vin the 17-character Vehicle Identification Number
     * @return a brand-specific {@link VehicleProfile}
     * @throws IllegalArgumentException if the VIN is invalid
     */
    VehicleProfile createVehicleProfile(String vin);
}
