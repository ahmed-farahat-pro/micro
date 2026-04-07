package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.cluster.InstrumentClusterSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW instrument cluster system implementation using the KOMBI ECU.
 * Provides language, units, oil/inspection service reset, needle sweep,
 * speed warning, mileage reading, and date/time format configuration.
 */
public class BmwClusterSystem extends InstrumentClusterSystem {

    private static final String KOMBI_ID = "KOMBI";

    public BmwClusterSystem() {
        super();
        addEcu(new EcuDefinition(KOMBI_ID, "KOMBI - Instrument Cluster",
                BmwEcuMap.KOMBI_ADDRESS.getPhysicalId(), BmwEcuMap.KOMBI_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Language Configuration",
                "Units Configuration (metric/imperial)",
                "Oil Service Reset",
                "Inspection Service Reset",
                "Needle Sweep Test",
                "Speed Warning Configuration",
                "Mileage Read",
                "Date/Time Format Configuration"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW KOMBI Instrument Cluster - CAN 0x720/0x728";
    }

    @Override
    public Map<String, String> readClusterConfig() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x4000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 8) {
            config.put("Language", decodeLanguage(response[3] & 0xFF));
            config.put("Speed Units", (response[4] & 0x01) == 0 ? "km/h" : "mph");
            config.put("Temperature Units", (response[4] & 0x02) == 0 ? "Celsius" : "Fahrenheit");
            config.put("Fuel Consumption Units", decodeFuelUnits(response[5] & 0xFF));
            config.put("Needle Sweep", (response[6] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Digital Speedo", (response[6] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Speed Warning Active", (response[7] & 0x01) != 0 ? "Yes" : "No");
            config.put("Speed Warning Threshold km/h", String.valueOf(response[8] & 0xFF));
        }
        return config;
    }

    @Override
    public void setLanguage(String lang) throws IOException {
        if (lang == null) {
            throw new IllegalArgumentException("Language must not be null");
        }

        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x40, 0x10, encodeLanguage(lang)});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setUnits(String unitSystem) throws IOException {
        if (unitSystem == null) {
            throw new IllegalArgumentException("Unit system must not be null");
        }

        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte unitFlags = 0;
        if ("imperial".equalsIgnoreCase(unitSystem)) {
            unitFlags = 0x03; // mph + Fahrenheit
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x40, 0x11, unitFlags});
        conn.getProtocol().readResponse();
    }

    @Override
    public void resetOilService() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        // Reset oil service CBS counter
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x40, 0x01});
        conn.getProtocol().readResponse();
    }

    @Override
    public void resetInspectionService() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        // Reset inspection CBS counter
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x40, 0x02});
        conn.getProtocol().readResponse();
    }

    @Override
    public void performNeedleSweep() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        // Trigger needle sweep test routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x40, 0x10});
        conn.getProtocol().readResponse();
    }

    @Override
    public void setSpeedWarning(int speed) throws IOException {
        if (speed < 0) {
            throw new IllegalArgumentException("Speed must be non-negative, got: " + speed);
        }

        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte active = speed > 0 ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x40, 0x12, active, (byte) (speed & 0xFF)
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public long readMileage() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x4003));
        byte[] response = conn.getProtocol().readResponse();

        if (response.length > 6) {
            return ((response[3] & 0xFFL) << 24) | ((response[4] & 0xFFL) << 16)
                    | ((response[5] & 0xFFL) << 8) | (response[6] & 0xFFL);
        }
        return 0;
    }

    @Override
    public void setDateTimeFormat(String format) throws IOException {
        if (format == null) {
            throw new IllegalArgumentException("Format must not be null");
        }

        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte timeFmt = format.contains("12h") ? (byte) 0x00 : (byte) 0x01; // 24h default
        byte dateFmt;
        if (format.contains("MM/dd")) {
            dateFmt = 0x02; // US format
        } else if (format.contains("yyyy")) {
            dateFmt = 0x03; // ISO format
        } else {
            dateFmt = 0x01; // DD.MM.YYYY European default
        }

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x40, 0x13, timeFmt, dateFmt});
        conn.getProtocol().readResponse();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String decodeLanguage(int code) {
        switch (code) {
            case 0x00: return "de";
            case 0x01: return "en";
            case 0x02: return "fr";
            case 0x03: return "it";
            case 0x04: return "es";
            case 0x05: return "pt";
            case 0x06: return "nl";
            case 0x07: return "tr";
            case 0x08: return "ja";
            case 0x09: return "zh";
            case 0x0A: return "ko";
            case 0x0B: return "ar";
            case 0x0C: return "ru";
            default: return "unknown";
        }
    }

    private static byte encodeLanguage(String lang) {
        switch (lang.toLowerCase()) {
            case "de": case "german": return 0x00;
            case "en": case "english": return 0x01;
            case "fr": case "french": return 0x02;
            case "it": case "italian": return 0x03;
            case "es": case "spanish": return 0x04;
            case "pt": case "portuguese": return 0x05;
            case "nl": case "dutch": return 0x06;
            case "tr": case "turkish": return 0x07;
            case "ja": case "japanese": return 0x08;
            case "zh": case "chinese": return 0x09;
            case "ko": case "korean": return 0x0A;
            case "ar": case "arabic": return 0x0B;
            case "ru": case "russian": return 0x0C;
            default: throw new IllegalArgumentException("Unsupported language: " + lang);
        }
    }

    private static String decodeFuelUnits(int code) {
        switch (code) {
            case 0x00: return "L/100km";
            case 0x01: return "km/L";
            case 0x02: return "MPG (US)";
            case 0x03: return "MPG (UK)";
            default: return "Unknown";
        }
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
