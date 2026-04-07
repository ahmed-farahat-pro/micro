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
                "Tilt Sensor Toggle"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW CAS Security System - Engineering level access via CAN 0x640/0x648";
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
    public int readKeyCount() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xD000));
        byte[] response = conn.getProtocol().readResponse();
        return response.length > 3 ? response[3] & 0xFF : 0;
    }

    @Override
    public void programNewKey(byte[] keyData) throws IOException {
        if (keyData == null || keyData.length == 0) {
            throw new IllegalArgumentException("Key data must not be null or empty");
        }

        EcuConnection conn = connections.get(CAS_ID);

        // Enter programming session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x02});
        conn.getProtocol().readResponse();

        // Engineering-level security access (0x61/0x62)
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x61});
        conn.getProtocol().readResponse();
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
    public void deleteKey(int keyIndex) throws IOException {
        if (keyIndex < 0 || keyIndex > 3) {
            throw new IllegalArgumentException("Key index must be 0-3, got: " + keyIndex);
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
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0xD0, 0x20, (byte) (keyIndex + 1)});
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
    public void setAlarmSensitivity(int level) throws IOException {
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("Sensitivity must be 1-10, got: " + level);
        }

        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0xD0, 0x41, (byte) level});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setTiltSensor(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0xD0, 0x42, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readKeylessEntryConfig() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xD020));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 3) {
            config.put("Comfort Access", (response[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Keyless Go", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Auto Relock", (response[3] & 0x04) != 0 ? "Enabled" : "Disabled");
        }
        return config;
    }

    @Override
    public void setKeylessEntry(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0xD0, 0x22, val});
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
