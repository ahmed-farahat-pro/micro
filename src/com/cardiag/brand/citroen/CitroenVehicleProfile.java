package com.cardiag.brand.citroen;

import com.cardiag.brand.VehicleProfile;

import java.util.*;

/**
 * Citroen-specific vehicle profile that decodes PSA VIN structure.
 *
 * <p>PSA VIN layout (17 characters):
 * <ul>
 *   <li>Positions 1-3: WMI (World Manufacturer Identifier) - VF7 for Citroen France</li>
 *   <li>Position 4: Vehicle type/platform code</li>
 *   <li>Positions 5-6: Model variant</li>
 *   <li>Position 7: Body style</li>
 *   <li>Position 8: Engine type</li>
 *   <li>Position 9: Transmission</li>
 *   <li>Position 10: Model year</li>
 *   <li>Position 11: Assembly plant</li>
 *   <li>Positions 12-17: Sequential number</li>
 * </ul>
 */
public class CitroenVehicleProfile extends VehicleProfile {

    private String bodyStyle;
    private String assemblyPlant;
    private String sequentialNumber;

    public CitroenVehicleProfile() {
        super();
    }

    @Override
    public void decodeVin(String vin) {
        if (vin == null || vin.length() != 17) {
            throw new IllegalArgumentException("VIN must be exactly 17 characters");
        }
        this.vin = vin.toUpperCase();
        this.manufacturer = "Citro\u00ebn";

        decodeWmi(this.vin.substring(0, 3));
        decodePlatform(this.vin.charAt(3));
        decodeModelVariant(this.vin.substring(4, 6));
        decodeBodyStyle(this.vin.charAt(6));
        decodeEngineType(this.vin.charAt(7));
        decodeTransmission(this.vin.charAt(8));
        decodeModelYear(this.vin.charAt(9));
        decodeAssemblyPlant(this.vin.charAt(10));
        this.sequentialNumber = this.vin.substring(11, 17);

        detectInstalledOptions();
    }

    /**
     * Returns the decoded body style description.
     *
     * @return body style string
     */
    public String getBodyStyle() {
        return bodyStyle;
    }

    /**
     * Returns the assembly plant code description.
     *
     * @return assembly plant string
     */
    public String getAssemblyPlant() {
        return assemblyPlant;
    }

    /**
     * Returns the sequential production number.
     *
     * @return the 6-digit sequential number
     */
    public String getSequentialNumber() {
        return sequentialNumber;
    }

    private void decodeWmi(String wmi) {
        switch (wmi) {
            case "VF7":
                // Citroen manufactured in France
                break;
            case "VR7":
                // Citroen manufactured in France (post-2019)
                break;
            case "W0L":
                // Citroen manufactured in Spain (Vigo)
                break;
            case "VF7":
                break;
            default:
                // Other assembly locations
                break;
        }
    }

    private void decodePlatform(char code) {
        switch (code) {
            case 'A':
                this.platform = "PF1";
                break;
            case 'B':
                this.platform = "PF2";
                break;
            case 'C':
                this.platform = "CMP";
                break;
            case 'D':
                this.platform = "EMP2";
                break;
            case 'E':
                this.platform = "EMP2 (extended)";
                break;
            case 'S':
                this.platform = "e-CMP";
                break;
            default:
                this.platform = "Unknown (" + code + ")";
                break;
        }
    }

    private void decodeModelVariant(String code) {
        Map<String, String> models = new HashMap<>();
        models.put("SC", "C3");
        models.put("SH", "C3 Aircross");
        models.put("DA", "C4");
        models.put("DB", "C4 Cactus");
        models.put("DC", "C4 X");
        models.put("DE", "e-C4");
        models.put("RD", "C5 Aircross");
        models.put("RE", "C5 X");
        models.put("RW", "C5");
        models.put("BA", "Berlingo");
        models.put("BB", "Berlingo Van");
        models.put("VA", "SpaceTourer");
        models.put("XA", "Jumpy");
        models.put("YA", "Jumper");
        models.put("SA", "C1");

        this.model = models.getOrDefault(code, "Unknown (" + code + ")");
    }

    private void decodeBodyStyle(char code) {
        switch (code) {
            case 'A':
                this.bodyStyle = "5-door hatchback";
                break;
            case 'B':
                this.bodyStyle = "3-door hatchback";
                break;
            case 'C':
                this.bodyStyle = "Sedan";
                break;
            case 'D':
                this.bodyStyle = "Estate/Wagon";
                break;
            case 'E':
                this.bodyStyle = "SUV/Crossover";
                break;
            case 'F':
                this.bodyStyle = "MPV";
                break;
            case 'G':
                this.bodyStyle = "Van";
                break;
            case 'H':
                this.bodyStyle = "Cabriolet";
                break;
            default:
                this.bodyStyle = "Unknown (" + code + ")";
                break;
        }
    }

