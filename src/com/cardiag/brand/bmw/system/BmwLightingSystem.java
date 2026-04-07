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
                "Brake Force Display"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW FRM Lighting Control - CAN 0x6C0/0x6C8";
    }

    @Override
    public Map<String, String> readDrlConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("DRL Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("DRL Brightness", (response[4] & 0xFF) + "%");
            config.put("Angel Eyes Active", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("Angel Eyes as DRL", (response[3] & 0x04) != 0 ? "Yes" : "No");
        }
        return config;
    }

    @Override
    public void writeDrlConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("DRL Active"))) flags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("Angel Eyes Active"))) flags |= 0x02;
        if ("Yes".equalsIgnoreCase(settings.get("Angel Eyes as DRL"))) flags |= 0x04;

        int brightness = 100;
        if (settings.containsKey("DRL Brightness")) {
            brightness = Integer.parseInt(settings.get("DRL Brightness").replace("%", ""));
        }

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x30, 0x01, flags, (byte) brightness
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readWelcomeLightConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 4) {
            config.put("Welcome Lights Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Duration Seconds", String.valueOf(response[4] & 0xFF));
            config.put("Pathway Lighting", (response[3] & 0x02) != 0 ? "Yes" : "No");
        }
        return config;
    }

    @Override
    public void writeWelcomeLightConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("Welcome Lights Active"))) flags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("Pathway Lighting"))) flags |= 0x02;

        int duration = 10;
        if (settings.containsKey("Duration Seconds")) {
            duration = Integer.parseInt(settings.get("Duration Seconds"));
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x10, flags, (byte) duration});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readCorneringLightConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3011));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 3) {
            config.put("Cornering Lights Active", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Fog as Cornering", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("Activation Speed km/h", response.length > 4 ? String.valueOf(response[4] & 0xFF) : "40");
        }
        return config;
    }

    @Override
    public void writeCorneringLightConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("Cornering Lights Active"))) flags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("Fog as Cornering"))) flags |= 0x02;

        int speed = 40;
        if (settings.containsKey("Activation Speed km/h")) {
            speed = Integer.parseInt(settings.get("Activation Speed km/h"));
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x11, flags, (byte) speed});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readFogLightConfig() throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x3012));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 3) {
            config.put("Front Fog Lights Installed", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Rear Fog Light", (response[3] & 0x02) != 0 ? "Yes" : "No");
            config.put("Fog Light Auto Off", (response[3] & 0x04) != 0 ? "Yes" : "No");
        }
        return config;
    }

    @Override
    public void writeFogLightConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(FRM_ID);
        enterCodingSession(conn);

        byte flags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("Front Fog Lights Installed"))) flags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("Rear Fog Light"))) flags |= 0x02;
        if ("Yes".equalsIgnoreCase(settings.get("Fog Light Auto Off"))) flags |= 0x04;

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x30, 0x12, flags});
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
