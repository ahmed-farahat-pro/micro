package com.cardiag.brand.bmw;

import com.cardiag.brand.VehicleProfile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * BMW-specific vehicle profile that decodes a BMW VIN to extract chassis code,
 * engine variant, production plant, and model year. Supports E-series, F-series,
 * G-series, and U-series vehicles.
 */
public class BmwVehicleProfile extends VehicleProfile {

    private String chassisCode;
    private String chassisGeneration;
    private String productionPlant;

    // VIN position 4-5 maps to chassis code
    private static final Map<String, String> CHASSIS_CODE_MAP = new HashMap<>();
    // VIN position 7 maps to engine variant
    private static final Map<Character, String> ENGINE_VARIANT_MAP = new HashMap<>();
    // VIN position 11 maps to production plant
    private static final Map<Character, String> PLANT_MAP = new HashMap<>();
    // VIN position 10 maps to model year
    private static final Map<Character, Integer> YEAR_MAP = new HashMap<>();

    static {
        // Chassis codes (VIN positions 4-5)
        CHASSIS_CODE_MAP.put("VA", "E90");
        CHASSIS_CODE_MAP.put("VB", "E91");
        CHASSIS_CODE_MAP.put("VC", "E92");
        CHASSIS_CODE_MAP.put("VD", "E93");
        CHASSIS_CODE_MAP.put("PM", "E60");
        CHASSIS_CODE_MAP.put("PN", "E61");
        CHASSIS_CODE_MAP.put("FE", "E70");
        CHASSIS_CODE_MAP.put("FF", "E71");
        CHASSIS_CODE_MAP.put("3A", "F30");
        CHASSIS_CODE_MAP.put("3B", "F31");
        CHASSIS_CODE_MAP.put("3C", "F34");
        CHASSIS_CODE_MAP.put("4A", "F32");
        CHASSIS_CODE_MAP.put("4B", "F33");
        CHASSIS_CODE_MAP.put("4C", "F36");
        CHASSIS_CODE_MAP.put("5A", "G30");
        CHASSIS_CODE_MAP.put("5B", "G31");
        CHASSIS_CODE_MAP.put("7A", "G11");
        CHASSIS_CODE_MAP.put("7B", "G12");
        CHASSIS_CODE_MAP.put("8A", "G20");
        CHASSIS_CODE_MAP.put("8B", "G21");
        CHASSIS_CODE_MAP.put("8C", "G80");
        CHASSIS_CODE_MAP.put("UA", "U06");
        CHASSIS_CODE_MAP.put("UB", "U11");

        // Engine variants (VIN position 7)
        ENGINE_VARIANT_MAP.put('A', "N20B20");
        ENGINE_VARIANT_MAP.put('B', "N55B30");
        ENGINE_VARIANT_MAP.put('C', "N52B30");
        ENGINE_VARIANT_MAP.put('D', "N54B30");
        ENGINE_VARIANT_MAP.put('E', "N63B44");
        ENGINE_VARIANT_MAP.put('F', "B48B20");
        ENGINE_VARIANT_MAP.put('G', "B58B30");
        ENGINE_VARIANT_MAP.put('H', "S58B30");
        ENGINE_VARIANT_MAP.put('J', "N47D20");
        ENGINE_VARIANT_MAP.put('K', "B47D20");
        ENGINE_VARIANT_MAP.put('L', "N57D30");
        ENGINE_VARIANT_MAP.put('M', "S55B30");

        // Production plants (VIN position 11)
        PLANT_MAP.put('A', "Munich");
        PLANT_MAP.put('B', "Dingolfing");
        PLANT_MAP.put('C', "Regensburg");
        PLANT_MAP.put('E', "Eisenach");
        PLANT_MAP.put('F', "Graz (Magna Steyr)");
        PLANT_MAP.put('G', "Spartanburg");
        PLANT_MAP.put('H', "Rosslyn");
        PLANT_MAP.put('J', "Leipzig");
        PLANT_MAP.put('K', "Cairo");
        PLANT_MAP.put('L', "Shenyang");
        PLANT_MAP.put('N', "Tiexi");
        PLANT_MAP.put('P', "San Luis Potosi");
        PLANT_MAP.put('W', "Chennai");

        // Model year codes (VIN position 10)
        YEAR_MAP.put('A', 2010);
        YEAR_MAP.put('B', 2011);
        YEAR_MAP.put('C', 2012);
        YEAR_MAP.put('D', 2013);
        YEAR_MAP.put('E', 2014);
        YEAR_MAP.put('F', 2015);
        YEAR_MAP.put('G', 2016);
        YEAR_MAP.put('H', 2017);
        YEAR_MAP.put('J', 2018);
        YEAR_MAP.put('K', 2019);
        YEAR_MAP.put('L', 2020);
        YEAR_MAP.put('M', 2021);
        YEAR_MAP.put('N', 2022);
        YEAR_MAP.put('P', 2023);
        YEAR_MAP.put('R', 2024);
        YEAR_MAP.put('S', 2025);
    }

