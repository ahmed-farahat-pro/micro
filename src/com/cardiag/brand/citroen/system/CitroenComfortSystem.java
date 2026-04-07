package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.comfort.ComfortSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen comfort system implementation using BSI comfort features.
 *
 * <p>Manages courtesy lights timing, ambient lighting colour and intensity,
 * electric seat memory positions, comfort closing (windows via key hold),
 * and remote key function configuration.</p>
 */
public class CitroenComfortSystem extends ComfortSystem {

    private static final String ECU_ID = "BSI";
    private static final String ECU_NAME = "BSI (Comfort Functions)";
    private static final int BSI_LOGICAL = 0x764;
    private static final int BSI_PHYSICAL = 0x664;

    private static final int DID_SEAT_MEMORY_BASE = 0x2100;
    private static final int DID_AMBIENT_LIGHT = 0x2110;
    private static final int DID_CONVENIENCE = 0x2120;
    private static final int DID_KEY_CONFIG = 0x2130;
    private static final int DID_COURTESY_LIGHT = 0x2140;
    private static final int DID_PANIC_ALARM = 0x2150;

    public CitroenComfortSystem() {
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
                "Read/store electric seat memory positions",
                "Read/configure ambient lighting colour and intensity",
                "Read/configure courtesy light delay",
                "Enable/disable comfort closing (windows via key hold)",
                "Read remote key configuration",
                "Set panic alarm duration"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Comfort System - BSI Comfort Features";
    }

    @Override
    public Map<Integer, byte[]> readSeatMemoryPositions() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<Integer, byte[]> positions = new LinkedHashMap<>();

        for (int slot = 1; slot <= 3; slot++) {
            byte[] data = readDid(conn, DID_SEAT_MEMORY_BASE + slot);
            if (data != null && data.length > 3) {
                positions.put(slot, Arrays.copyOfRange(data, 3, data.length));
            }
        }

        return positions;
    }

    @Override
    public void setSeatMemory(int slot, byte[] positionData) throws IOException {
        if (slot < 1 || slot > 3) {
            throw new IllegalArgumentException("Slot must be 1-3, got: " + slot);
        }
        if (positionData == null) {
            throw new IllegalArgumentException("Position data must not be null");
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        writeDid(proto, DID_SEAT_MEMORY_BASE + slot, positionData);
    }

    @Override
    public Map<String, String> readAmbientLightingConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_AMBIENT_LIGHT);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("ambient_enabled", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("color_preset", String.valueOf(data[4] & 0xFF));
            config.put("intensity_percent", String.valueOf(data[5] & 0xFF));
            config.put("zone", data[6] == 0x01 ? "Front only" : "Full cabin");
        }

        return config;
    }

    @Override
    public void setAmbientLightColor(int red, int green, int blue) throws IOException {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException("RGB values must be 0-255");
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        writeDid(proto, DID_AMBIENT_LIGHT, new byte[]{0x01, (byte) red, (byte) green, (byte) blue});
    }

    @Override
    public Map<String, String> readConvenienceConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_CONVENIENCE);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 6) {
            config.put("comfort_closing", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("comfort_opening", (data[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("courtesy_light_delay_s", String.valueOf(data[4] & 0xFF));
            config.put("welcome_sequence", (data[3] & 0x04) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void setComfortClosing(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte[] current = readDid(conn, DID_CONVENIENCE);
        if (current != null && current.length >= 6) {
            byte flags = current[3];
            if (enabled) {
                flags |= 0x01;
            } else {
                flags &= ~0x01;
            }
            writeDid(proto, DID_CONVENIENCE, new byte[]{flags, current[4]});
        }
    }

    @Override
    public Map<String, String> readKeyConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_KEY_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("registered_keys", String.valueOf(data[3] & 0xFF));
            config.put("key_type", data[4] == 0x01 ? "Flip key" : "Keyless entry");
            config.put("remote_boot_open", (data[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("remote_window_control", (data[5] & 0x02) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void setPanicAlarmDuration(int seconds) throws IOException {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Duration must be positive, got: " + seconds);
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        writeDid(proto, DID_PANIC_ALARM, new byte[]{(byte) seconds});
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
}
