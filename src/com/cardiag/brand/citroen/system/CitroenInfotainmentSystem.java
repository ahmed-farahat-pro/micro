package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.infotainment.InfotainmentSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen infotainment system implementation for SMEG/NAC head units.
 *
 * <p>Manages region configuration, Bluetooth pairing reset, Apple CarPlay /
 * Android Auto activation, DAB radio, reversing camera settings, and
 * audio configuration.</p>
 */
public class CitroenInfotainmentSystem extends InfotainmentSystem {

    private static final String ECU_ID = "SMEG";
    private static final String ECU_NAME = "SMEG/NAC Infotainment";
    private static final int SMEG_LOGICAL = 0x767;
    private static final int SMEG_PHYSICAL = 0x667;

    private static final int DID_INFOTAINMENT_CONFIG = 0x5001;
    private static final int DID_REGION = 0x5002;
    private static final int DID_BLUETOOTH = 0x5003;
    private static final int DID_CONNECTED_SERVICES = 0x5004;
    private static final int DID_AUDIO_CONFIG = 0x5005;
    private static final int DID_MEDIA_STATUS = 0x5010;

    private static final int ROUTINE_BT_RESET = 0x5F01;

    private static final Map<Integer, String> REGION_MAP = new LinkedHashMap<>();
    static {
        REGION_MAP.put(0, "Europe");
        REGION_MAP.put(1, "United Kingdom");
        REGION_MAP.put(2, "Middle East");
        REGION_MAP.put(3, "China");
        REGION_MAP.put(4, "Japan");
        REGION_MAP.put(5, "South America");
        REGION_MAP.put(6, "Oceania");
    }

    public CitroenInfotainmentSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, SMEG_LOGICAL, SMEG_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/set region code",
                "Read/configure Bluetooth settings",
                "Reset Bluetooth pairings",
                "Enable/disable Apple CarPlay",
                "Enable/disable Android Auto",
                "Enable/disable DAB radio",
                "Configure reversing camera guidelines",
                "Read media source status",
                "Configure speed-dependent volume"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Infotainment System - SMEG+/NAC";
    }

    @Override
    public Map<String, String> readInfotainmentConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_INFOTAINMENT_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 10) {
            config.put("region", REGION_MAP.getOrDefault(data[3] & 0xFF, "Unknown"));
            config.put("navigation_enabled", (data[4] & 0x01) != 0 ? "Yes" : "No");
            config.put("bluetooth_enabled", (data[5] & 0x01) != 0 ? "Yes" : "No");
            config.put("apple_carplay", (data[6] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("android_auto", (data[6] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("dab_radio", (data[7] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("rear_camera", (data[8] & 0x01) != 0 ? "Present" : "Not present");
            config.put("dynamic_guidelines", (data[8] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("volume_speed_compensation", (data[9] & 0x01) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void writeInfotainmentConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte connectivity = 0;
        if ("Enabled".equals(settings.get("apple_carplay"))) connectivity |= 0x01;
        if ("Enabled".equals(settings.get("android_auto"))) connectivity |= 0x02;

        byte dab = "Enabled".equals(settings.get("dab_radio")) ? (byte) 0x01 : 0x00;

        byte camera = 0;
        if ("Present".equals(settings.get("rear_camera"))) camera |= 0x01;
        if ("Enabled".equals(settings.get("dynamic_guidelines"))) camera |= 0x02;

        byte volume = "Enabled".equals(settings.get("volume_speed_compensation")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_CONNECTED_SERVICES, new byte[]{connectivity, dab, camera, volume});
    }

    @Override
    public String readRegionCode() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_REGION);

        if (data != null && data.length >= 4) {
            return REGION_MAP.getOrDefault(data[3] & 0xFF, "Unknown");
        }
        return "Unknown";
    }

    @Override
    public void setRegionCode(String region) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        int regionCode = 0;
        for (Map.Entry<Integer, String> entry : REGION_MAP.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(region)) {
                regionCode = entry.getKey();
                break;
            }
        }

        writeDid(proto, DID_REGION, new byte[]{(byte) regionCode});
    }

    @Override
    public void resetBluetooth() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_BT_RESET, new byte[0]);
    }

    @Override
    public Map<String, String> readMediaStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_MEDIA_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            int source = data[3] & 0xFF;
            status.put("current_source",
                    source == 0x00 ? "FM Radio" :
                    source == 0x01 ? "DAB" :
                    source == 0x02 ? "USB" :
                    source == 0x03 ? "Bluetooth Audio" :
                    source == 0x04 ? "CarPlay" :
                    source == 0x05 ? "Android Auto" : "Unknown");
            status.put("bluetooth_connected", (data[4] & 0x01) != 0 ? "Yes" : "No");
            status.put("usb_device_connected", (data[5] & 0x01) != 0 ? "Yes" : "No");
            status.put("volume_level", String.valueOf(data[6] & 0xFF));
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
        proto.sendRequest(new byte[]{0x27, 0x01});
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
