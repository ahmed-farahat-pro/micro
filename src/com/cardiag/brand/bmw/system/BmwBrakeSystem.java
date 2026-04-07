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
 * Provides DSC coding, brake bleed routines, CBS brake pad reset,
 * and parking brake calibration.
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
                "DSC Configuration Read/Write",
                "ABS Status Read",
                "Brake Bleed Routine (per corner)",
                "CBS Brake Pad Wear Reset",
                "Electronic Parking Brake Calibration",
                "Hill Start Assist Configuration",
                "Brake Data Read"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW DSC Brake System - ABS/ESP/EPB via CAN 0x6C2/0x6CA";
    }

    @Override
    public Map<String, String> readBrakeConfig() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 7) {
            config.put("DSC Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("DTC Mode Available", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("ABS Active", (response[3] & 0x04) != 0 ? "Yes" : "No");
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
    public void writeBrakeConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte dscFlags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("DSC Active"))) dscFlags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("DTC Mode Available"))) dscFlags |= 0x02;
        if ("Yes".equalsIgnoreCase(settings.get("ABS Active"))) dscFlags |= 0x04;

        byte brakeFlags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("CBC Active"))) brakeFlags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("DBC Active"))) brakeFlags |= 0x02;

        byte assistFlags = 0;
        if ("Enabled".equalsIgnoreCase(settings.get("Hill Start Assist"))) assistFlags |= 0x01;
        if ("Enabled".equalsIgnoreCase(settings.get("Brake Drying"))) assistFlags |= 0x02;
        if ("Enabled".equalsIgnoreCase(settings.get("Brake Standby"))) assistFlags |= 0x04;

        byte fadeFlag = "Enabled".equalsIgnoreCase(settings.get("Fading Compensation")) ? (byte) 0x01 : 0x00;

        byte epbFlags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("EPB Active"))) epbFlags |= 0x01;
        if ("Enabled".equalsIgnoreCase(settings.get("EPB Auto Hold"))) epbFlags |= 0x02;

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x70, 0x00,
                dscFlags, brakeFlags, assistFlags, fadeFlag, epbFlags
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public void performBrakeBleed(String corner) throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte cornerCode;
        switch (corner.toUpperCase()) {
            case "FL": case "FRONT_LEFT": cornerCode = 0x01; break;
            case "FR": case "FRONT_RIGHT": cornerCode = 0x02; break;
            case "RL": case "REAR_LEFT": cornerCode = 0x03; break;
            case "RR": case "REAR_RIGHT": cornerCode = 0x04; break;
            case "ALL": cornerCode = 0x00; break;
            default:
                throw new IllegalArgumentException("Unknown corner: " + corner);
        }

        // Start brake bleed routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x70, 0x10, cornerCode});
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
    public Map<String, Double> readBrakeData() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> data = new LinkedHashMap<>();
        if (response.length > 12) {
            data.put("Brake Pressure bar", extractDouble(response, 3));
            data.put("Wheel Speed FL km/h", extractDouble(response, 5));
            data.put("Wheel Speed FR km/h", extractDouble(response, 7));
            data.put("Wheel Speed RL km/h", extractDouble(response, 9));
            data.put("Wheel Speed RR km/h", extractDouble(response, 11));
        }
        return data;
    }

    @Override
    public Map<String, String> readAbsStatus() throws IOException {
        EcuConnection conn = connections.get(DSC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x7001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 6) {
            status.put("ABS Status", (response[3] & 0x01) != 0 ? "OK" : "Fault");
            status.put("DSC Status", (response[3] & 0x02) != 0 ? "OK" : "Fault");
            status.put("Sensor FL", (response[4] & 0x01) != 0 ? "OK" : "Fault");
            status.put("Sensor FR", (response[4] & 0x02) != 0 ? "OK" : "Fault");
            status.put("Sensor RL", (response[4] & 0x04) != 0 ? "OK" : "Fault");
            status.put("Sensor RR", (response[4] & 0x08) != 0 ? "OK" : "Fault");
            status.put("Hydraulic Unit", (response[5] & 0x01) != 0 ? "OK" : "Fault");
            status.put("Pressure Sensor", (response[6] & 0x01) != 0 ? "OK" : "Fault");
        }
        return status;
    }

    private static double extractDouble(byte[] data, int offset) {
        if (data == null || offset + 1 >= data.length) return 0.0;
        return (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF)) / 100.0;
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