    @Override
    public void decodeVin(String vin) {
        if (vin == null || vin.length() != 17) {
            throw new IllegalArgumentException("VIN must be exactly 17 characters, got: " + vin);
        }

        this.vin = vin.toUpperCase();
        this.manufacturer = "BMW";

        // WMI (World Manufacturer Identifier) - positions 1-3
        String wmi = this.vin.substring(0, 3);
        if (!wmi.startsWith("WBA") && !wmi.startsWith("WBS") && !wmi.startsWith("WBY")
                && !wmi.startsWith("4US") && !wmi.startsWith("5UX") && !wmi.startsWith("WBW")) {
            throw new IllegalArgumentException("VIN does not appear to be a BMW: " + wmi);
        }

        if (wmi.startsWith("WBS")) {
            this.manufacturer = "BMW M";
        } else if (wmi.startsWith("WBY")) {
            this.manufacturer = "BMW i";
        }

        // Chassis code - positions 4-5
        String chassisKey = this.vin.substring(3, 5);
        this.chassisCode = CHASSIS_CODE_MAP.getOrDefault(chassisKey, "Unknown");
        this.platform = this.chassisCode;

        // Determine chassis generation
        if (this.chassisCode.startsWith("E")) {
            this.chassisGeneration = "E-Series";
        } else if (this.chassisCode.startsWith("F")) {
            this.chassisGeneration = "F-Series";
        } else if (this.chassisCode.startsWith("G")) {
            this.chassisGeneration = "G-Series";
        } else if (this.chassisCode.startsWith("U")) {
            this.chassisGeneration = "U-Series";
        } else {
            this.chassisGeneration = "Unknown";
        }

        // Determine model name from chassis code
        this.model = resolveModel(this.chassisCode);

        // Engine variant - position 7 (index 6)
        char engineChar = this.vin.charAt(6);
        this.engineCode = ENGINE_VARIANT_MAP.getOrDefault(engineChar, "Unknown");

        // Transmission - position 6 (index 5): even = automatic, odd = manual
        char transChar = this.vin.charAt(5);
        this.transmissionType = Character.isDigit(transChar) && (transChar - '0') % 2 == 0
                ? "Automatic" : "Manual";

        // Model year - position 10 (index 9)
        char yearChar = this.vin.charAt(9);
        this.year = YEAR_MAP.getOrDefault(yearChar, 0);

        // Production plant - position 11 (index 10)
        char plantChar = this.vin.charAt(10);
        this.productionPlant = PLANT_MAP.getOrDefault(plantChar, "Unknown");

        // Installed options based on known decodings
        this.installedOptions = Arrays.asList(
                "BMW ConnectedDrive",
                "iDrive Navigation",
                "Park Distance Control"
        );
    }

    /**
     * Returns the decoded BMW chassis code (e.g. E90, F30, G20).
     *
     * @return the chassis code
     */
    public String getChassisCode() {
        return chassisCode;
    }

    /**
     * Returns the chassis generation identifier: "E-Series", "F-Series",
     * "G-Series", or "U-Series".
     *
     * @return the chassis generation
     */
    public String getChassisGeneration() {
        return chassisGeneration;
    }

    /**
     * Returns the production plant name.
     *
     * @return the production plant
     */
    public String getProductionPlant() {
        return productionPlant;
    }

    private static String resolveModel(String chassisCode) {
        switch (chassisCode) {
            case "E90": case "F30": case "G20": return "3 Series Sedan";
            case "E91": case "F31": case "G21": return "3 Series Touring";
            case "E92": case "F32": return "4 Series Coupe";
            case "E93": case "F33": return "4 Series Convertible";
            case "F34": return "3 Series Gran Turismo";
            case "F36": return "4 Series Gran Coupe";
            case "E60": case "G30": return "5 Series Sedan";
            case "E61": case "G31": return "5 Series Touring";
            case "E70": return "X5";
            case "E71": return "X6";
            case "G11": return "7 Series (SWB)";
            case "G12": return "7 Series (LWB)";
            case "G80": return "M3";
            case "U06": return "iX";
            case "U11": return "iX1";
            default: return "Unknown Model";
        }
    }
}
