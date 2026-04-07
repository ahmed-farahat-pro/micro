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
 * Provides camera calibration, radar calibration, lane departure warning,
 * forward collision warning, adaptive cruise control, and parking assist.
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
                "Radar Calibration",
                "Lane Departure Warning Toggle",
                "Forward Collision Warning Configuration",
                "Adaptive Cruise Control Toggle",
                "Parking Assist Configuration Read",
                "Calibration Status Read"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW KAFAS Camera-Based ADAS - CAN 0x6D0/0x6D8";
    }

    @Override
    public Map<String, String> readCameraCalibration() throws IOException {
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
    public void calibrateCamera() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Start camera calibration routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0xB0, 0x10});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readRadarConfig() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xB010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("Radar Installed", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Radar Calibrated", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("Range m", String.valueOf(response[4] & 0xFF));
            config.put("ACC Available", (response[5] & 0x01) != 0 ? "Yes" : "No");
        }
        return config;
    }

    @Override
    public void setLaneDepartureWarning(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0xB0, 0x01, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setCollisionWarning(boolean enabled, int sensitivity) throws IOException {
        if (sensitivity < 1 || sensitivity > 3) {
            throw new IllegalArgumentException("Sensitivity must be 1-3, got: " + sensitivity);
        }

        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte enableFlag = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0xB0, 0x02, enableFlag, (byte) sensitivity
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public void setAdaptiveCruise(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0xB0, 0x03, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readParkingAssistConfig() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xB020));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("PDC Front", (response[3] & 0x01) != 0 ? "Installed" : "Not Installed");
            config.put("PDC Rear", (response[3] & 0x02) != 0 ? "Installed" : "Not Installed");
            config.put("Parking Assistant", (response[4] & 0x01) != 0 ? "Active" : "Inactive");
            config.put("Surround View", (response[4] & 0x02) != 0 ? "Active" : "Inactive");
            config.put("Rear Camera", (response[5] & 0x01) != 0 ? "Active" : "Inactive");
        }
        return config;
    }

    @Override
    public void calibrateRadar() throws IOException {
        EcuConnection conn = connections.get(KAFAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Start radar calibration routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0xB0, 0x11});
        conn.getProtocol().readResponse();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

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
