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
 * Provides region coding, video in motion unlock, and Bluetooth reset.
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
                "DVD Region Coding",
                "Bluetooth Reset",
                "Voice Control Configuration",
                "Navigation Configuration",
                "USB/AUX Configuration",
                "Rear View Camera Activation"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW CIC/NBT Infotainment System - CAN 0x6F1/0x6F9";
    }

    @Override
    public Map<String, String> readInfotainmentConfig() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x5000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 8) {
            config.put("Head Unit Type", decodeHeadUnitType(response[3] & 0xFF));
            config.put("Region", decodeRegion(response[4] & 0xFF));
            config.put("DVD Region", String.valueOf(response[5] & 0xFF));
            config.put("Video in Motion", (response[6] & 0x01) != 0 ? "Unlocked" : "Locked");
            config.put("Voice Control", (response[6] & 0x02) != 0 ? "Active" : "Inactive");
            config.put("Navigation", (response[7] & 0x01) != 0 ? "Active" : "Inactive");
            config.put("Bluetooth", (response[7] & 0x02) != 0 ? "Active" : "Inactive");
            config.put("Bluetooth Audio Streaming", (response[7] & 0x04) != 0 ? "Active" : "Inactive");
            config.put("Rear View Camera", (response[8] & 0x01) != 0 ? "Active" : "Inactive");
        }
        return config;
    }

    @Override
    public void writeInfotainmentConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        byte region = encodeRegion(settings.getOrDefault("Region", "Europe"));
        byte dvdRegion = (byte) Integer.parseInt(settings.getOrDefault("DVD Region", "2"));

        byte mediaFlags = 0;
        if ("Unlocked".equalsIgnoreCase(settings.get("Video in Motion"))) mediaFlags |= 0x01;
        if ("Active".equalsIgnoreCase(settings.get("Voice Control"))) mediaFlags |= 0x02;

        byte connFlags = 0;
        if ("Active".equalsIgnoreCase(settings.get("Navigation"))) connFlags |= 0x01;
        if ("Active".equalsIgnoreCase(settings.get("Bluetooth"))) connFlags |= 0x02;
        if ("Active".equalsIgnoreCase(settings.get("Bluetooth Audio Streaming"))) connFlags |= 0x04;

        byte cameraFlag = "Active".equalsIgnoreCase(settings.get("Rear View Camera")) ? (byte) 0x01 : 0x00;

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x50, 0x00,
                0x00, region, dvdRegion, mediaFlags, connFlags, cameraFlag
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public String readRegionCode() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x5000));
        byte[] response = conn.getProtocol().readResponse();
        return response.length > 4 ? decodeRegion(response[4] & 0xFF) : "Unknown";
    }

    @Override
    public void setRegionCode(String region) throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x50, 0x10, encodeRegion(region)});
        conn.getProtocol().readResponse();
    }

    @Override
    public void resetBluetooth() throws IOException {
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
    public Map<String, String> readMediaStatus() throws IOException {
        EcuConnection conn = connections.get(CIC_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x5002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> status = new LinkedHashMap<>();
        if (response.length > 5) {
            status.put("Bluetooth Module Version", String.format("%d.%d", response[3] & 0xFF, response[4] & 0xFF));
            status.put("Paired Devices", String.valueOf(response[5] & 0xFF));
        }

        conn.getProtocol().sendRequest(buildReadDid(0x5001));
        byte[] navResp = conn.getProtocol().readResponse();
        if (navResp.length > 5) {
            status.put("Navigation DB Version", String.format("%d.%d.%d",
                    navResp[3] & 0xFF, navResp[4] & 0xFF, navResp[5] & 0xFF));
        }

        return status;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String decodeHeadUnitType(int code) {
        switch (code) {
            case 0x01: return "CIC Basic";
            case 0x02: return "CIC Professional";
            case 0x03: return "NBT";
            case 0x04: return "NBT EVO";
            case 0x05: return "MGU/Live Cockpit";
            default: return "Unknown (0x" + Integer.toHexString(code) + ")";
        }
    }

    private static String decodeRegion(int code) {
        switch (code) {
            case 0x01: return "Europe";
            case 0x02: return "North America";
            case 0x03: return "Asia Pacific";
            case 0x04: return "Middle East";
            case 0x05: return "Korea";
            case 0x06: return "Japan";
            case 0x07: return "China";
            default: return "Unknown (" + code + ")";
        }
    }

    private static byte encodeRegion(String region) {
        switch (region.toLowerCase()) {
            case "europe": case "eu": return 0x01;
            case "north america": case "us": case "usa": return 0x02;
            case "asia pacific": case "asia": return 0x03;
            case "middle east": return 0x04;
            case "korea": return 0x05;
            case "japan": return 0x06;
            case "china": return 0x07;
            default: return 0x01;
        }
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
