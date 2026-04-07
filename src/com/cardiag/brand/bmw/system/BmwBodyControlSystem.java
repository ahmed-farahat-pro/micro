package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.body.BodyControlSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW body control system implementation using the FRM (Footwell Module).
 * Provides central locking behavior, auto-fold mirrors, rain sensor
 * sensitivity, window configuration, and wiper settings.
 */
public class BmwBodyControlSystem extends BodyControlSystem {

    private static final String FRM_ID = "FRM";

    public BmwBodyControlSystem() {
        super();
        addEcu(new EcuDefinition(FRM_ID, "FRM - Footwell Module",
                BmwEcuMap.FRM_ADDRESS.getPhysicalId(), BmwEcuMap.FRM_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Central Locking Behavior Configuration",
                "Auto-Fold Mirror Toggle",
                "Rain Sensor Sensitivity Adjustment",
                "One-Touch Window Up/Down",
                "Comfort Close via Remote",
                "Speed Lock Configuration",
                "Selective Unlock"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW FRM Body Control Module - CAN 0x6C0/0x6C8";
    }

    @Override
    public Map<String, Boolean> readCentralLockConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3003));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Boolean> config = new LinkedHashMap<>();
        config.put("Speed Lock", bitSet(response, 3, 0));
        config.put("Selective Unlock", bitSet(response, 3, 1));
        config.put("Auto Relock After Timeout", bitSet(response, 3, 2));
        config.put("Lock Confirmation Horn", bitSet(response, 3, 3));
        config.put("Unlock Confirmation Flash", bitSet(response, 3, 4));
        config.put("Double Lock (Deadlock)", bitSet(response, 3, 5));
        return config;
    }

    @Override
    public void setCentralLockBehavior(Map<String, Boolean> config) throws IOException {
        if (config == null || config.isEmpty()) {
            throw new IllegalArgumentException("Config must not be null or empty");
        }

        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte flags = 0;
        if (Boolean.TRUE.equals(config.get("Speed Lock"))) flags |= 0x01;
        if (Boolean.TRUE.equals(config.get("Selective Unlock"))) flags |= 0x02;
        if (Boolean.TRUE.equals(config.get("Auto Relock After Timeout"))) flags |= 0x04;
        if (Boolean.TRUE.equals(config.get("Lock Confirmation Horn"))) flags |= 0x08;
        if (Boolean.TRUE.equals(config.get("Unlock Confirmation Flash"))) flags |= 0x10;
        if (Boolean.TRUE.equals(config.get("Double Lock (Deadlock)"))) flags |= 0x20;

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x03, flags});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readWindowConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        config.put("One Touch Up", bitSet(response, 3, 0) ? "Enabled" : "Disabled");
        config.put("One Touch Down", bitSet(response, 3, 1) ? "Enabled" : "Disabled");
        config.put("Comfort Close via Remote", bitSet(response, 3, 2) ? "Enabled" : "Disabled");
        config.put("Anti-Trap Force Level", String.valueOf((response.length > 4 ? response[4] : 3) & 0xFF));
        return config;
    }

    @Override
    public void setAutoCloseWindows(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : (byte) 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x02, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readMirrorConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3004));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        config.put("Auto Fold on Lock", bitSet(response, 3, 0) ? "Enabled" : "Disabled");
        config.put("Auto Dim", bitSet(response, 3, 1) ? "Enabled" : "Disabled");
        config.put("Tilt on Reverse", bitSet(response, 3, 2) ? "Enabled" : "Disabled");
        return config;
    }

    @Override
    public void setMirrorFoldOnLock(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : (byte) 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x04, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readWiperConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3005));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        config.put("Rain Sensor Active", bitSet(response, 3, 0) ? "Yes" : "No");
        config.put("Rain Sensor Sensitivity", String.valueOf((response.length > 4 ? response[4] : 3) & 0xFF));
        return config;
    }

    @Override
    public void setRainSensorSensitivity(int level) throws IOException {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("Rain sensor sensitivity must be 1-5, got: " + level);
        }

        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x05, (byte) level});
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }

    private static boolean bitSet(byte[] data, int byteIdx, int bitIdx) {
        if (data == null || byteIdx >= data.length) return false;
        return (data[byteIdx] & (1 << bitIdx)) != 0;
    }
}
