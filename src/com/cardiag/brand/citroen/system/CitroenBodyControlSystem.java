package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.body.BodyControlSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen body control system implementation based on the BSI.
 *
 * <p>The BSI (Boitier de Servitude Intelligent) is the most complex module in
 * Citroen vehicles, controlling central locking, deadlocking, auto-fold mirrors,
 * plip (remote key) programming, window behaviour, wiper control, and all
 * body electrical functions.</p>
 */
public class CitroenBodyControlSystem extends BodyControlSystem {

    private static final String ECU_ID = "BSI";
    private static final String ECU_NAME = "BSI (Boitier de Servitude Intelligent)";
    private static final int BSI_LOGICAL = 0x764;
    private static final int BSI_PHYSICAL = 0x664;

    // BSI DIDs
    private static final int DID_LOCK_CONFIG = 0x2010;
    private static final int DID_WINDOW_CONFIG = 0x2011;
    private static final int DID_MIRROR_CONFIG = 0x2012;
    private static final int DID_WIPER_CONFIG = 0x2013;
    private static final int DID_DEADLOCK_CONFIG = 0x2014;
    private static final int DID_PLIP_CONFIG = 0x2020;
    private static final int DID_PLIP_COUNT = 0x2021;

    // Routine IDs
    private static final int ROUTINE_PLIP_PROGRAM = 0x2F01;
    private static final int ROUTINE_WINDOW_INIT = 0x2F02;

    public CitroenBodyControlSystem() {
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
                "Read/configure central locking behaviour",
                "Enable/disable speed-sensitive auto-lock",
                "Configure selective unlock (driver door only)",
                "Enable/disable deadlocking",
                "Set auto-relock timeout",
                "Enable/disable lock confirmation horn chirp",
                "Read/configure power window behaviour",
                "Enable/disable one-touch window up/down",
                "Enable/disable auto-close windows on lock",
                "Configure anti-pinch sensitivity",
                "Read/configure mirror behaviour",
                "Enable/disable auto-fold mirrors on lock",
                "Enable/disable mirror tilt on reverse",
                "Read/configure wiper and rain sensor settings",
                "Set rain sensor sensitivity level",
                "Enable/disable rear wiper in reverse",
                "Program plip (remote key)",
                "Read plip count and configuration"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Body Control System - BSI (Boitier de Servitude Intelligent)";
    }

    @Override
    public Map<String, Boolean> readCentralLockConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_LOCK_CONFIG);

        Map<String, Boolean> config = new LinkedHashMap<>();
        if (data != null && data.length >= 10) {
            config.put("speed_lock_enabled", (data[3] & 0x01) != 0);
            config.put("selective_unlock", (data[3] & 0x02) != 0);
            config.put("deadlocking_enabled", (data[3] & 0x04) != 0);
            config.put("confirmation_horn", (data[3] & 0x08) != 0);
            config.put("confirmation_lights", (data[3] & 0x10) != 0);
            config.put("auto_relock_enabled", (data[4] & 0x01) != 0);
            config.put("boot_separate_unlock", (data[4] & 0x02) != 0);
            config.put("child_lock_rear", (data[4] & 0x04) != 0);
        }

        return config;
    }

    @Override
    public void setCentralLockBehavior(Map<String, Boolean> config) throws IOException {
        if (config == null || config.isEmpty()) {
            throw new IllegalArgumentException("Config must not be null or empty");
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte flags1 = 0;
        byte flags2 = 0;

        if (config.getOrDefault("speed_lock_enabled", false)) flags1 |= 0x01;
        if (config.getOrDefault("selective_unlock", false)) flags1 |= 0x02;
        if (config.getOrDefault("deadlocking_enabled", false)) flags1 |= 0x04;
        if (config.getOrDefault("confirmation_horn", false)) flags1 |= 0x08;
        if (config.getOrDefault("confirmation_lights", true)) flags1 |= 0x10;
        if (config.getOrDefault("auto_relock_enabled", true)) flags2 |= 0x01;
        if (config.getOrDefault("boot_separate_unlock", false)) flags2 |= 0x02;
        if (config.getOrDefault("child_lock_rear", false)) flags2 |= 0x04;

        writeDid(proto, DID_LOCK_CONFIG, new byte[]{flags1, flags2});
    }

    @Override
    public Map<String, String> readWindowConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_WINDOW_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("one_touch_up", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("one_touch_down", (data[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("close_on_lock", (data[3] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("open_on_unlock", (data[3] & 0x08) != 0 ? "Enabled" : "Disabled");
            config.put("anti_pinch_sensitivity", String.valueOf(data[4] & 0x07));
        }

        return config;
    }

    @Override
    public void setAutoCloseWindows(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte[] current = readDid(conn, DID_WINDOW_CONFIG);
        if (current != null && current.length >= 7) {
            byte flags = current[3];
            if (enabled) {
                flags |= 0x04;
            } else {
                flags &= ~0x04;
            }
            writeDid(proto, DID_WINDOW_CONFIG, new byte[]{flags, current[4]});
        }
    }

    @Override
    public Map<String, String> readMirrorConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_MIRROR_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("auto_fold_on_lock", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("auto_unfold_on_unlock", (data[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("tilt_on_reverse", (data[3] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("heated_mirrors_auto", (data[3] & 0x08) != 0 ? "Enabled" : "Disabled");
            config.put("auto_dimming", (data[4] & 0x01) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void setMirrorFoldOnLock(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte[] current = readDid(conn, DID_MIRROR_CONFIG);
        if (current != null && current.length >= 7) {
            byte flags = current[3];
            if (enabled) {
                flags |= 0x01;
                flags |= 0x02; // also enable unfold on unlock
            } else {
                flags &= ~0x01;
                flags &= ~0x02;
            }
            writeDid(proto, DID_MIRROR_CONFIG, new byte[]{flags, current[4]});
        }
    }

    @Override
    public Map<String, String> readWiperConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_WIPER_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("rear_wiper_in_reverse", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("rain_sensor_sensitivity", String.valueOf(data[4] & 0x07));
            config.put("wiper_service_position", (data[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("auto_wiper_speed_adjust", (data[3] & 0x04) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void setRainSensorSensitivity(int level) throws IOException {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("Sensitivity level must be 1-5, got: " + level);
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte[] current = readDid(conn, DID_WIPER_CONFIG);
        if (current != null && current.length >= 7) {
            writeDid(proto, DID_WIPER_CONFIG, new byte[]{current[3], (byte) level});
        }
    }

    /**
     * Programs a new plip (remote key) into the BSI.
     *
     * @param keySlot the key slot number (1-4)
     * @throws IOException if the programming fails
     */
    public void programPlip(int keySlot) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_PLIP_PROGRAM, new byte[]{(byte) keySlot});
    }

    /**
     * Reads the number of plips (remote keys) currently registered in the BSI.
     *
     * @return the number of registered keys
     * @throws IOException if reading fails
     */
    public int readPlipCount() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_PLIP_COUNT);
        if (data != null && data.length >= 4) {
            return data[3] & 0xFF;
        }
        return 0;
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
