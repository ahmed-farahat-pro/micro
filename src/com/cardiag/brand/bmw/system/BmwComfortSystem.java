package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.comfort.ComfortSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW comfort system implementation using FRM and CAS modules.
 * Provides seat memory, ambient lighting color, comfort access,
 * and panic alarm configuration.
 */
public class BmwComfortSystem extends ComfortSystem {

    private static final String FRM_ID = "FRM";
    private static final String CAS_ID = "CAS";

    public BmwComfortSystem() {
        super();
        addEcu(new EcuDefinition(FRM_ID, "FRM - Footwell Module",
                BmwEcuMap.FRM_ADDRESS.getPhysicalId(), BmwEcuMap.FRM_ADDRESS.getResponseId()));
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
                "Seat Memory Read/Write",
                "Ambient Lighting Color Configuration",
                "Comfort Access Toggle",
                "Comfort Closing via Remote",
                "Key Configuration Read",
                "Panic Alarm Duration Adjustment"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW Comfort System - FRM (0x6C0) + CAS (0x640)";
    }

    @Override
    public Map<Integer, byte[]> readSeatMemoryPositions() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        Map<Integer, byte[]> positions = new LinkedHashMap<>();

        for (int slot = 1; slot <= 3; slot++) {
            int did = 0x3100 + slot;
            conn.getProtocol().sendRequest(buildReadDid(did));
            byte[] response = conn.getProtocol().readResponse();
            byte[] posData = new byte[Math.max(0, response.length - 3)];
            if (response.length > 3) {
                System.arraycopy(response, 3, posData, 0, posData.length);
            }
            positions.put(slot, posData);
        }
        return positions;
    }

    @Override
    public void setSeatMemory(int slot, byte[] positionData) throws IOException {
        if (slot < 1 || slot > 3) {
            throw new IllegalArgumentException("Seat memory slot must be 1-3, got: " + slot);
        }
        if (positionData == null) {
            throw new IllegalArgumentException("Position data must not be null");
        }

        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        int did = 0x3100 + slot;
        byte[] request = new byte[3 + positionData.length];
        request[0] = 0x2E;
        request[1] = (byte) ((did >> 8) & 0xFF);
        request[2] = (byte) (did & 0xFF);
        System.arraycopy(positionData, 0, request, 3, positionData.length);
        conn.getProtocol().sendRequest(request);
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readAmbientLightingConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3200));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Red", String.valueOf(response[4] & 0xFF));
            config.put("Green", String.valueOf(response[5] & 0xFF));
            config.put("Blue", response.length > 6 ? String.valueOf(response[6] & 0xFF) : "0");
            config.put("Brightness", response.length > 7 ? String.valueOf(response[7] & 0xFF) + "%" : "100%");
        }
        return config;
    }

    @Override
    public void setAmbientLightColor(int red, int green, int blue) throws IOException {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Color values must be 0-255");
        }

        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x32, 0x00,
                (byte) red, (byte) green, (byte) blue
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readConvenienceConfig() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xD020));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 3) {
            config.put("Comfort Access", (response[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Comfort Closing", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Keyless Go", (response[3] & 0x04) != 0 ? "Enabled" : "Disabled");
            config.put("Auto Relock", (response[3] & 0x08) != 0 ? "Enabled" : "Disabled");
        }
        return config;
    }

    @Override
    public void setComfortClosing(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : (byte) 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0xD0, 0x21, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readKeyConfig() throws IOException {
        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(buildReadDid(0xD000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 3) {
            config.put("Number of Keys", String.valueOf(response[3] & 0xFF));
            config.put("Key Slot 1", response.length > 4 && response[4] != 0 ? "Programmed" : "Empty");
            config.put("Key Slot 2", response.length > 5 && response[5] != 0 ? "Programmed" : "Empty");
            config.put("Key Slot 3", response.length > 6 && response[6] != 0 ? "Programmed" : "Empty");
            config.put("Key Slot 4", response.length > 7 && response[7] != 0 ? "Programmed" : "Empty");
        }
        return config;
    }

    @Override
    public void setPanicAlarmDuration(int seconds) throws IOException {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Duration must be positive, got: " + seconds);
        }

        EcuConnection conn = connections.get(CAS_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0xD0, 0x30, (byte) (seconds & 0xFF)
        });
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
