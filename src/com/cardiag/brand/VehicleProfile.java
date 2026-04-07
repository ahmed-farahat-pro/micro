package com.cardiag.brand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class representing a decoded vehicle profile derived from a VIN.
 * Brand-specific subclasses implement {@link #decodeVin(String)} to populate
 * manufacturer-specific fields.
 */
public abstract class VehicleProfile {

    protected String vin;
    protected String manufacturer;
    protected String model;
    protected int year;
    protected String platform;
    protected String engineCode;
    protected String transmissionType;
    protected List<String> installedOptions;

    /**
     * Creates a new vehicle profile with default values.
     */
    protected VehicleProfile() {
        this.installedOptions = new ArrayList<>();
    }

    /**
     * Decodes the given VIN and populates all profile fields.
     *
     * @param vin the 17-character Vehicle Identification Number
     * @throws IllegalArgumentException if the VIN is invalid or cannot be decoded
     */
    public abstract void decodeVin(String vin);

    // ── Getters ─────────────────────────────────────────────────────────

    public String getVin() {
        return vin;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public String getPlatform() {
        return platform;
    }

    public String getEngineCode() {
        return engineCode;
    }

    public String getTransmissionType() {
        return transmissionType;
    }

    public List<String> getInstalledOptions() {
        return Collections.unmodifiableList(installedOptions);
    }

    @Override
    public String toString() {
        return String.format("VehicleProfile{vin='%s', manufacturer='%s', model='%s', year=%d, " +
                        "platform='%s', engineCode='%s', transmissionType='%s', options=%s}",
                vin, manufacturer, model, year, platform, engineCode, transmissionType, installedOptions);
    }
}
