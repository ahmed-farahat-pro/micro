package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.climate.ClimateSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW climate system implementation using the IHKA ECU.
 * Provides auto start/stop climate control, residual heat configuration,
 * seat heating stage control, and temperature monitoring.
 */
public class BmwClimateSystem extends ClimateSystem {

    private static final String IHKA_ID = "IHKA";

    public BmwClimateSystem() {
        super();
        addEcu(new EcuDefinition(IHKA_ID, "IHKA - Climate Control",
                BmwEcuMap.IHKA_ADDRESS.getPhysicalId(), BmwEcuMap.IHKA_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Auto Climate Configuration",
                "AC Compressor Auto-Off with Engine Stop",
                "Residual Heat Configuration",
                "Seat Heating Stage Configuration",
                "Temperature Sensor Read",
                "Recirculation Mode Configuration"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW IHKA Climate Control System - CAN 0x6C1/0x6C9";
    }

    @Override
    public Map<String, String> readClimateConfig() throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x6000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 6) {
            config.put("Auto Climate", (response[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Dual Zone", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("AC Off with Engine Stop", (response[3] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("Residual Heat", (response[4] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Residual Heat Duration min", String.valueOf(response[5] & 0xFF));
            config.put("Recirculation Auto", (response[6] & 0x01) != 0 ? "Enabled" : "Disabled");
        }
        return config;
    }

    @Override
    public void writeClimateConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte mainFlags = 0;
        if ("Enabled".equalsIgnoreCase(settings.get("Auto Climate"))) mainFlags |= 0x01;
        if ("Enabled".equalsIgnoreCase(settings.get("Dual Zone"))) mainFlags |= 0x02;
        if ("Enabled".equalsIgnoreCase(settings.get("AC Off with Engine Stop"))) mainFlags |= 0x04;

        byte heatFlags = 0;
        if ("Enabled".equalsIgnoreCase(settings.get("Residual Heat"))) heatFlags |= 0x01;

        int heatDuration = 15;
        if (settings.containsKey("Residual Heat Duration min")) {
            heatDuration = Integer.parseInt(settings.get("Residual Heat Duration min"));
        }

        byte recirc = 0;
        if ("Enabled".equalsIgnoreCase(settings.get("Recirculation Auto"))) recirc = 0x01;

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x60, 0x00, mainFlags, heatFlags, (byte) heatDuration, recirc
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readCompressorStatus() throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x6001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 5) {
            status.put("Compressor Running", (response[3] & 0x01) != 0 ? "Yes" : "No");
            status.put("Compressor Duty Cycle %", String.valueOf(response[4] & 0xFF));
            status.put("Refrigerant Pressure bar", String.valueOf(response[5] & 0xFF));
        }
        return status;
    }

    @Override
    public Map<String, String> readSeatHeatingConfig() throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x6010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("Seat Heating Stages", String.valueOf(response[3] & 0xFF));
            config.put("Auto Off Enabled", (response[4] & 0x01) != 0 ? "Yes" : "No");
            config.put("Steering Wheel Heating", (response[5] & 0x01) != 0 ? "Installed" : "Not Installed");
        }
        return config;
    }

    @Override
    public void writeSeatHeatingConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        int stages = 3;
        if (settings.containsKey("Seat Heating Stages")) {
            stages = Integer.parseInt(settings.get("Seat Heating Stages"));
        }
        byte autoOff = "Yes".equalsIgnoreCase(settings.get("Auto Off Enabled")) ? (byte) 0x01 : 0x00;

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x60, 0x10, (byte) stages, autoOff});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Double> readTemperatures() throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x6002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> temps = new LinkedHashMap<>();
        if (response.length > 10) {
            temps.put("Interior", extractTemp(response, 3));
            temps.put("Exterior", extractTemp(response, 5));
            temps.put("Evaporator", extractTemp(response, 7));
            temps.put("Coolant", extractTemp(response, 9));
        }
        return temps;
    }

    private static double extractTemp(byte[] data, int offset) {
        int raw = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        return (raw - 4000) / 100.0; // BMW temperature encoding: offset by 40.00 C
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