    private void decodeEngineType(char code) {
        switch (code) {
            case 'A':
                this.engineCode = "EB2 1.2 PureTech 82";
                break;
            case 'B':
                this.engineCode = "EB2DT 1.2 PureTech 110";
                break;
            case 'C':
                this.engineCode = "EB2ADTS 1.2 PureTech 130";
                break;
            case 'D':
                this.engineCode = "EP6FDTM 1.6 THP 165";
                break;
            case 'E':
                this.engineCode = "EP6FADTX 1.6 THP 200";
                break;
            case 'H':
                this.engineCode = "DV5RC 1.5 BlueHDi 100";
                break;
            case 'J':
                this.engineCode = "DV5RD 1.5 BlueHDi 130";
                break;
            case 'K':
                this.engineCode = "DW10FC 2.0 BlueHDi 150";
                break;
            case 'L':
                this.engineCode = "DW10FD 2.0 BlueHDi 180";
                break;
            case 'S':
                this.engineCode = "Electric Motor 136";
                break;
            default:
                this.engineCode = "Unknown (" + code + ")";
                break;
        }
    }

    private void decodeTransmission(char code) {
        switch (code) {
            case '5':
                this.transmissionType = "5-speed manual";
                break;
            case '6':
                this.transmissionType = "6-speed manual";
                break;
            case 'A':
                this.transmissionType = "AL4 4-speed automatic";
                break;
            case 'E':
                this.transmissionType = "EAT6 6-speed automatic";
                break;
            case 'T':
                this.transmissionType = "EAT8 8-speed automatic";
                break;
            case 'R':
                this.transmissionType = "e-CMP single-speed reduction";
                break;
            default:
                this.transmissionType = "Unknown (" + code + ")";
                break;
        }
    }

    private void decodeModelYear(char code) {
        Map<Character, Integer> yearMap = new HashMap<>();
        yearMap.put('A', 2010);
        yearMap.put('B', 2011);
        yearMap.put('C', 2012);
        yearMap.put('D', 2013);
        yearMap.put('E', 2014);
        yearMap.put('F', 2015);
        yearMap.put('G', 2016);
        yearMap.put('H', 2017);
        yearMap.put('J', 2018);
        yearMap.put('K', 2019);
        yearMap.put('L', 2020);
        yearMap.put('M', 2021);
        yearMap.put('N', 2022);
        yearMap.put('P', 2023);
        yearMap.put('R', 2024);
        yearMap.put('S', 2025);

        this.year = yearMap.getOrDefault(code, 0);
    }

    private void decodeAssemblyPlant(char code) {
        switch (code) {
            case 'P':
                this.assemblyPlant = "Poissy, France";
                break;
            case 'M':
                this.assemblyPlant = "Mulhouse, France";
                break;
            case 'R':
                this.assemblyPlant = "Rennes, France";
                break;
            case 'V':
                this.assemblyPlant = "Vigo, Spain";
                break;
            case 'T':
                this.assemblyPlant = "Trnava, Slovakia";
                break;
            case 'W':
                this.assemblyPlant = "Wuhan, China";
                break;
            case 'K':
                this.assemblyPlant = "Kolin, Czech Republic";
                break;
            default:
                this.assemblyPlant = "Unknown (" + code + ")";
                break;
        }
    }

    private void detectInstalledOptions() {
        // Detect standard options based on platform and model
        if ("EMP2".equals(platform) || "EMP2 (extended)".equals(platform)) {
            installedOptions.add("EMP2 platform features");
            installedOptions.add("AMVAR Progressive Hydraulic Cushions");
            installedOptions.add("Advanced Comfort seats");
        }
        if ("CMP".equals(platform) || "e-CMP".equals(platform)) {
            installedOptions.add("CMP platform features");
        }
        if (engineCode != null && engineCode.contains("Electric")) {
            installedOptions.add("Electric drivetrain");
            installedOptions.add("Regenerative braking");
            installedOptions.add("Heat pump climate");
        }
        if (transmissionType != null && transmissionType.contains("EAT8")) {
            installedOptions.add("EAT8 Aisin 8-speed automatic");
        }
    }
}
