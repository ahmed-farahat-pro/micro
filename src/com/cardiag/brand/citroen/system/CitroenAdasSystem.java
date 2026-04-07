package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.adas.AdasSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen ADAS (Advanced Driver-Assistance Systems) implementation.
 *
 * <p>Manages camera calibration for the front-facing ADAS camera, active safety
 * brake (ASB), lane keeping assist (LKA), speed limit recognition, blind spot
 * monitoring, and driver attention alert configuration.</p>
 */
public class CitroenAdasSystem extends AdasSystem {

    private static final String ECU_ID = "ADAS";
    private static final String ECU_NAME = "ADAS Camera Module";
    private static final int ADAS_LOGICAL = 0x776;
    private static final int ADAS_PHYSICAL = 0x676;

    private static final int DID_ADAS_CONFIG = 0xC001;
    private static final int DID_CALIBRATION_STATUS = 0xC002;
    private static final int DID_SENSOR_STATUS = 0xC003;
    private static final int DID_ASB_CONFIG = 0xC010;
    private static final int DID_LKA_CONFIG = 0xC011;
    private static final int DID_BSM_CONFIG = 0xC012;
    private static final int DID_SPEED_SIGN = 0xC013;

    private static final int ROUTINE_CAMERA_CALIBRATE = 0xCF01;

    public CitroenAdasSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, ADAS_LOGICAL, ADAS_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/configure ADAS features",
                "Calibrate front-facing camera",
                "Read calibration status",
                "Enable/disable Active Safety Brake",
                "Enable/disable Lane Keeping Assist",
                "Enable/disable Blind Spot Monitoring",
                "Configure speed limit recognition",
                "Read sensor health status"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn ADAS System - Camera & Active Safety";
    }

    @Override
    public Map<String, String> readAdasConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_ADAS_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            config.put("active_safety_brake", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("asb_sensitivity", String.valueOf(data[4] & 0x03));
            config.put("lane_keeping_assist", (data[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("lka_mode", (data[5] & 0x02) != 0 ? "Active steering" : "Vibration only");
            config.put("blind_spot_monitoring", (data[6] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("speed_limit_recognition", (data[7] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("driver_attention_alert", (data[8] & 0x01) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void writeAdasConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte asb = 0;
        if ("Enabled".equals(settings.get("active_safety_brake"))) asb |= 0x01;
        byte asbSens = 0x02;
        if (settings.containsKey("asb_sensitivity")) {
            asbSens = (byte) Integer.parseInt(settings.get("asb_sensitivity"));
        }

        byte lka = 0;
        if ("Enabled".equals(settings.get("lane_keeping_assist"))) lka |= 0x01;
        if ("Active steering".equals(settings.get("lka_mode"))) lka |= 0x02;

        byte bsm = "Enabled".equals(settings.get("blind_spot_monitoring")) ? (byte) 0x01 : 0x00;
        byte slr = "Enabled".equals(settings.get("speed_limit_recognition")) ? (byte) 0x01 : 0x00;
        byte daa = "Enabled".equals(settings.get("driver_attention_alert")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_ADAS_CONFIG, new byte[]{asb, asbSens, lka, bsm, slr, daa});
    }

    @Override
    public void calibrateCamera() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_CAMERA_CALIBRATE, new byte[0]);
    }

    @Override
    public Map<String, String> readCalibrationStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_CALIBRATION_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            int calStatus = data[3] & 0xFF;
            status.put("calibration_status",
                    calStatus == 0x00 ? "Not calibrated" :
                    calStatus == 0x01 ? "Calibrated" :
                    calStatus == 0x02 ? "Calibration in progress" : "Error");
            status.put("camera_aligned", (data[4] & 0x01) != 0 ? "Yes" : "No");
            status.put("horizontal_offset_deg", String.valueOf((data[5] & 0xFF) / 10.0 - 5.0));
            status.put("vertical_offset_deg", String.valueOf((data[6] & 0xFF) / 10.0 - 5.0));
        }

        return status;
    }

    @Override
    public Map<String, String> readSensorStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_SENSOR_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            status.put("front_camera", (data[3] & 0x01) != 0 ? "OK" : "Fault");
            status.put("front_camera_blocked", (data[3] & 0x02) != 0 ? "Yes" : "No");
            status.put("front_radar", (data[4] & 0x01) != 0 ? "OK" : "Not present");
            status.put("rear_ultrasonic_sensors", (data[5] & 0x01) != 0 ? "OK" : "Fault");
            status.put("blind_spot_sensor_left", (data[6] & 0x01) != 0 ? "OK" : "Fault");
            status.put("blind_spot_sensor_right", (data[6] & 0x02) != 0 ? "OK" : "Fault");
        }

        return status;
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
