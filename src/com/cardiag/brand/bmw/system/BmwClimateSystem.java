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
 * Provides auto climate control, compressor management, seat heating
 * stage configuration, steering wheel heating, and residual heat.
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
                "Auto Climate Toggle",
                "Max Cooling Power Configuration",
                "Seat Heating Stage Configuration",
                "Steering Wheel Heating Toggle",
                "Residual Heat Configuration",
                "Compressor Status Read",
                "Temperature Sensor Read"
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
    public void setAutoClimate(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x60, 0x01, val});
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
    public void setMaxCoolingPower(int percent) throws IOException {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be 0-100, got: " + percent);
        }

        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x60, 0x02, (byte) percent});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Integer> readSeatHeatingLevels() throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x6010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Integer> levels = new LinkedHashMap<>();
        if (response.length > 6) {
            levels.put("Driver", response[3] & 0xFF);
            levels.put("Passenger", response[4] & 0xFF);
            levels.put("Rear Left", response[5] & 0xFF);
            levels.put("Rear Right", response[6] & 0xFF);
        }
        return levels;
    }

    @Override
    public void setSeatHeatingStages(int stages) throws IOException {
        if (stages < 1 || stages > 5) {
            throw new IllegalArgumentException("Stages must be 1-5, got: " + stages);
        }

        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x60, 0x10, (byte) stages});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readSteeringWheelHeating() throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x6020));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 4) {
            config.put("Installed", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Active", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("Level", String.valueOf(response[4] & 0xFF));
        }
        return config;
    }

    @Override
    public void setSteeringWheelHeating(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x60, 0x20, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setResidualHeat(boolean enabled, int durationMin) throws IOException {
        if (durationMin <= 0) {
            throw new IllegalArgumentException("Duration must be positive, got: " + durationMin);
        }

        EcuConnection conn = connections.get(IHKA_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte flag = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x60, 0x03, flag, (byte) durationMin});
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
