package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.climate.ClimateSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen climate system implementation for dual-zone automatic climate control.
 *
 * <p>Manages the HVAC system including A/C compressor control, residual heat
 * function (rest heat after engine off), automatic windscreen defog,
 * heated seat configuration, and cabin temperature sensors.</p>
 */
public class CitroenClimateSystem extends ClimateSystem {

    private static final String ECU_ID = "CLIM";
    private static final String ECU_NAME = "Climate Control ECU";
    private static final int CLIM_LOGICAL = 0x768;
    private static final int CLIM_PHYSICAL = 0x668;

    private static final int DID_CLIMATE_CONFIG = 0x6001;
    private static final int DID_COMPRESSOR = 0x6002;
    private static final int DID_CABIN_TEMP = 0x6003;
    private static final int DID_EVAP_TEMP = 0x6004;
    private static final int DID_AMBIENT_TEMP = 0x6005;
    private static final int DID_HUMIDITY = 0x6006;
    private static final int DID_SEAT_HEAT = 0x6010;
    private static final int DID_DEFOG_CONFIG = 0x6020;

    public CitroenClimateSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, CLIM_LOGICAL, CLIM_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/configure dual-zone automatic climate settings",
                "Read A/C compressor status and pressure",
                "Enable/disable residual heat (rest heat)",
                "Configure auto-defog sensitivity",
                "Read/configure heated seat levels",
                "Read cabin, evaporator, and ambient temperatures",
                "Read cabin humidity level"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Climate System - Dual-Zone Automatic Climate Control";
    }

    @Override
    public Map<String, String> readClimateConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_CLIMATE_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            int type = data[3] & 0xFF;
            config.put("climate_type", type == 0x00 ? "Manual" :
                    type == 0x01 ? "Single-zone auto" : "Dual-zone auto");
            config.put("ac_compressor_type", (data[4] & 0x01) != 0 ? "Variable displacement" : "Fixed");
            config.put("residual_heat", (data[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("residual_heat_duration_min", String.valueOf(data[6] & 0xFF));
            config.put("auto_defog", (data[7] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("air_quality_sensor", (data[8] & 0x01) != 0 ? "Present" : "Not present");
        }

        return config;
    }

    @Override
    public void writeClimateConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte restHeat = "Enabled".equals(settings.get("residual_heat")) ? (byte) 0x01 : 0x00;
        byte restHeatDuration = 0x0F;
        if (settings.containsKey("residual_heat_duration_min")) {
            restHeatDuration = (byte) Integer.parseInt(settings.get("residual_heat_duration_min"));
        }
        byte defog = "Enabled".equals(settings.get("auto_defog")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_CLIMATE_CONFIG, new byte[]{restHeat, restHeatDuration, defog});
    }

    @Override
    public Map<String, String> readCompressorStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_COMPRESSOR);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            status.put("compressor_state", (data[3] & 0x01) != 0 ? "Running" : "Off");
            status.put("compressor_duty_cycle_percent", String.valueOf(data[4] & 0xFF));
            int pressure = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
            status.put("refrigerant_pressure_kpa", String.valueOf(pressure));
        }

        return status;
    }

    @Override
    public Map<String, String> readSeatHeatingConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_SEAT_HEAT);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 6) {
            config.put("heated_seats_present", (data[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("heat_levels", String.valueOf(data[4] & 0xFF));
            config.put("driver_current_level", String.valueOf(data[5] & 0x07));
            if (data.length >= 7) {
                config.put("passenger_current_level", String.valueOf(data[6] & 0x07));
            }
        }

        return config;
    }

    @Override
    public void writeSeatHeatingConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte present = "Yes".equals(settings.get("heated_seats_present")) ? (byte) 0x01 : 0x00;
        byte levels = 0x03;
        if (settings.containsKey("heat_levels")) {
            levels = (byte) Integer.parseInt(settings.get("heat_levels"));
        }

        writeDid(proto, DID_SEAT_HEAT, new byte[]{present, levels});
    }

    @Override
    public Map<String, Double> readTemperatures() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> temps = new LinkedHashMap<>();

        byte[] cabin = readDid(conn, DID_CABIN_TEMP);
        byte[] evap = readDid(conn, DID_EVAP_TEMP);
        byte[] ambient = readDid(conn, DID_AMBIENT_TEMP);
        byte[] humidity = readDid(conn, DID_HUMIDITY);

        temps.put("cabin_temperature_c", decodeTemp(cabin));
        temps.put("evaporator_temperature_c", decodeTemp(evap));
        temps.put("ambient_temperature_c", decodeTemp(ambient));
        if (humidity != null && humidity.length >= 4) {
            temps.put("cabin_humidity_percent", (double) (humidity[3] & 0xFF));
        }

        return temps;
    }

    // -- Helpers --

    private double decodeTemp(byte[] data) {
        if (data == null || data.length < 5) return 0.0;
        int raw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        return raw / 10.0 - 40.0;
    }

    private byte[] readDid(EcuConnection conn, int did) throws IOException {
        DiagnosticProtocol proto = conn.getProtocol();
        byte[] request = new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
        proto.sendRequest(request);
        return proto.readResponse();
    }

    private void writeDid(DiagnosticProtocol proto, int did, byte[] data) throws IOException {
        byte[] request = new byte[3 + data.length];
        request[0] = 0x2E;
        request[1] = (byte) ((did >> 8) & 0xFF);
        request[2] = (byte) (did & 0xFF);
        System.arraycopy(data, 0, request, 3, data.length);
        proto.sendRequest(request);
        proto.readResponse();
    }

    private void sendSessionControl(DiagnosticProtocol proto, int session) throws IOException {
        proto.sendRequest(new byte[]{0x10, (byte) session});
        proto.readResponse();
    }

    private void sendSecurityAccess(DiagnosticProtocol proto) throws IOException {
        proto.sendRequest(new byte[]{0x27, 0x01});
        proto.readResponse();
    }
}
