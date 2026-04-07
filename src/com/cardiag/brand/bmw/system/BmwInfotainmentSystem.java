package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.infotainment.InfotainmentSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW infotainment system implementation using the CIC/NBT head unit ECU.
 * Provides region coding, video in motion unlock, Bluetooth reset,
 * navigation voice volume, installed apps listing, and startup screen config.
 */
public class BmwInfotainmentSystem extends InfotainmentSystem {

    private static final String CIC_ID = "CIC";

    public BmwInfotainmentSystem() {
        super();
        addEcu(new EcuDefinition(CIC_ID, "CIC - Head Unit",
                BmwEcuMap.CIC_ADDRESS.getPhysicalId(), BmwEcuMap.CIC_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Region Coding (EU/US/Asia/Middle East)",
                "Video in Motion Unlock",
                "Bluetooth Pairing Reset",
                "Navigation Voice Volume",
                "Installed Apps Read",
                "Startup Screen Configuration",
                "DVD Region Coding"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW CIC/NBT Infotainment System - CAN 0x6F1/0x6F9";
    }

    @Override
    public Map<String, String> readRegionCoding() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x5000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> coding = new LinkedHashMap<>();
        if (response.length > 5) {
            coding.put("Region", decodeRegion(response[4] & 0xFF));
            coding.put("DVD Region", String.valueOf(response[5] & 0xFF));
            coding.put("Head Unit Type", decodeHeadUnitType(response[3] & 0xFF));
        }
        return coding;
    }

    @Override
    public void setRegionCode(String region) throws IOException {
        if (region == null) {
            throw new IllegalArgumentException("Region must not be null");
        }

        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x50, 0x10, encodeRegion(region)});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setVideoInMotion(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x50, 0x11, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readBluetoothConfig() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x5002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 5) {
            config.put("Module Version", String.format("%d.%d", response[3] & 0xFF, response[4] & 0xFF));
            config.put("Paired Devices", String.valueOf(response[5] & 0xFF));
            config.put("Audio Streaming", response.length > 6 && (response[6] & 0x01) != 0 ? "Active" : "Inactive");
        }
        return config;
    }

    @Override
    public void resetBluetoothPairings() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        // Bluetooth module reset routine - clears all pairings
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x50, 0x20});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setNaviVoiceVolume(int volume) throws IOException {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("Volume must be 0-100, got: " + volume);
        }

        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x50, 0x12, (byte) volume});
        conn.getProtocol().readResponse();
    }

    @Override
    public List<String> readInstalledApps() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x5010));
        byte[] response = conn.getProtocol().readResponse();

        List<String> apps = new ArrayList<>();
        apps.add("BMW Connected");
        apps.add("Navigation");
        apps.add("Media Player");
        apps.add("Radio");
        apps.add("Phone");
        apps.add("Settings");

        // Check optional apps from ECU response
        if (response.length > 3) {
            if ((response[3] & 0x01) != 0) apps.add("BMW Apps");
            if ((response[3] & 0x02) != 0) apps.add("Internet Browser");
            if ((response[3] & 0x04) != 0) apps.add("Office");
            if ((response[3] & 0x08) != 0) apps.add("Spotify");
        }
        return apps;
    }

    @Override
    public void setStartupScreen(String screen) throws IOException {
        if (screen == null) {
            throw new IllegalArgumentException("Screen must not be null");
        }

        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        byte screenCode;
        switch (screen.toLowerCase()) {
            case "navigation": case "nav": screenCode = 0x01; break;
            case "media": screenCode = 0x02; break;
            case "home": screenCode = 0x03; break;
            case "climate": screenCode = 0x04; break;
            case "radio": screenCode = 0x05; break;
            default: throw new IllegalArgumentException("Unknown startup screen: " + screen);
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x50, 0x13, screenCode});
        conn.getProtocol().readResponse();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String decodeHeadUnitType(int code) {
        switch (code) {
            case 0x01: return "CIC Basic";
            case 0x02: return "CIC Professional";
            case 0x03: return "NBT";
            case 0x04: return "NBT EVO";
            case 0x05: return "MGU/Live Cockpit";
            default: return "Unknown";
        }
    }

    private static String decodeRegion(int code) {
        switch (code) {
            case 0x01: return "EU";
            case 0x02: return "US";
            case 0x03: return "Asia";
            case 0x04: return "Middle East";
            case 0x05: return "Korea";
            case 0x06: return "Japan";
            case 0x07: return "China";
            default: return "Unknown";
        }
    }

    private static byte encodeRegion(String region) {
        switch (region.toUpperCase()) {
            case "EU": case "EUROPE": return 0x01;
            case "US": case "USA": case "NORTH AMERICA": return 0x02;
            case "ASIA": case "ASIA PACIFIC": return 0x03;
            case "MIDDLE EAST": return 0x04;
            case "KOREA": return 0x05;
            case "JP": case "JAPAN": return 0x06;
            case "CHINA": return 0x07;
            default: throw new IllegalArgumentException("Unknown region: " + region);
        }
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
