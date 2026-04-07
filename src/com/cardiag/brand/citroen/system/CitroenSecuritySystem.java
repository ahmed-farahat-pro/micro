package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.security.SecuritySystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen security system implementation using BSI immobilizer and alarm functions.
 *
 * <p>Manages the PSA immobilizer system, key programming (plip learning),
 * Thatcham-category alarm configuration, deadlocking, and perimetric alarm
 * sensor settings.</p>
 */
public class CitroenSecuritySystem extends SecuritySystem {

    private static final String ECU_ID = "BSI";
    private static final String ECU_NAME = "BSI (Security Functions)";
    private static final int BSI_LOGICAL = 0x764;
    private static final int BSI_PHYSICAL = 0x664;

    private static final int DID_IMMOBILIZER_STATUS = 0x2300;
    private static final int DID_KEY_LIST = 0x2301;
    private static final int DID_ALARM_CONFIG = 0x2310;
    private static final int DID_DEADLOCK_STATUS = 0x2320;

    private static final int ROUTINE_PROGRAM_KEY = 0x2F10;
    private static final int ROUTINE_DELETE_KEY = 0x2F11;
    private static final int ROUTINE_ALIGN_IMMOBILIZER = 0x2F12;

    public CitroenSecuritySystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, BSI_LOGICAL, BSI_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read registered key list",
                "Program new key (plip learning)",
                "Delete key from BSI",
                "Read immobilizer status",
                "Align immobilizer (key-ECM sync)",
                "Read/configure Thatcham alarm settings",
                "Enable/disable perimetric alarm sensors",
                "Enable/disable volumetric (interior) sensor",
                "Configure deadlocking"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Security System - BSI Immobilizer & Thatcham Alarm";
    }

    @Override
    public List<String> readRegisteredKeys() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_KEY_LIST);

        List<String> keys = new ArrayList<>();
        if (data != null && data.length >= 5) {
            int keyCount = data[3] & 0xFF;
            for (int i = 0; i < keyCount; i++) {
                int offset = 4 + (i * 2);
                if (offset + 1 < data.length) {
                    int keyType = data[offset] & 0xFF;
                    int keyStatus = data[offset + 1] & 0xFF;
                    String type = keyType == 0x01 ? "Flip key" : keyType == 0x02 ? "Keyless entry" : "Unknown";
                    String status = keyStatus == 0x01 ? "Active" : "Inactive";
                    keys.add("Key " + (i + 1) + ": " + type + " (" + status + ")");
                }
            }
        }

        return keys;
    }

    @Override
    public void programKey(byte[] keyData) throws IOException {
        if (keyData == null) {
            throw new IllegalArgumentException("Key data must not be null");
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_PROGRAM_KEY, keyData);
    }

    @Override
    public void deleteKey(int keySlot) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_DELETE_KEY, new byte[]{(byte) keySlot});
    }

    @Override
    public Map<String, String> readImmobilizerStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_IMMOBILIZER_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            status.put("immobilizer_state", (data[3] & 0x01) != 0 ? "Armed" : "Disarmed");
            status.put("transponder_detected", (data[3] & 0x02) != 0 ? "Yes" : "No");
            status.put("key_validated", (data[3] & 0x04) != 0 ? "Yes" : "No");
            status.put("engine_start_allowed", (data[4] & 0x01) != 0 ? "Yes" : "No");
            status.put("immobilizer_type", data[5] == 0x02 ? "PSA Gen3" : "PSA Gen2");
            status.put("emergency_code_available", (data[6] & 0x01) != 0 ? "Yes" : "No");
        }

        return status;
    }

    @Override
    public void alignImmobilizer() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_ALIGN_IMMOBILIZER, new byte[0]);
    }

    @Override
    public Map<String, String> readAlarmConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_ALARM_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 8) {
            int alarmType = data[3] & 0xFF;
            config.put("alarm_type", alarmType == 0x00 ? "None" :
                    alarmType == 0x01 ? "Basic" : "Thatcham Cat 1");
            config.put("volumetric_sensor", (data[4] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("perimetric_alarm", (data[4] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("tilt_sensor", (data[4] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("siren_duration_s", String.valueOf(data[5] & 0xFF));
            config.put("deadlocking_enabled", (data[6] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("auto_arm_delay_s", String.valueOf(data[7] & 0xFF));
        }

        return config;
    }

    @Override
    public void writeAlarmConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte alarmType = 0x02; // Thatcham default
        byte sensors = 0;
        byte sirenDuration = 30;
        byte deadlock = 0x01;

        if ("None".equals(settings.get("alarm_type"))) alarmType = 0x00;
        else if ("Basic".equals(settings.get("alarm_type"))) alarmType = 0x01;

        if ("Enabled".equals(settings.get("volumetric_sensor"))) sensors |= 0x01;
        if ("Enabled".equals(settings.get("perimetric_alarm"))) sensors |= 0x02;
        if ("Enabled".equals(settings.get("tilt_sensor"))) sensors |= 0x04;

        if (settings.containsKey("siren_duration_s")) {
            sirenDuration = (byte) Integer.parseInt(settings.get("siren_duration_s"));
        }
        if ("Disabled".equals(settings.get("deadlocking_enabled"))) deadlock = 0x00;

        writeDid(proto, DID_ALARM_CONFIG, new byte[]{alarmType, sensors, sirenDuration, deadlock});
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
