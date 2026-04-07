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
 * damper configuration, sensor calibration, and air spring pressure reads.
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
                "Self-Leveling Configuration",
                "Air Spring Pressure Read"
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
            heights.put("Front Left mm", extractSigned(response, 3));
            heights.put("Front Right mm", extractSigned(response, 5));
            heights.put("Rear Left mm", extractSigned(response, 7));
            heights.put("Rear Right mm", extractSigned(response, 9));
        }
        return heights;
    }

    @Override
    public void setRideHeight(int mm) throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        int encoded = mm * 10;
        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0x90, 0x00,
                (byte) ((encoded >> 8) & 0xFF),
                (byte) (encoded & 0xFF)
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public String readDampingMode() throws IOException {
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
    public void setDampingMode(String mode) throws IOException {
        if (mode == null) {
            throw new IllegalArgumentException("Mode must not be null");
        }

        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        byte modeCode;
        switch (mode.toLowerCase()) {
            case "comfort": modeCode = 0x00; break;
            case "normal": modeCode = 0x01; break;
            case "sport": modeCode = 0x02; break;
            case "sport+": modeCode = 0x03; break;
            default: throw new IllegalArgumentException("Unknown damping mode: " + mode);
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x90, 0x05, modeCode});
        conn.getProtocol().readResponse();
    }

    @Override
    public void calibrateSuspension() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Start full suspension calibration routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0x90, 0x10});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readSelfLevelingStatus() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x9010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 5) {
            status.put("Self-Leveling Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            status.put("Load Compensation", (response[3] & 0x02) != 0 ? "Active" : "Inactive");
            status.put("Speed-Dependent Lowering", (response[4] & 0x01) != 0 ? "Active" : "Inactive");
            status.put("Lowering Speed Threshold km/h", String.valueOf(response[5] & 0xFF));
        }
        return status;
    }

    @Override
    public void setSelfLeveling(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x90, 0x10, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Double> readAirSpringPressures() throws IOException {
        EcuConnection conn = connections.get(EHC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x9001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> pressures = new LinkedHashMap<>();
        if (response.length > 10) {
            pressures.put("Front Left bar", extractUnsigned(response, 3));
            pressures.put("Front Right bar", extractUnsigned(response, 5));
            pressures.put("Rear Left bar", extractUnsigned(response, 7));
            pressures.put("Rear Right bar", extractUnsigned(response, 9));
        }
        return pressures;
    }

    private static double extractSigned(byte[] data, int offset) {
        int raw = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        if (raw > 32767) raw -= 65536;
        return raw / 10.0;
    }

    private static double extractUnsigned(byte[] data, int offset) {
        int raw = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        return raw / 100.0;
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
