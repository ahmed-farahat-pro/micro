package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.lighting.LightingSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW lighting system implementation using the FRM (Footwell Module).
 * Provides coding for angel eyes, DRL brightness, welcome lights,
 * cornering lights, fog-as-cornering, and LED tail light coding.
 */
public class BmwLightingSystem extends LightingSystem {

    private static final String FRM_ID = "FRM";

    public BmwLightingSystem() {
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
                "Angel Eyes Configuration",
                "DRL Brightness Adjustment",
                "Welcome Lights Configuration",
                "Cornering Lights Toggle",
                "Fog Light as Cornering Light",
                "LED Tail Light Coding",
                "Adaptive Headlight Configuration",
                "Brake Force Display",
                "Footwell Light Color"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW FRM Lighting Control - CAN 0x6C0/0x6C8";
    }

    @Override
    public Map<String, String> readHeadlightConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("DRL Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("DRL Brightness", (response[4] & 0xFF) + "%");
            config.put("Angel Eyes Active", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("Angel Eyes as DRL", (response[3] & 0x04) != 0 ? "Yes" : "No");
            config.put("Adaptive Headlights", (response[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Welcome Lights", (response[5] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Cornering Lights", (response[5] & 0x04) != 0 ? "Enabled" : "Disabled");
        }
        return config;
    }

    @Override
    public void setDaytimeRunningLights(boolean enabled, int brightness) throws IOException {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness must be 0-100, got: " + brightness);
        }

        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x30, 0x01, flags, (byte) brightness
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public void setAdaptiveHeadlights(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x13, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setWelcomeLights(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x10, flags, 0x0A});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setCorneringLights(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x11, flags});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readTailLightConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3014));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 4) {
            config.put("LED Tail Coding", (response[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Brake Force Display", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Brake Light Intensity %", String.valueOf(response[4] & 0xFF));
        }
        return config;
    }

    @Override
    public void setBrakeLightIntensity(int intensity) throws IOException {
        if (intensity < 0 || intensity > 100) {
            throw new IllegalArgumentException("Intensity must be 0-100, got: " + intensity);
        }

        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x14, (byte) intensity});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setFogLightAsCorneringLight(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x15, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readInteriorLightConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3016));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 6) {
            config.put("Footwell Lights", (response[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            int color = ((response[4] & 0xFF) << 16) | ((response[5] & 0xFF) << 8) | (response[6] & 0xFF);
            config.put("Footwell Color", String.format("#%06X", color));
            config.put("Ambient Lighting", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
        }
        return config;
    }

    @Override
    public void setFootwellLights(boolean enabled, int color) throws IOException {
        if (color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException("Color must be 0x000000-0xFFFFFF");
        }

        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = enabled ? (byte) 0x01 : 0x00;
        byte r = (byte) ((color >> 16) & 0xFF);
        byte g = (byte) ((color >> 8) & 0xFF);
        byte b = (byte) (color & 0xFF);

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x16, flags, r, g, b});
        conn.getProtocol().readResponse();
    }

    private void enterCodingSession(EcuConnection conn) throws IOException {
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
