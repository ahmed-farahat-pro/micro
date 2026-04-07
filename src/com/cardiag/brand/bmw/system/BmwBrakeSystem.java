package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.brakes.BrakeSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW brake system implementation using the DSC ECU.
 * Provides ABS/ESP configuration, brake bleed routines, CBS brake pad reset,
 * parking brake calibration, fluid level read, and brake pre-fill control.
 */
public class BmwBrakeSystem extends BrakeSystem {

    private static final String DSC_ID = "DSC";

    public BmwBrakeSystem() {
        super();
        addEcu(new EcuDefinition(DSC_ID, "DSC - Dynamic Stability Control",
                BmwEcuMap.DSC_ADDRESS.getPhysicalId(), BmwEcuMap.DSC_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "ABS Configuration Read",
                "ESP Configuration Read/Set",
                "Brake Bleed Routine (per circuit)",
                "CBS Brake Pad Wear Reset",
                "Brake Pad Status Read",
                "Electronic Parking Brake Calibration",
                "Brake Fluid Level Read",
                "Brake Pre-Fill Toggle"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW DSC Brake System - ABS/ESP/EPB via CAN 0x6C2/0x6CA";
    }

    @Override
    public Map<String, String> readAbsConfig() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 6) {
            config.put("ABS Status", (response[3] & 0x01) != 0 ? "OK" : "Fault");
            config.put("Sensor FL", (response[4] & 0x01) != 0 ? "OK" : "Fault");
            config.put("Sensor FR", (response[4] & 0x02) != 0 ? "OK" : "Fault");
            config.put("Sensor RL", (response[4] & 0x04) != 0 ? "OK" : "Fault");
            config.put("Sensor RR", (response[4] & 0x08) != 0 ? "OK" : "Fault");
            config.put("Hydraulic Unit", (response[5] & 0x01) != 0 ? "OK" : "Fault");
            config.put("Pressure Sensor", (response[6] & 0x01) != 0 ? "OK" : "Fault");
        }
        return config;
    }

    @Override
    public Map<String, String> readEspConfig() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 7) {
            config.put("DSC Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("DTC Mode Available", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("CBC Active", (response[4] & 0x01) != 0 ? "Yes" : "No");
            config.put("DBC Active", (response[4] & 0x02) != 0 ? "Yes" : "No");
            config.put("Hill Start Assist", (response[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Brake Drying", (response[5] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Brake Standby", (response[5] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("Fading Compensation", (response[6] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("EPB Active", (response[7] & 0x01) != 0 ? "Yes" : "No");
            config.put("EPB Auto Hold", (response[7] & 0x02) != 0 ? "Enabled" : "Disabled");
        }
        return config;
    }

    @Override
    public void setEspMode(String mode) throws IOException {
        if (mode == null) {
            throw new IllegalArgumentException("Mode must not be null");
        }

        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte modeCode;
        switch (mode.toLowerCase()) {
            case "on": modeCode = 0x01; break;
            case "sport": case "dtc": modeCode = 0x02; break;
            case "off": modeCode = 0x00; break;
            default: throw new IllegalArgumentException("Unknown ESP mode: " + mode);
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x70, 0x01, modeCode});
        conn.getProtocol().readResponse();
    }

    @Override
    public void performBrakeBleed(int circuit) throws IOException {
        if (circuit < 1 || circuit > 2) {
            throw new IllegalArgumentException("Circuit must be 1 or 2, got: " + circuit);
        }

        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Start brake bleed routine for specified circuit
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x70, 0x10, (byte) circuit});
        conn.getProtocol().readResponse();
    }

    @Override
    public void resetBrakePadWear() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Reset CBS brake pad counters - front and rear
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x70, 0x20, 0x01}); // Front
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x70, 0x20, 0x02}); // Rear
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Integer> readBrakePadStatus() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Integer> status = new LinkedHashMap<>();
        if (response.length > 6) {
            status.put("Front Left %", response[3] & 0xFF);
            status.put("Front Right %", response[4] & 0xFF);
            status.put("Rear Left %", response[5] & 0xFF);
            status.put("Rear Right %", response[6] & 0xFF);
        }
        return status;
    }

    @Override
    public void calibrateParkingBrake() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // EPB calibration routine: release, reset, and re-apply
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x70, 0x30});
        conn.getProtocol().readResponse();
    }

    @Override
    public String readBrakeFluidLevel() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7020));
        byte[] response = conn.getProtocol().readResponse();

        if (response.length > 3) {
            switch (response[3] & 0xFF) {
                case 0x00: return "OK";
                case 0x01: return "Low";
                case 0x02: return "Critical - service immediately";
                default: return "Unknown";
            }
        }
        return "Unknown";
    }

    @Override
    public void setPrefillBrakes(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x70, 0x02, val});
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
