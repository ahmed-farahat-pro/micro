package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.cluster.InstrumentClusterSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen instrument cluster system implementation for the CMB.
 *
 * <p>Manages cluster coding including language selection, unit configuration
 * (km/h vs mph, Celsius vs Fahrenheit), trip computer pages, speed alert
 * thresholds, service interval display, and display brightness settings.</p>
 */
public class CitroenClusterSystem extends InstrumentClusterSystem {

    private static final String ECU_ID = "CMB";
    private static final String ECU_NAME = "CMB (Combined Instrument Cluster)";
    private static final int CMB_LOGICAL = 0x766;
    private static final int CMB_PHYSICAL = 0x666;

    private static final int DID_CLUSTER_CONFIG = 0x4001;
    private static final int DID_LANGUAGE = 0x4002;
    private static final int DID_UNITS = 0x4003;
    private static final int DID_ODOMETER = 0x4004;
    private static final int DID_TRIP_A = 0x4005;
    private static final int DID_TRIP_B = 0x4006;
    private static final int DID_SERVICE_INTERVAL = 0x4010;
    private static final int DID_SPEED_ALERT = 0x4020;
    private static final int DID_BRIGHTNESS = 0x4030;

    private static final int ROUTINE_RESET_OIL = 0x4F01;
    private static final int ROUTINE_RESET_INSPECTION = 0x4F02;

    private static final Map<Integer, String> LANGUAGE_MAP = new LinkedHashMap<>();
    static {
        LANGUAGE_MAP.put(0, "Fran\u00e7ais");
        LANGUAGE_MAP.put(1, "Deutsch");
        LANGUAGE_MAP.put(2, "Espa\u00f1ol");
        LANGUAGE_MAP.put(3, "English");
        LANGUAGE_MAP.put(4, "Italiano");
        LANGUAGE_MAP.put(5, "Portugu\u00eas");
        LANGUAGE_MAP.put(6, "Nederlands");
        LANGUAGE_MAP.put(7, "Polski");
        LANGUAGE_MAP.put(8, "\u010ce\u0161tina");
        LANGUAGE_MAP.put(9, "T\u00fcrk\u00e7e");
    }

