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
 * alternator coding, regenerative braking configuration, HV battery
 * health, and charging schedule management.
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
                "Battery Management Read (IBS)",
                "Charging Status Read",
                "Charging Limit Configuration",
                "Alternator Output Read",
                "Regenerative Braking Configuration",
                "HV Battery Health Read",
                "Charging Schedule Configuration"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW IBS Battery Management / Alternator / Regenerative Braking via DME 0x7E0/0x7E8";
    }

    @Override
    public Map<String, String> readBatteryManagement() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2500));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 10) {
            int voltage = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
            status.put("Voltage V", String.format("%.2f", voltage / 1000.0));

            int current = ((response[5] & 0xFF) << 8) | (response[6] & 0xFF);
            if (current > 32767) current -= 65536;
            status.put("Current A", String.format("%.2f", current / 100.0));

            status.put("State of Charge %", String.valueOf(response[7] & 0xFF));
            status.put("State of Health %", String.valueOf(response[8] & 0xFF));
            status.put("Temperature C", String.valueOf((response[9] & 0xFF) - 40));
            status.put("Battery Registered", (response[10] & 0x01) != 0 ? "Yes" : "No");
        }
        return status;
    }

    @Override
    public Map<String, String> readChargingStatus() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2540));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 6) {
            status.put("Charging Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            int chargeRate = ((response[4] & 0xFF) << 8) | (response[5] & 0xFF);
            status.put("Charge Rate W", String.valueOf(chargeRate));
            status.put("Estimated Time to Full min", String.valueOf(response[6] & 0xFF));
        }
        return status;
    }

    @Override
    public void setChargingLimit(int percent) throws IOException {
        if (percent < 50 || percent > 100) {
            throw new IllegalArgumentException("Charging limit must be 50-100%, got: " + percent);
        }

        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x25, 0x41, (byte) percent});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readAlternatorOutput() throws IOException {
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
    public void setRegenerativeBraking(int level) throws IOException {
        if (level < 0 || level > 3) {
            throw new IllegalArgumentException("Regenerative braking level must be 0-3, got: " + level);
        }

        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x25, 0x30, (byte) level});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readHvBatteryHealth() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2550));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> health = new LinkedHashMap<>();
        if (response.length > 8) {
            health.put("SOH %", String.valueOf(response[3] & 0xFF));
            int cycles = ((response[4] & 0xFF) << 8) | (response[5] & 0xFF);
            health.put("Cycle Count", String.valueOf(cycles));
            health.put("Degradation %", String.valueOf(response[6] & 0xFF));
            int maxCapacity = ((response[7] & 0xFF) << 8) | (response[8] & 0xFF);
            health.put("Max Capacity kWh", String.format("%.1f", maxCapacity / 10.0));
        }
        return health;
    }

    @Override
    public void setChargingSchedule(String schedule) throws IOException {
        if (schedule == null || schedule.isEmpty()) {
            throw new IllegalArgumentException("Schedule must not be null or empty");
        }

        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        // Parse schedule: e.g. "daily:23:00-06:00" or "weekdays:01:00-05:00"
        byte scheduleType;
        if (schedule.startsWith("weekdays")) {
            scheduleType = 0x01;
        } else if (schedule.startsWith("weekends")) {
            scheduleType = 0x02;
        } else {
            scheduleType = 0x00; // daily
        }

        // Extract times from schedule string
        String[] parts = schedule.split(":");
        byte startHour = 0x17; // default 23
        byte startMin = 0x00;
        byte endHour = 0x06;
        byte endMin = 0x00;

        if (parts.length >= 3) {
            try {
                startHour = (byte) Integer.parseInt(parts[1]);
                String endPart = parts[2].contains("-") ? parts[2].split("-")[1] : parts[2];
                endHour = (byte) Integer.parseInt(endPart);
            } catch (NumberFormatException ignored) {
                // Use defaults
            }
        }

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x25, 0x42,
                scheduleType, startHour, startMin, endHour, endMin
        });
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
