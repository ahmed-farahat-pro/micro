package com.cardiag.brand.bmw;

import com.cardiag.brand.Brand;
import com.cardiag.brand.VehicleProfile;
import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;
import com.cardiag.core.security.SeedKeyAlgorithm;
import com.cardiag.brand.bmw.ecu.*;
import com.cardiag.system.SystemType;

import java.util.*;

/**
 * BMW brand implementation. Registers all BMW ECUs and systems, and provides
 * BMW-specific diagnostic configuration. Supports E-series, F-series, and
 * G-series vehicles.
 */
public class BmwBrand implements Brand {

    private static final String NAME = "BMW";
    private final Map<String, EcuDefinition> ecuMap;
    private final BmwSeedKeyAlgorithm seedKeyAlgorithm;
    private final List<SystemType> supportedSystems;

    public BmwBrand() {
        this.seedKeyAlgorithm = new BmwSeedKeyAlgorithm();
        this.ecuMap = initializeEcuMap();
        this.supportedSystems = initializeSupportedSystems();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<SystemType> getSupportedSystems() {
        return Collections.unmodifiableList(supportedSystems);
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
        BmwVehicleProfile profile = new BmwVehicleProfile();
        profile.decodeVin(vin);
        return profile;
    }

    private Map<String, EcuDefinition> initializeEcuMap() {
        Map<String, EcuDefinition> map = new LinkedHashMap<>();
        map.put("DME", new BmwDme());
        map.put("EGS", new BmwEgs());
        map.put("CAS", new BmwCas());
        map.put("FRM", new BmwFrm());
        map.put("KOMBI", new BmwKombi());
        map.put("CIC", new BmwCic());
        map.put("IHKA", new BmwIhka());
        map.put("DSC", new BmwDsc());
        map.put("ELV", new BmwElv());
        map.put("EHC", new BmwEhc());
        map.put("SZL", new BmwSzl());
        map.put("KAFAS", new BmwKafas());
        return map;
    }

    private List<SystemType> initializeSupportedSystems() {
        return Arrays.asList(
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
}
