package com.cardiag.brand.citroen;

import com.cardiag.brand.Brand;
import com.cardiag.brand.VehicleProfile;
import com.cardiag.core.ecu.EcuDefinition;
import com.cardiag.core.security.SeedKeyAlgorithm;
import com.cardiag.system.SystemType;

import com.cardiag.brand.citroen.ecu.*;

import java.util.*;

/**
 * Citroen brand implementation for the PSA/Stellantis platform.
 *
 * <p>Supports models including C3, C4, C5, C5 Aircross, Berlingo,
 * SpaceTourer, C4 Cactus, and e-C4. Uses the PSA DiagBox-style
 * diagnostic protocol suite with BSI-centric body control architecture.</p>
 */
public class CitroenBrand implements Brand {

    private static final String BRAND_NAME = "Citro\u00ebn";

    private final CitroenSeedKeyAlgorithm seedKeyAlgorithm;
    private final Map<String, EcuDefinition> ecuMap;

    public CitroenBrand() {
        this.seedKeyAlgorithm = new CitroenSeedKeyAlgorithm();
        this.ecuMap = buildEcuMap();
    }

    @Override
    public String getName() {
        return BRAND_NAME;
    }

    @Override
    public List<SystemType> getSupportedSystems() {
        return List.of(
                SystemType.ENGINE,
                SystemType.TRANSMISSION,
                SystemType.BODY_CONTROL,
                SystemType.COMFORT,
                SystemType.LIGHTING,
                SystemType.SECURITY,
                SystemType.CLIMATE,
                SystemType.SUSPENSION,
                SystemType.STEERING,
                SystemType.BRAKES,
                SystemType.INSTRUMENT_CLUSTER,
                SystemType.INFOTAINMENT,
                SystemType.ADAS,
                SystemType.CHARGING
        );
    }

    @Override
    public Map<String, EcuDefinition> getEcuMap() {
        return Collections.unmodifiableMap(ecuMap);
    }

    @Override
    public SeedKeyAlgorithm getSeedKeyAlgorithm() {
        return seedKeyAlgorithm;
    }

    @Override
    public VehicleProfile createVehicleProfile(String vin) {
        if (vin == null || vin.length() != 17) {
            throw new IllegalArgumentException("VIN must be exactly 17 characters");
        }
        CitroenVehicleProfile profile = new CitroenVehicleProfile();
        profile.decodeVin(vin);
        return profile;
    }

    /**
     * Returns the PSA/Stellantis platform identifier.
     *
     * @return "PSA/Stellantis"
     */
    public String getPlatform() {
        return "PSA/Stellantis";
    }

    /**
     * Returns the list of supported Citroen models.
     *
     * @return an unmodifiable list of model names
     */
    public List<String> getSupportedModels() {
        return List.of(
                "C1", "C3", "C3 Aircross", "C4", "C4 Cactus",
                "C4 X", "e-C4", "C5", "C5 Aircross", "C5 X",
                "Berlingo", "SpaceTourer", "Jumpy", "Jumper"
        );
    }

    private Map<String, EcuDefinition> buildEcuMap() {
        Map<String, EcuDefinition> map = new LinkedHashMap<>();
        map.put("ECM", new CitroenEcm());
        map.put("BSI", new CitroenBsi());
        map.put("BEM", new CitroenBem());
        map.put("CMB", new CitroenCmb());
        map.put("SMEG", new CitroenSmeg());
        map.put("CLIM", new CitroenClim());
        map.put("ABS", new CitroenAbs());
        map.put("EPS", new CitroenEps());
        map.put("AMVAR", new CitroenAmvar());
        map.put("DAEP", new CitroenDaep());
        return map;
    }
}
