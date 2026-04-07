package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.security.SecuritySystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW security system implementation using the CAS (Car Access System).
 * Provides key programming, EWS/ISN alignment, alarm sensitivity
 * configuration, and keyless entry management.
 */
public class BmwSecuritySystem extends SecuritySystem {

    private static final String CAS_ID = "CAS";

    public BmwSecuritySystem() {
        super();
        addEcu(new EcuDefinition(CAS_ID, "CAS - Car Access System",
                BmwEcuMap.CAS_ADDRESS.getPhysicalId(), BmwEcuMap.CAS_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Key Programming (Engineering Access)",
                "Key Deletion",
                "EWS/ISN Alignment",
                "Immobilizer Status Read",
                "Alarm Configuration",
                "Keyless Entry Configuration",
                "Tilt Sensor Sensitivity"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW CAS Security System - Engineering level access via CAN 0x640/0x648";
    }

    @Override
    public List<String> readRegisteredKeys() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        List<String> keys = new ArrayList<>();

        for (int slot = 1; slot <= 4; slot++) {
            int did = 0xD000 + slot;
            conn.getProtocol().sendRequest(buildReadDid(did));
            byte[] response = conn.getProtocol().readResponse();

            if (response.length > 3 && response[3] != 0x00) {
                StringBuilder keyId = new StringBuilder();
                for (int i = 3; i < response.length; i++) {
                    keyId.append(String.format("%02X", response[i] & 0xFF));
                }
                keys.add("Slot " + slot + ": Key ID " + keyId.toString());
            } else {
                keys.add("Slot " + slot + ": Empty");
            }
        }
        return keys;
    }

    @Override
    public void programKey(byte[] keyData) throws IOException {
        EcuConnection conn = connections.get(CAS_ID);

        // Enter programming session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x02});
        conn.getProtocol().readResponse();

        // Engineering-level security access (0x61/0x62)
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x61});
        byte[] seedResponse = conn.getProtocol().readResponse();

        // Key programming requires engineering seed/key exchange
        // In a real implementation, the seed would be processed by BmwSeedKeyAlgorithm
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x62, 0x00, 0x00, 0x00, 0x00});
        conn.getProtocol().readResponse();

        // Write key data via routine control
        byte[] request = new byte[4 + keyData.length];
        request[0] = 0x31; // RoutineControl
        request[1] = 0x01; // Start routine
        request[2] = (byte) 0xD0;
        request[3] = 0x10;
        System.arraycopy(keyData, 0, request, 4, keyData.length);
        conn.getProtocol().sendRequest(request);
        conn.getProtocol().readResponse();
    }

    @Override
    public void deleteKey(int keySlot) throws IOException {
        if (keySlot < 1 || keySlot > 4) {
            throw new IllegalArgumentException("Key slot must be 1-4, got: " + keySlot);
        }

        EcuConnection conn = connections.get(CAS_ID);

        // Engineering session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x02});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x61});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x62, 0x00, 0x00, 0x00, 0x00});
        conn.getProtocol().readResponse();

        // Delete key in specified slot
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0xD0, 0x20, (byte) keySlot});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readImmobilizerStatus() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xD010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 5) {
            status.put("EWS Status", (response[3] & 0x01) != 0 ? "Aligned" : "Not Aligned");
            status.put("ISN Valid", (response[3] & 0x02) != 0 ? "Yes" : "No");
            status.put("Starter Lockout", (response[4] & 0x01) != 0 ? "Active" : "Inactive");
            status.put("Tamper Counter", String.valueOf(response[5] & 0xFF));
        }
        return status;
    }

    @Override
    public void alignImmobilizer() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);

        // Extended session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        // Engineering security access
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x61});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x62, 0x00, 0x00, 0x00, 0x00});
        conn.getProtocol().readResponse();

        // ISN alignment routine: synchronize CAS ISN with DME ISN
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0xD0, 0x30});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readAlarmConfig() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xD040));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("Alarm Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Tilt Alarm", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Interior Motion", (response[3] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("Sensitivity", String.valueOf(response[4] & 0xFF));
            config.put("Siren Duration Seconds", String.valueOf(response[5] & 0xFF));
        }
        return config;
    }

    @Override
    public void writeAlarmConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte flags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("Alarm Active"))) flags |= 0x01;
        if ("Enabled".equalsIgnoreCase(settings.get("Tilt Alarm"))) flags |= 0x02;
        if ("Enabled".equalsIgnoreCase(settings.get("Interior Motion"))) flags |= 0x04;

        int sensitivity = 3;
        if (settings.containsKey("Sensitivity")) {
            sensitivity = Integer.parseInt(settings.get("Sensitivity"));
        }

        int duration = 30;
        if (settings.containsKey("Siren Duration Seconds")) {
            duration = Integer.parseInt(settings.get("Siren Duration Seconds"));
        }

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0xD0, 0x40, flags, (byte) sensitivity, (byte) duration
        });
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
