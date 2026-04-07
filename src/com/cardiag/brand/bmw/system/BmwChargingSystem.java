package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.charging.ChargingSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW charging and battery management system implementation.
 * Uses the DME ECU for IBS (Intelligent Battery Sensor) access,
 * alternator coding, and regenerative braking configuration.
 */
public class BmwChargingSystem extends ChargingSystem {

    private static final String DME_ID = "DME";

    public BmwChargingSystem() {
        super();
        addEcu(new EcuDefinition(DME_ID, "DME - Digital Motor Electronics",
                BmwEcuMap.DME_ADDRESS.getPhysicalId(), BmwEcuMap.DME_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Battery Status Read (IBS)",
                "Battery Registration",
                "Alternator Configuration",
                "Regenerative Braking Configuration",
                "Charging History Read",
                "Energy Recovery Data Read"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW IBS Battery Management / Alternator / Regenerative Braking via DME 0x7E0/0x7E8";
    }

    @Override
    public Map<String, String> readBatteryStatus() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2500));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 10) {
            int voltage = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
            status.put("Voltage V", String.format("%.2f", voltage / 1000.0));

            int current = ((response[5] & 0xFF) << 8) | (response[6] & 0xFF);
            if (current > 32767) current -= 65536; // signed
            status.put("Current A", String.format("%.2f", current / 100.0));

            status.put("State of Charge %", String.valueOf(response[7] & 0xFF));
            status.put("State of Health %", String.valueOf(response[8] & 0xFF));

            int temp = response[9] & 0xFF;
            status.put("Temperature C", String.valueOf(temp - 40));

            status.put("Battery Registered", (response[10] & 0x01) != 0 ? "Yes" : "No");
        }
        return status;
    }

    @Override
    public void registerBattery(String partNumber, int capacityAh) throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        // BMW battery registration: write part number and capacity
        byte[] pnBytes = partNumber.getBytes();
        byte[] request = new byte[5 + pnBytes.length];
        request[0] = 0x2E;
        request[1] = 0x25;
        request[2] = 0x10;
        request[3] = (byte) ((capacityAh >> 8) & 0xFF);
        request[4] = (byte) (capacityAh & 0xFF);
        System.arraycopy(pnBytes, 0, request, 5, pnBytes.length);

        conn.getProtocol().sendRequest(request);
        conn.getProtocol().readResponse();

        // Reset IBS learned values after battery registration
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x25, 0x01});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readAlternatorConfig() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2510));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 6) {
            int targetVoltage = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
            config.put("Target Voltage V", String.format("%.2f", targetVoltage / 1000.0));
            config.put("Intelligent Charging", (response[5] & 0x01) != 0 ? "Active" : "Inactive");
            config.put("Regenerative Mode", (response[5] & 0x02) != 0 ? "Active" : "Inactive");
            config.put("Load Response %", String.valueOf(response[6] & 0xFF));
        }
        return config;
    }

    @Override
    public void writeAlternatorConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        // Parse target voltage (e.g. "14.40" -> 14400)
        int targetVoltage = 14400;
        if (settings.containsKey("Target Voltage V")) {
            targetVoltage = (int) (Double.parseDouble(settings.get("Target Voltage V")) * 1000);
        }

        byte chargeFlags = 0;
        if ("Active".equalsIgnoreCase(settings.get("Intelligent Charging"))) chargeFlags |= 0x01;
        if ("Active".equalsIgnoreCase(settings.get("Regenerative Mode"))) chargeFlags |= 0x02;

        int loadResponse = 100;
        if (settings.containsKey("Load Response %")) {
            loadResponse = Integer.parseInt(settings.get("Load Response %"));
        }

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x25, 0x10,
                (byte) ((targetVoltage >> 8) & 0xFF),
                (byte) (targetVoltage & 0xFF),
                chargeFlags,
                (byte) (loadResponse & 0xFF)
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readChargingHistory() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2520));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> history = new LinkedHashMap<>();
        if (response.length > 10) {
            int totalChargeAh = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
            history.put("Total Charge Throughput Ah", String.valueOf(totalChargeAh));

            int deepDischarges = response[5] & 0xFF;
            history.put("Deep Discharge Events", String.valueOf(deepDischarges));

            int batteryAge = ((response[6] & 0xFF) << 8) | (response[7] & 0xFF);
            history.put("Battery Age days", String.valueOf(batteryAge));

            int avgTemp = response[8] & 0xFF;
            history.put("Average Temperature C", String.valueOf(avgTemp - 40));

            int maxTemp = response[9] & 0xFF;
            history.put("Max Temperature C", String.valueOf(maxTemp - 40));

            int minVoltage = ((response[10] & 0xFF) << 8) | (response.length > 11 ? (response[11] & 0xFF) : 0);
            history.put("Min Voltage V", String.format("%.2f", minVoltage / 1000.0));
        }
        return history;
    }

    @Override
    public Map<String, Double> readEnergyRecoveryData() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2530));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> data = new LinkedHashMap<>();
        if (response.length > 10) {
            data.put("Regenerative Power kW", extractDouble(response, 3));
            data.put("Energy Recovered kWh", extractDouble(response, 5));
            data.put("Regeneration Duty Cycle %", (double) (response[7] & 0xFF));
            data.put("Alternator Efficiency %", (double) (response[8] & 0xFF));
            data.put("Battery Charging Rate A", extractDouble(response, 9));
        }
        return data;
    }

    private static double extractDouble(byte[] data, int offset) {
        if (data == null || offset + 1 >= data.length) return 0.0;
        return (((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF)) / 100.0;
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
