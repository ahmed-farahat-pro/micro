package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.transmission.TransmissionSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW transmission system implementation using the EGS ECU.
 * Provides adaptation reset, sport mode shift point configuration,
 * torque converter monitoring, and fluid level checks.
 */
public class BmwTransmissionSystem extends TransmissionSystem {

    private static final String EGS_ID = "EGS";

    public BmwTransmissionSystem() {
        super();
        addEcu(new EcuDefinition(EGS_ID, "EGS - Automatic Transmission",
                BmwEcuMap.EGS_ADDRESS.getPhysicalId(), BmwEcuMap.EGS_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Gear Adaptation Read/Reset",
                "Sport Mode Shift Point Adjustment",
                "Torque Converter Data Read",
                "Transmission Temperature Read",
                "Fluid Level Check",
                "Launch Control Configuration"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW EGS Transmission Control - 8HP/6HP automatic via CAN 0x6F1/0x6F9";
    }

    @Override
    public Map<String, Double> readGearAdaptation() throws IOException {
        EcuConnection conn = connections.get(EGS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2100));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> adaptations = new LinkedHashMap<>();
        adaptations.put("Clutch Pack A Fill Pressure", extractDouble(response, 3));
        adaptations.put("Clutch Pack B Fill Pressure", extractDouble(response, 5));
        adaptations.put("Shift Time 1-2", extractDouble(response, 7));
        adaptations.put("Shift Time 2-3", extractDouble(response, 9));
        adaptations.put("Shift Time 3-4", extractDouble(response, 11));
        adaptations.put("Torque Converter Slip", extractDouble(response, 13));
        return adaptations;
    }

    @Override
    public void resetGearAdaptation() throws IOException {
        EcuConnection conn = connections.get(EGS_ID);

        // Enter extended diagnostic session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        // Security access
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        // Routine control - reset all adaptation values
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x20, 0x00});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<Integer, Integer> readShiftPoints() throws IOException {
        EcuConnection conn = connections.get(EGS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2110));
        byte[] response = conn.getProtocol().readResponse();

        Map<Integer, Integer> shiftPoints = new LinkedHashMap<>();
        for (int gear = 1; gear <= 7; gear++) {
            int offset = 3 + ((gear - 1) * 2);
            if (offset + 1 < response.length) {
                int speed = ((response[offset] & 0xFF) << 8) | (response[offset + 1] & 0xFF);
                shiftPoints.put(gear, speed);
            }
        }
        return shiftPoints;
    }

    @Override
    public void adjustShiftPoints(Map<Integer, Integer> shiftPoints) throws IOException {
        if (shiftPoints == null || shiftPoints.isEmpty()) {
            throw new IllegalArgumentException("Shift points must not be null or empty");
        }

        EcuConnection conn = connections.get(EGS_ID);

        // Enter extended session and authenticate
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        // Write shift point data
        byte[] request = new byte[3 + shiftPoints.size() * 2];
        request[0] = 0x2E;
        request[1] = 0x21;
        request[2] = 0x10;
        int idx = 3;
        for (Map.Entry<Integer, Integer> entry : shiftPoints.entrySet()) {
            request[idx++] = (byte) ((entry.getValue() >> 8) & 0xFF);
            request[idx++] = (byte) (entry.getValue() & 0xFF);
        }
        conn.getProtocol().sendRequest(request);
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Double> readTorqueConverterData() throws IOException {
        EcuConnection conn = connections.get(EGS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2101));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Slip RPM", extractDouble(response, 3));
        data.put("Lockup Duty Cycle %", extractDouble(response, 5));
        data.put("Applied Pressure bar", extractDouble(response, 7));
        data.put("Turbine Speed RPM", extractDouble(response, 9));
        return data;
    }

    @Override
    public double readTransmissionTemp() throws IOException {
        EcuConnection conn = connections.get(EGS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2102));
        byte[] response = conn.getProtocol().readResponse();
        return extractDouble(response, 3);
    }

    @Override
    public String performFluidLevelCheck() throws IOException {
        EcuConnection conn = connections.get(EGS_ID);

        // Start fluid level check routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x20, 0x10});
        byte[] response = conn.getProtocol().readResponse();

        if (response.length > 4) {
            int status = response[4] & 0xFF;
            switch (status) {
                case 0x00: return "Fluid level OK - within normal range";
                case 0x01: return "Fluid level LOW - add approximately 0.5L";
                case 0x02: return "Fluid level HIGH - drain required";
                case 0x03: return "Temperature out of range - warm transmission to 40-50C and retry";
                default: return "Unable to determine fluid level - status code: 0x" + Integer.toHexString(status);
            }
        }
        return "No response from fluid level sensor";
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }

    private static double extractDouble(byte[] data, int offset) {
        if (data == null || offset + 1 >= data.length) return 0.0;
        return (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF)) / 100.0;
    }
}