    public CitroenClusterSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, CMB_LOGICAL, CMB_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/set display language",
                "Read/set measurement units (speed, temperature, fuel consumption)",
                "Read/configure speed alert threshold",
                "Read/configure cluster brightness (day/night)",
                "Reset oil service indicator",
                "Reset inspection interval",
                "Read service history",
                "Read odometer and trip distances",
                "Configure trip computer pages"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Instrument Cluster System - CMB";
    }

    @Override
    public Map<String, String> readClusterConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_CLUSTER_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 10) {
            config.put("language", LANGUAGE_MAP.getOrDefault(data[3] & 0xFF, "Unknown"));
            config.put("speed_units", (data[4] & 0x01) == 0 ? "km/h" : "mph");
            config.put("temperature_units", (data[4] & 0x02) == 0 ? "Celsius" : "Fahrenheit");
            config.put("consumption_units",
                    (data[5] & 0x03) == 0 ? "L/100km" :
                    (data[5] & 0x03) == 1 ? "mpg" : "km/L");
            config.put("tachometer", (data[6] & 0x01) != 0 ? "Present" : "Absent");
            config.put("digital_speed_display", (data[6] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("gear_indicator", (data[7] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("trip_pages", String.valueOf(data[8] & 0xFF));
            config.put("service_type", data[9] == 0x02 ? "Flexible" : "Fixed interval");
        }

        return config;
    }

    @Override
    public void writeClusterConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        // Read current config then apply changes
        byte[] current = readDid(conn, DID_CLUSTER_CONFIG);
        if (current != null && current.length >= 10) {
            byte[] newConfig = Arrays.copyOfRange(current, 3, current.length);

            if (settings.containsKey("digital_speed_display")) {
                if ("Enabled".equals(settings.get("digital_speed_display"))) {
                    newConfig[3] |= 0x02;
                } else {
                    newConfig[3] &= ~0x02;
                }
            }
            if (settings.containsKey("gear_indicator")) {
                if ("Enabled".equals(settings.get("gear_indicator"))) {
                    newConfig[4] |= 0x01;
                } else {
                    newConfig[4] &= ~0x01;
                }
            }

            writeDid(proto, DID_CLUSTER_CONFIG, newConfig);
        }
    }

    @Override
    public void resetOilService() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_RESET_OIL, new byte[0]);
    }

    @Override
    public void resetInspection() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_RESET_INSPECTION, new byte[0]);
    }

    @Override
    public Map<String, String> readServiceHistory() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_SERVICE_INTERVAL);

        Map<String, String> history = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            int kmRemaining = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
            int daysRemaining = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
            history.put("km_to_next_service", String.valueOf(kmRemaining * 100));
            history.put("days_to_next_service", String.valueOf(daysRemaining));
        }

        // Read odometer
        byte[] odo = readDid(conn, DID_ODOMETER);
        if (odo != null && odo.length >= 6) {
            int totalKm = ((odo[3] & 0xFF) << 16) | ((odo[4] & 0xFF) << 8) | (odo[5] & 0xFF);
            history.put("total_odometer_km", String.valueOf(totalKm));
        }

        return history;
    }

    @Override
    public String readLanguage() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_LANGUAGE);

        if (data != null && data.length >= 4) {
            return LANGUAGE_MAP.getOrDefault(data[3] & 0xFF, "Unknown");
        }
        return "Unknown";
    }

    @Override
    public void setLanguage(String language) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        int langCode = 3; // Default English
        for (Map.Entry<Integer, String> entry : LANGUAGE_MAP.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(language)) {
                langCode = entry.getKey();
                break;
            }
        }

        writeDid(proto, DID_LANGUAGE, new byte[]{(byte) langCode});
    }

    @Override
    public String readUnits() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_UNITS);

        if (data != null && data.length >= 5) {
            String speed = (data[3] & 0x01) == 0 ? "km/h" : "mph";
            String temp = (data[3] & 0x02) == 0 ? "Celsius" : "Fahrenheit";
            String consumption = (data[4] & 0x03) == 0 ? "L/100km" :
                    (data[4] & 0x03) == 1 ? "mpg" : "km/L";
            return "Speed: " + speed + ", Temp: " + temp + ", Consumption: " + consumption;
        }
        return "Unknown";
    }

    @Override
    public void setUnits(String units) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte speedTemp = 0;
        byte consumption = 0;

        if (units.contains("mph")) speedTemp |= 0x01;
        if (units.contains("Fahrenheit")) speedTemp |= 0x02;
        if (units.contains("mpg")) consumption = 0x01;
        else if (units.contains("km/L")) consumption = 0x02;

        writeDid(proto, DID_UNITS, new byte[]{speedTemp, consumption});
    }

    // -- Helpers --

    private byte[] readDid(EcuConnection conn, int did) throws IOException {
        DiagnosticProtocol proto = conn.getProtocol();
        byte[] request = new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
        proto.sendRequest(request);
        return proto.readResponse();
    }

    private void writeDid(DiagnosticProtocol proto, int did, byte[] data) throws IOException {
        byte[] request = new byte[3 + data.length];
        request[0] = 0x2E;
        request[1] = (byte) ((did >> 8) & 0xFF);
        request[2] = (byte) (did & 0xFF);
        System.arraycopy(data, 0, request, 3, data.length);
        proto.sendRequest(request);
        proto.readResponse();
    }

    private void sendSessionControl(DiagnosticProtocol proto, int session) throws IOException {
        proto.sendRequest(new byte[]{0x10, (byte) session});
        proto.readResponse();
    }

    private void sendSecurityAccess(DiagnosticProtocol proto) throws IOException {
        proto.sendRequest(new byte[]{0x27, 0x01});
        proto.readResponse();
    }

    private void executeRoutine(DiagnosticProtocol proto, int routineId, byte[] data) throws IOException {
        byte[] request = new byte[4 + data.length];
        request[0] = 0x31;
        request[1] = 0x01;
        request[2] = (byte) ((routineId >> 8) & 0xFF);
        request[3] = (byte) (routineId & 0xFF);
        System.arraycopy(data, 0, request, 4, data.length);
        proto.sendRequest(request);
        proto.readResponse();
    }
}
