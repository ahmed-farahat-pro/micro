package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.suspension.SuspensionSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen suspension system implementation for AMVAR Progressive Hydraulic Cushions.
 *
 * <p>Controls the Citroen Advanced Comfort suspension including adaptive damping
 * modes (comfort, normal, sport), ride height adjustment on equipped models
 * (C5 Aircross), and sensor calibration.</p>
 */
public class CitroenSuspensionSystem extends SuspensionSystem {

    private static final String ECU_ID = "AMVAR";
    private static final String ECU_NAME = "AMVAR Suspension Control";
    private static final int AMVAR_LOGICAL = 0x769;
    private static final int AMVAR_PHYSICAL = 0x669;

    private static final int DID_RIDE_HEIGHT_FL = 0x9002;
    private static final int DID_RIDE_HEIGHT_FR = 0x9003;
    private static final int DID_RIDE_HEIGHT_RL = 0x9004;
    private static final int DID_RIDE_HEIGHT_RR = 0x9005;
    private static final int DID_DAMPING_CONFIG = 0x9010;
    private static final int DID_SUSPENSION_MODE = 0x9001;

    private static final int ROUTINE_CALIBRATE = 0x9F01;
    private static final int ROUTINE_SET_HEIGHT = 0x9F02;

    public CitroenSuspensionSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, AMVAR_LOGICAL, AMVAR_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read ride height at all four corners",
                "Set ride height mode (normal/raised/lowered)",
                "Read/configure damping mode (comfort/normal/sport)",
                "Calibrate ride height sensors",
                "Read current suspension mode",
                "Switch suspension mode"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Suspension System - AMVAR Progressive Hydraulic Cushions";
    }

    @Override
    public Map<String, Double> readRideHeight() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> heights = new LinkedHashMap<>();

        heights.put("front_left_mm", decodeHeight(readDid(conn, DID_RIDE_HEIGHT_FL)));
        heights.put("front_right_mm", decodeHeight(readDid(conn, DID_RIDE_HEIGHT_FR)));
        heights.put("rear_left_mm", decodeHeight(readDid(conn, DID_RIDE_HEIGHT_RL)));
        heights.put("rear_right_mm", decodeHeight(readDid(conn, DID_RIDE_HEIGHT_RR)));

        return heights;
    }

    @Override
    public void setRideHeight(String mode, double targetMm) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte modeCode;
        switch (mode.toLowerCase()) {
            case "normal":
                modeCode = 0x01;
                break;
            case "raised":
                modeCode = 0x02;
                break;
            case "lowered":
                modeCode = 0x03;
                break;
            default:
                throw new IllegalArgumentException("Unknown ride height mode: " + mode);
        }

        int target = (int) targetMm;
        executeRoutine(proto, ROUTINE_SET_HEIGHT, new byte[]{
                modeCode,
                (byte) ((target >> 8) & 0xFF),
                (byte) (target & 0xFF)
        });
    }

    @Override
    public Map<String, String> readDampingConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_DAMPING_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 8) {
            config.put("comfort_mode_available", (data[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("normal_mode_available", (data[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("sport_mode_available", (data[3] & 0x04) != 0 ? "Yes" : "No");
            config.put("current_damping_level_fl", String.valueOf(data[4] & 0xFF));
            config.put("current_damping_level_fr", String.valueOf(data[5] & 0xFF));
            config.put("current_damping_level_rl", String.valueOf(data[6] & 0xFF));
            config.put("current_damping_level_rr", String.valueOf(data[7] & 0xFF));
        }

        return config;
    }

    @Override
    public void writeDampingConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte modes = 0;
        if ("Yes".equals(settings.get("comfort_mode_available"))) modes |= 0x01;
        if ("Yes".equals(settings.get("normal_mode_available"))) modes |= 0x02;
        if ("Yes".equals(settings.get("sport_mode_available"))) modes |= 0x04;

        writeDid(proto, DID_DAMPING_CONFIG, new byte[]{modes});
    }

    @Override
    public void calibrateSensors() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_CALIBRATE, new byte[0]);
    }

    @Override
    public String readSuspensionMode() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_SUSPENSION_MODE);

        if (data != null && data.length >= 4) {
            int mode = data[3] & 0xFF;
            switch (mode) {
                case 0x01: return "Comfort";
                case 0x02: return "Normal";
                case 0x03: return "Sport";
                default: return "Unknown (0x" + Integer.toHexString(mode) + ")";
            }
        }
        return "Unknown";
    }

    @Override
    public void setSuspensionMode(String mode) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte modeCode;
        switch (mode.toLowerCase()) {
            case "comfort": modeCode = 0x01; break;
            case "normal": modeCode = 0x02; break;
            case "sport": modeCode = 0x03; break;
            default: throw new IllegalArgumentException("Unknown suspension mode: " + mode);
        }

        writeDid(proto, DID_SUSPENSION_MODE, new byte[]{modeCode});
    }

    // -- Helpers --

    private double decodeHeight(byte[] data) {
        if (data == null || data.length < 5) return 0.0;
        return ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
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
