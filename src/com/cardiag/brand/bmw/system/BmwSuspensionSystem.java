package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.suspension.SuspensionSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW suspension system implementation using the EHC ECU.
 * Provides ride height adjustment, adaptive M suspension mode selection,
 * damper configuration, and sensor calibration.
 */
public class BmwSuspensionSystem extends SuspensionSystem {

    private static final String EHC_ID = "EHC";

    public BmwSuspensionSystem() {
        super();
        addEcu(new EcuDefinition(EHC_ID, "EHC - Electronic Height Control",
                BmwEcuMap.EHC_ADDRESS.getPhysicalId(), BmwEcuMap.EHC_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Ride Height Read/Adjust",
                "Adaptive M Suspension Mode Selection",
                "Damper Configuration",
                "Level Sensor Calibration",
                "Speed-Dependent Lowering",
                "Jack Mode Activation"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW EHC Air Suspension / Adaptive M Suspension - CAN 0x6C4/0x6CC";
    }

    @Override
    public Map<String, Double> readRideHeight() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x9000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> heights = new LinkedHashMap<>();
        if (response.length > 10) {
            heights.put("Front Left mm", extractSignedDouble(response, 3));
            heights.put("Front Right mm", extractSignedDouble(response, 5));
            heights.put("Rear Left mm", extractSignedDouble(response, 7));
            heights.put("Rear Right mm", extractSignedDouble(response, 9));
        }
        return heights;
    }

    @Override
    public void setRideHeight(String mode, double targetMm) throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte modeCode;
        switch (mode.toLowerCase()) {
            case "normal": modeCode = 0x00; break;
            case "lowered": modeCode = 0x01; break;
            case "raised": modeCode = 0x02; break;
            case "jack": modeCode = 0x03; break;
            default: modeCode = 0x00;
        }

        int targetEncoded = (int) (targetMm * 10);
        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0x90, 0x00,
                modeCode,
                (byte) ((targetEncoded >> 8) & 0xFF),
                (byte) (targetEncoded & 0xFF)
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readDampingConfig() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x9002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("Adaptive Damping", (response[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Sport Mode Firmness", decodeFirmness(response[4] & 0xFF));
            config.put("Comfort Mode Softness", decodeSoftness(response[5] & 0xFF));
        }
        return config;
    }

    @Override
    public void writeDampingConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte adaptive = "Enabled".equalsIgnoreCase(settings.get("Adaptive Damping")) ? (byte) 0x01 : 0x00;
        byte sportFirm = encodeFirmness(settings.getOrDefault("Sport Mode Firmness", "Medium"));
        byte comfortSoft = encodeSoftness(settings.getOrDefault("Comfort Mode Softness", "Soft"));

        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x90, 0x02, adaptive, sportFirm, comfortSoft});
        conn.getProtocol().readResponse();
    }

    @Override
    public void calibrateSensors() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Start sensor calibration routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0x90, 0x10});
        conn.getProtocol().readResponse();
    }

    @Override
    public String readSuspensionMode() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x9005));
        byte[] response = conn.getProtocol().readResponse();

        if (response.length > 3) {
            switch (response[3] & 0xFF) {
                case 0x00: return "Comfort";
                case 0x01: return "Normal";
                case 0x02: return "Sport";
                case 0x03: return "Sport+";
                default: return "Unknown (0x" + Integer.toHexString(response[3] & 0xFF) + ")";
            }
        }
        return "Unknown";
    }

    @Override
    public void setSuspensionMode(String mode) throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        byte modeCode;
        switch (mode.toLowerCase()) {
            case "comfort": modeCode = 0x00; break;
            case "normal": modeCode = 0x01; break;
            case "sport": modeCode = 0x02; break;
            case "sport+": modeCode = 0x03; break;
            default: modeCode = 0x01;
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x90, 0x05, modeCode});
        conn.getProtocol().readResponse();
    }

    private static double extractSignedDouble(byte[] data, int offset) {
        int raw = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        if (raw > 32767) raw -= 65536; // signed 16-bit
        return raw / 10.0;
    }

    private static String decodeFirmness(int val) {
        switch (val) {
            case 1: return "Soft";
            case 2: return "Medium";
            case 3: return "Firm";
            default: return "Unknown";
        }
    }

    private static String decodeSoftness(int val) {
        switch (val) {
            case 1: return "Soft";
            case 2: return "Medium";
            case 3: return "Firm";
            default: return "Unknown";
        }
    }

    private static byte encodeFirmness(String firmness) {
        switch (firmness.toLowerCase()) {
            case "soft": return 0x01;
            case "medium": return 0x02;
            case "firm": return 0x03;
            default: return 0x02;
        }
    }

    private static byte encodeSoftness(String softness) {
        switch (softness.toLowerCase()) {
            case "soft": return 0x01;
            case "medium": return 0x02;
            case "firm": return 0x03;
            default: return 0x01;
        }
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
