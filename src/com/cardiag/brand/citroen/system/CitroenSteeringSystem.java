package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.steering.SteeringSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen steering system implementation for variable electric power steering.
 *
 * <p>Manages the column or rack-mounted EPS including speed-sensitive
 * assist curves, steering angle sensor calibration, mode selection
 * (comfort/normal/sport), and lane-keeping torque overlay support.</p>
 */
public class CitroenSteeringSystem extends SteeringSystem {

    private static final String ECU_ID = "EPS";
    private static final String ECU_NAME = "Electric Power Steering";
    private static final int EPS_LOGICAL = 0x762;
    private static final int EPS_PHYSICAL = 0x662;

    private static final int DID_STEERING_CONFIG = 0x8001;
    private static final int DID_STEERING_ANGLE = 0x8002;
    private static final int DID_MOTOR_CURRENT = 0x8003;
    private static final int DID_ASSIST_TORQUE = 0x8004;
    private static final int DID_MOTOR_TEMP = 0x8005;
    private static final int DID_ASSIST_CURVE = 0x8010;

    private static final int ROUTINE_CALIBRATE_SAS = 0x8F01;

    public CitroenSteeringSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, EPS_LOGICAL, EPS_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/configure steering assist settings",
                "Read/modify speed-sensitive assist curve",
                "Calibrate steering angle sensor",
                "Read steering angle, motor current, and torque",
                "Configure assist modes (comfort/normal/sport)",
                "Enable/disable pull-drift compensation"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Steering System - Variable Electric Power Steering";
    }

    @Override
    public Map<String, String> readSteeringConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_STEERING_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            config.put("eps_type", data[3] == 0x00 ? "Column-mounted" : "Rack-mounted");
            config.put("speed_sensitive_assist", (data[4] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("comfort_mode", (data[5] & 0x01) != 0 ? "Available" : "Not available");
            config.put("sport_mode", (data[5] & 0x02) != 0 ? "Available" : "Not available");
            config.put("active_return", (data[6] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("pull_drift_compensation", (data[7] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("lka_torque_overlay", (data[8] & 0x01) != 0 ? "Supported" : "Not supported");
        }

        return config;
    }

    @Override
    public void writeSteeringConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte speedSensitive = "Enabled".equals(settings.get("speed_sensitive_assist")) ? (byte) 0x01 : 0x00;
        byte modes = 0;
        if ("Available".equals(settings.get("comfort_mode"))) modes |= 0x01;
        if ("Available".equals(settings.get("sport_mode"))) modes |= 0x02;
        byte activeReturn = "Enabled".equals(settings.get("active_return")) ? (byte) 0x01 : 0x00;
        byte driftComp = "Enabled".equals(settings.get("pull_drift_compensation")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_STEERING_CONFIG, new byte[]{speedSensitive, modes, activeReturn, driftComp});
    }

    @Override
    public Map<String, String> readAssistCurve() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_ASSIST_CURVE);

        Map<String, String> curve = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            curve.put("assist_at_0_kph_percent", String.valueOf(data[3] & 0xFF));
            curve.put("assist_at_30_kph_percent", String.valueOf(data[4] & 0xFF));
            curve.put("assist_at_80_kph_percent", String.valueOf(data[5] & 0xFF));
            curve.put("assist_at_130_kph_percent", String.valueOf(data[6] & 0xFF));
        }

        return curve;
    }

    @Override
    public void writeAssistCurve(Map<String, String> curve) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte at0 = (byte) Integer.parseInt(curve.getOrDefault("assist_at_0_kph_percent", "100"));
        byte at30 = (byte) Integer.parseInt(curve.getOrDefault("assist_at_30_kph_percent", "80"));
        byte at80 = (byte) Integer.parseInt(curve.getOrDefault("assist_at_80_kph_percent", "60"));
        byte at130 = (byte) Integer.parseInt(curve.getOrDefault("assist_at_130_kph_percent", "40"));

        writeDid(proto, DID_ASSIST_CURVE, new byte[]{at0, at30, at80, at130});
    }

    @Override
    public void calibrateSteering() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_CALIBRATE_SAS, new byte[0]);
    }

    @Override
    public Map<String, Double> readSteeringAngles() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> angles = new LinkedHashMap<>();

        byte[] angleData = readDid(conn, DID_STEERING_ANGLE);
        byte[] currentData = readDid(conn, DID_MOTOR_CURRENT);
        byte[] torqueData = readDid(conn, DID_ASSIST_TORQUE);
        byte[] tempData = readDid(conn, DID_MOTOR_TEMP);

        if (angleData != null && angleData.length >= 5) {
            int raw = ((angleData[3] & 0xFF) << 8) | (angleData[4] & 0xFF);
            angles.put("steering_angle_deg", (raw - 32768) / 10.0);
        }
        if (currentData != null && currentData.length >= 5) {
            int raw = ((currentData[3] & 0xFF) << 8) | (currentData[4] & 0xFF);
            angles.put("motor_current_a", raw / 100.0);
        }
        if (torqueData != null && torqueData.length >= 5) {
            int raw = ((torqueData[3] & 0xFF) << 8) | (torqueData[4] & 0xFF);
            angles.put("assist_torque_nm", (raw - 32768) / 100.0);
        }
        if (tempData != null && tempData.length >= 4) {
            angles.put("motor_temperature_c", (double) (tempData[3] & 0xFF) - 40.0);
        }

        return angles;
    }

    // -- Helpers --

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
