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
 * Provides language, units, oil/inspection service reset, needle sweep
 * configuration, and speed warning coding.
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
                "Units Configuration (km/h, mph, Celsius, Fahrenheit)",
                "Oil Service Reset",
                "Inspection Service Reset",
                "Needle Sweep Toggle",
                "Digital Speedo Toggle",
                "Speed Warning Configuration",
                "Date/Time Format",
                "Service History Read"
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
    public void writeClusterConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte lang = encodeLanguage(settings.getOrDefault("Language", "English"));

        byte units = 0;
        if ("mph".equalsIgnoreCase(settings.get("Speed Units"))) units |= 0x01;
        if ("Fahrenheit".equalsIgnoreCase(settings.get("Temperature Units"))) units |= 0x02;

        byte fuelUnits = encodeFuelUnits(settings.getOrDefault("Fuel Consumption Units", "L/100km"));

        byte display = 0;
        if ("Enabled".equalsIgnoreCase(settings.get("Needle Sweep"))) display |= 0x01;
        if ("Enabled".equalsIgnoreCase(settings.get("Digital Speedo"))) display |= 0x02;

        byte warning = "Yes".equalsIgnoreCase(settings.get("Speed Warning Active")) ? (byte) 0x01 : 0x00;
        byte threshold = 0x78; // 120 km/h default
        if (settings.containsKey("Speed Warning Threshold km/h")) {
            threshold = (byte) (Integer.parseInt(settings.get("Speed Warning Threshold km/h")) & 0xFF);
        }

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, 0x40, 0x00,
                lang, units, fuelUnits, display, warning, threshold
        });
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
    public void resetInspection() throws IOException {
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
    public Map<String, String> readServiceHistory() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x4000));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> history = new LinkedHashMap<>();

        // Read oil service data
        conn.getProtocol().sendRequest(buildReadDid(0x4001));
        byte[] oilResp = conn.getProtocol().readResponse();
        if (oilResp.length > 6) {
            int oilKm = ((oilResp[3] & 0xFF) << 8) | (oilResp[4] & 0xFF);
            int oilDays = ((oilResp[5] & 0xFF) << 8) | (oilResp[6] & 0xFF);
            history.put("Oil Service Remaining km", String.valueOf(oilKm * 100));
            history.put("Oil Service Remaining days", String.valueOf(oilDays));
        }

        // Read inspection data
        conn.getProtocol().sendRequest(buildReadDid(0x4002));
        byte[] inspResp = conn.getProtocol().readResponse();
        if (inspResp.length > 6) {
            int inspKm = ((inspResp[3] & 0xFF) << 8) | (inspResp[4] & 0xFF);
            int inspDays = ((inspResp[5] & 0xFF) << 8) | (inspResp[6] & 0xFF);
            history.put("Inspection Remaining km", String.valueOf(inspKm * 100));
            history.put("Inspection Remaining days", String.valueOf(inspDays));
        }

        // Read total mileage
        conn.getProtocol().sendRequest(buildReadDid(0x4003));
        byte[] mileResp = conn.getProtocol().readResponse();
        if (mileResp.length > 6) {
            long mileage = ((mileResp[3] & 0xFFL) << 24) | ((mileResp[4] & 0xFFL) << 16)
                    | ((mileResp[5] & 0xFFL) << 8) | (mileResp[6] & 0xFFL);
            history.put("Total Mileage km", String.valueOf(mileage));
        }

        return history;
    }

    @Override
    public String readLanguage() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x4000));
        byte[] response = conn.getProtocol().readResponse();
        return response.length > 3 ? decodeLanguage(response[3] & 0xFF) : "Unknown";
    }

    @Override
    public void setLanguage(String language) throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x40, 0x10, encodeLanguage(language)});
        conn.getProtocol().readResponse();
    }

    @Override
    public String readUnits() throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x4000));
        byte[] response = conn.getProtocol().readResponse();
        if (response.length > 4) {
            boolean isMph = (response[4] & 0x01) != 0;
            boolean isFahr = (response[4] & 0x02) != 0;
            return (isMph ? "mph" : "km/h") + " / " + (isFahr ? "Fahrenheit" : "Celsius");
        }
        return "Unknown";
    }

    @Override
    public void setUnits(String units) throws IOException {
        EcuConnection conn = connections.get(KOMBI_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x01});
        conn.getProtocol().readResponse();

        byte unitFlags = 0;
        if (units.toLowerCase().contains("mph")) unitFlags |= 0x01;
        if (units.toLowerCase().contains("fahrenheit")) unitFlags |= 0x02;

        conn.getProtocol().sendRequest(new byte[]{0x2E, 0x40, 0x11, unitFlags});
        conn.getProtocol().readResponse();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String decodeLanguage(int code) {
        switch (code) {
            case 0x00: return "German";
            case 0x01: return "English";
            case 0x02: return "French";
            case 0x03: return "Italian";
            case 0x04: return "Spanish";
            case 0x05: return "Portuguese";
            case 0x06: return "Dutch";
            case 0x07: return "Turkish";
            case 0x08: return "Japanese";
            case 0x09: return "Chinese";
            case 0x0A: return "Korean";
            case 0x0B: return "Arabic";
            case 0x0C: return "Russian";
            default: return "Unknown (" + code + ")";
        }
    }

    private static byte encodeLanguage(String language) {
        switch (language.toLowerCase()) {
            case "german": return 0x00;
            case "english": return 0x01;
            case "french": return 0x02;
            case "italian": return 0x03;
            case "spanish": return 0x04;
            case "portuguese": return 0x05;
            case "dutch": return 0x06;
            case "turkish": return 0x07;
            case "japanese": return 0x08;
            case "chinese": return 0x09;
            case "korean": return 0x0A;
            case "arabic": return 0x0B;
            case "russian": return 0x0C;
            default: return 0x01; // Default to English
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

    private static byte encodeFuelUnits(String units) {
        switch (units.toLowerCase()) {
            case "l/100km": return 0x00;
            case "km/l": return 0x01;
            case "mpg (us)": case "mpg": return 0x02;
            case "mpg (uk)": return 0x03;
            default: return 0x00;
        }
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
