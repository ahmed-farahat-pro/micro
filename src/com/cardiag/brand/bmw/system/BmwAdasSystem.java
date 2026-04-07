package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.adas.AdasSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW ADAS system implementation using the KAFAS camera module.
 * Provides camera calibration, lane departure warning sensitivity
 * adjustment, forward collision warning configuration, and traffic
 * sign recognition settings.
 */
public class BmwAdasSystem extends AdasSystem {

    private static final String KAFAS_ID = "KAFAS";

    public BmwAdasSystem() {
        super();
        addEcu(new EcuDefinition(KAFAS_ID, "KAFAS - Camera ADAS Module",
                BmwEcuMap.KAFAS_ADDRESS.getPhysicalId(), BmwEcuMap.KAFAS_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Camera Calibration",
                "Lane Departure Warning Configuration",
                "Forward Collision Warning Configuration",
                "City Braking Toggle",
                "Pedestrian Warning Toggle",
                "Traffic Sign Recognition Toggle",
                "High Beam Assistant Toggle",
                "Calibration Status Read"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW KAFAS Camera-Based ADAS - CAN 0x6D0/0x6D8";
    }

    @Override
    public Map<String, String> readAdasConfig() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xB001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 8) {
            config.put("Lane Departure Warning", (response[3] & 0x01) != 0 ? "Active" : "Inactive");
            config.put("LDW Sensitivity", decodeSensitivity(response[4] & 0xFF));
            config.put("LDW Warning Type", (response[5] & 0x01) == 0 ? "Visual" : "Steering Vibration");
            config.put("Forward Collision Warning", (response[5] & 0x02) != 0 ? "Active" : "Inactive");
            config.put("FCW Sensitivity", decodeSensitivity(response[6] & 0xFF));
            config.put("City Braking", (response[7] & 0x01) != 0 ? "Active" : "Inactive");
            config.put("Pedestrian Warning", (response[7] & 0x02) != 0 ? "Active" : "Inactive");
            config.put("Traffic Sign Recognition", (response[7] & 0x04) != 0 ? "Active" : "Inactive");
            config.put("High Beam Assistant", (response[8] & 0x01) != 0 ? "Active" : "Inactive");
        }
        return config;
    }

    @Override
    public void writeAdasConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte ldwFlag = "Active".equalsIgnoreCase(settings.get("Lane Departure Warning")) ? (byte) 0x01 : 0x00;
        byte ldwSens = encodeSensitivity(settings.getOrDefault("LDW Sensitivity", "Medium"));

        byte warningFlags = 0;
        if ("Steering Vibration".equalsIgnoreCase(settings.get("LDW Warning Type"))) warningFlags |= 0x01;
        if ("Active".equalsIgnoreCase(settings.get("Forward Collision Warning"))) warningFlags |= 0x02;

        byte fcwSens = encodeSensitivity(settings.getOrDefault("FCW Sensitivity", "Medium"));

        byte featureFlags = 0;
        if ("Active".equalsIgnoreCase(settings.get("City Braking"))) featureFlags |= 0x01;
        if ("Active".equalsIgnoreCase(settings.get("Pedestrian Warning"))) featureFlags |= 0x02;
        if ("Active".equalsIgnoreCase(settings.get("Traffic Sign Recognition"))) featureFlags |= 0x04;

        byte hba = "Active".equalsIgnoreCase(settings.get("High Beam Assistant")) ? (byte) 0x01 : 0x00;

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0xB0, 0x01,
                ldwFlag, ldwSens, warningFlags, fcwSens, featureFlags, hba
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public void calibrateCamera() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Start camera calibration routine
        // Vehicle must be on a level surface facing a calibration target
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0xB0, 0x10});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readCalibrationStatus() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xB000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 6) {
            status.put("Calibration Status", (response[3] & 0x01) != 0 ? "Calibrated" : "Not Calibrated");
            status.put("Vertical Offset deg", String.format("%.2f", ((response[4] & 0xFF) - 128) / 100.0));
            status.put("Horizontal Offset deg", String.format("%.2f", ((response[5] & 0xFF) - 128) / 100.0));
            status.put("Calibration Quality", decodeQuality(response[6] & 0xFF));
        }
        return status;
    }

    @Override
    public Map<String, String> readSensorStatus() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xB002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 5) {
            status.put("Camera Status", (response[3] & 0x01) != 0 ? "OK" : "Fault");
            status.put("Lens Clean", (response[3] & 0x02) != 0 ? "Yes" : "Blocked/Dirty");
            status.put("Temperature OK", (response[4] & 0x01) != 0 ? "Yes" : "Overheated");
            status.put("Firmware Version", String.format("%d.%d", response[5] & 0xFF,
                    response.length > 6 ? response[6] & 0xFF : 0));
        }
        return status;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String decodeSensitivity(int val) {
        switch (val) {
            case 0x01: return "Low";
            case 0x02: return "Medium";
            case 0x03: return "High";
            default: return "Unknown";
        }
    }

    private static byte encodeSensitivity(String sens) {
        switch (sens.toLowerCase()) {
            case "low": return 0x01;
            case "medium": return 0x02;
            case "high": return 0x03;
            default: return 0x02;
        }
    }

    private static String decodeQuality(int val) {
        if (val >= 90) return "Excellent";
        if (val >= 70) return "Good";
        if (val >= 50) return "Acceptable";
        return "Poor - recalibration recommended";
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
