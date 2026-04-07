package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.brakes.BrakeSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen brake system implementation for ABS/ESP and electric parking brake.
 *
 * <p>Manages the ABS/ESP configuration, electric parking brake calibration,
 * brake pad wear monitoring, brake bleed routines, and stability control
 * parameters.</p>
 */
public class CitroenBrakeSystem extends BrakeSystem {

    private static final String ECU_ID = "ABS";
    private static final String ECU_NAME = "ABS/ESP Control Unit";
    private static final int ABS_LOGICAL = 0x760;
    private static final int ABS_PHYSICAL = 0x660;

    private static final int DID_BRAKE_CONFIG = 0x7001;
    private static final int DID_ESP_STATUS = 0x7002;
    private static final int DID_WHEEL_SPEED_FL = 0x7003;
    private static final int DID_WHEEL_SPEED_FR = 0x7004;
    private static final int DID_WHEEL_SPEED_RL = 0x7005;
    private static final int DID_WHEEL_SPEED_RR = 0x7006;
    private static final int DID_EPB_STATUS = 0x7010;
    private static final int DID_PAD_WEAR = 0x7011;
    private static final int DID_BRAKE_PRESSURE = 0x7020;

    private static final int ROUTINE_BRAKE_BLEED = 0x7F01;
    private static final int ROUTINE_PAD_RESET = 0x7F02;
    private static final int ROUTINE_EPB_CALIBRATE = 0x7F03;

    public CitroenBrakeSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, ABS_LOGICAL, ABS_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/configure ABS/ESP settings",
                "Read wheel speed sensor data",
                "Perform brake bleed routine per corner",
                "Reset brake pad wear indicator",
                "Calibrate electric parking brake",
                "Read brake pressure data",
                "Read ABS/ESP status and fault history"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Brake System - ABS/ESP with Electric Parking Brake";
    }

    @Override
    public Map<String, String> readBrakeConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_BRAKE_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            config.put("abs_variant", "Gen " + (data[3] & 0xFF));
            config.put("esp_enabled", (data[4] & 0x01) != 0 ? "Yes" : "No");
            config.put("esp_sport_mode", (data[4] & 0x02) != 0 ? "Available" : "Not available");
            config.put("traction_control", (data[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("hill_start_assist", (data[5] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("electric_parking_brake", (data[6] & 0x01) != 0 ? "Present" : "Not present");
            config.put("auto_hold", (data[6] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("brake_disc_wipe", (data[7] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("emergency_brake_assist", (data[8] & 0x01) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void writeBrakeConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte espFlags = 0;
        if ("Yes".equals(settings.get("esp_enabled"))) espFlags |= 0x01;
        if ("Available".equals(settings.get("esp_sport_mode"))) espFlags |= 0x02;

        byte tcFlags = 0;
        if ("Enabled".equals(settings.get("traction_control"))) tcFlags |= 0x01;
        if ("Enabled".equals(settings.get("hill_start_assist"))) tcFlags |= 0x02;

        byte epbFlags = 0;
        if ("Enabled".equals(settings.get("auto_hold"))) epbFlags |= 0x02;

        byte discWipe = "Enabled".equals(settings.get("brake_disc_wipe")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_BRAKE_CONFIG, new byte[]{espFlags, tcFlags, epbFlags, discWipe});
    }

    @Override
    public void performBrakeBleed(String corner) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte cornerCode;
        switch (corner.toUpperCase()) {
            case "FL": case "FRONT_LEFT": cornerCode = 0x01; break;
            case "FR": case "FRONT_RIGHT": cornerCode = 0x02; break;
            case "RL": case "REAR_LEFT": cornerCode = 0x03; break;
            case "RR": case "REAR_RIGHT": cornerCode = 0x04; break;
            case "ALL": cornerCode = 0x00; break;
            default: throw new IllegalArgumentException("Unknown corner: " + corner);
        }

        executeRoutine(proto, ROUTINE_BRAKE_BLEED, new byte[]{cornerCode});
    }

    @Override
    public void resetBrakePadWear() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_PAD_RESET, new byte[0]);
    }

    @Override
    public void calibrateParkingBrake() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_EPB_CALIBRATE, new byte[0]);
    }

    @Override
    public Map<String, Double> readBrakeData() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> data = new LinkedHashMap<>();

        data.put("wheel_speed_fl_kph", decodeSpeed(readDid(conn, DID_WHEEL_SPEED_FL)));
        data.put("wheel_speed_fr_kph", decodeSpeed(readDid(conn, DID_WHEEL_SPEED_FR)));
        data.put("wheel_speed_rl_kph", decodeSpeed(readDid(conn, DID_WHEEL_SPEED_RL)));
        data.put("wheel_speed_rr_kph", decodeSpeed(readDid(conn, DID_WHEEL_SPEED_RR)));

        byte[] pressure = readDid(conn, DID_BRAKE_PRESSURE);
        if (pressure != null && pressure.length >= 5) {
            data.put("brake_pressure_bar", (double) (((pressure[3] & 0xFF) << 8) | (pressure[4] & 0xFF)) / 10.0);
        }

        byte[] padWear = readDid(conn, DID_PAD_WEAR);
        if (padWear != null && padWear.length >= 5) {
            data.put("pad_wear_front_percent", (double) (padWear[3] & 0xFF));
            data.put("pad_wear_rear_percent", (double) (padWear[4] & 0xFF));
        }

        return data;
    }

    @Override
    public Map<String, String> readAbsStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_ESP_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            status.put("abs_active", (data[3] & 0x01) != 0 ? "Yes" : "No");
            status.put("esp_active", (data[3] & 0x02) != 0 ? "Yes" : "No");
            status.put("tcs_active", (data[3] & 0x04) != 0 ? "Yes" : "No");
            status.put("esp_lamp", (data[4] & 0x01) != 0 ? "On" : "Off");
            status.put("abs_lamp", (data[4] & 0x02) != 0 ? "On" : "Off");
            status.put("epb_status", data[5] == 0x01 ? "Applied" : data[5] == 0x02 ? "Released" : "In motion");
            status.put("auto_hold_active", (data[6] & 0x01) != 0 ? "Yes" : "No");
        }

        return status;
    }

    // -- Helpers --

    private double decodeSpeed(byte[] data) {
        if (data == null || data.length < 5) return 0.0;
        int raw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        return raw / 100.0;
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
        proto.sendRequest(new byte[]{0x27, 0x03});
        proto.readResponse();
    }

    private void executeRoutine(DiagnosticProtocol proto, int routineId, byte[] data) throws IOException {
        byte[] request = new byte[4 + data.length];
        request[0] = 0x31;
        request[1] = 0x01;
        request[2] = (byte) ((routineId >> 8) & 0xFF);
        request[3] = (byte) (routineId & 0xFF);
        System.arraycopy(data, 0, request, 4, data.length);
        proto.sendRequest(request);
        proto.readResponse();
    }
}
