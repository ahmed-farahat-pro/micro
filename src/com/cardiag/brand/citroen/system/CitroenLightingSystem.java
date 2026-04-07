package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.lighting.LightingSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen lighting system implementation using BSI and DAEP ECUs.
 *
 * <p>Controls daytime running lights, cornering lights, follow-me-home,
 * welcome lighting sequences, and the distinctive Citroen C-shaped
 * LED signature lighting.</p>
 */
public class CitroenLightingSystem extends LightingSystem {

    private static final String BSI_ID = "BSI";
    private static final String DAEP_ID = "DAEP";
    private static final int BSI_LOGICAL = 0x764;
    private static final int BSI_PHYSICAL = 0x664;
    private static final int DAEP_LOGICAL = 0x770;
    private static final int DAEP_PHYSICAL = 0x670;

    private static final int DID_DRL_CONFIG = 0x2011;
    private static final int DID_WELCOME_CONFIG = 0x2200;
    private static final int DID_CORNERING_CONFIG = 0x2201;
    private static final int DID_FOG_CONFIG = 0x2202;
    private static final int DID_FOLLOW_HOME = 0x2203;
    private static final int DID_LED_SIGNATURE = 0x2204;

    public CitroenLightingSystem() {
        super();
        addEcu(new EcuDefinition(BSI_ID, "BSI (Lighting Control)", BSI_LOGICAL, BSI_PHYSICAL));
        addEcu(new EcuDefinition(DAEP_ID, "DAEP (Adaptive Headlights)", DAEP_LOGICAL, DAEP_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read/configure daytime running lights (DRL)",
                "Configure DRL intensity",
                "Enable/disable C-shaped LED DRL signature",
                "Read/configure cornering lights (static via fog)",
                "Enable/disable dynamic cornering lights (DAEP)",
                "Read/configure follow-me-home lights and duration",
                "Read/configure welcome lighting sequence",
                "Read/configure fog light settings"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Lighting System - BSI/DAEP (C-Shape LED Signature)";
    }

    @Override
    public Map<String, String> readDrlConfig() throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        byte[] data = readDid(conn, DID_DRL_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("drl_enabled", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("drl_intensity_percent", String.valueOf(data[4] & 0xFF));
            config.put("c_shape_led_signature", (data[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("drl_type", data[6] == 0x02 ? "LED" : data[6] == 0x01 ? "Halogen" : "Off");
        }

        return config;
    }

    @Override
    public void writeDrlConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte flags = 0;
        byte intensity = 0x64;
        byte signature = 0x01;

        if ("Enabled".equals(settings.get("drl_enabled"))) flags |= 0x01;
        if (settings.containsKey("drl_intensity_percent")) {
            intensity = (byte) Integer.parseInt(settings.get("drl_intensity_percent"));
        }
        if ("Disabled".equals(settings.get("c_shape_led_signature"))) signature = 0x00;

        writeDid(proto, DID_DRL_CONFIG, new byte[]{flags, intensity, signature, 0x02});
    }

    @Override
    public Map<String, String> readWelcomeLightConfig() throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        byte[] data = readDid(conn, DID_WELCOME_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 6) {
            config.put("welcome_enabled", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("welcome_duration_s", String.valueOf(data[4] & 0xFF));
            config.put("welcome_sequence", data[5] == 0x01 ? "Standard" : "Extended");
        }

        return config;
    }

    @Override
    public void writeWelcomeLightConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte enabled = "Enabled".equals(settings.get("welcome_enabled")) ? (byte) 0x01 : 0x00;
        byte duration = 0x0A;
        if (settings.containsKey("welcome_duration_s")) {
            duration = (byte) Integer.parseInt(settings.get("welcome_duration_s"));
        }
        byte sequence = "Extended".equals(settings.get("welcome_sequence")) ? (byte) 0x02 : 0x01;

        writeDid(proto, DID_WELCOME_CONFIG, new byte[]{enabled, duration, sequence});
    }

    @Override
    public Map<String, String> readCorneringLightConfig() throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        byte[] data = readDid(conn, DID_CORNERING_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 6) {
            config.put("static_cornering_via_fog", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("dynamic_cornering", (data[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("cornering_activation_speed_kph", String.valueOf(data[4] & 0xFF));
            config.put("cornering_steering_threshold_deg", String.valueOf(data[5] & 0xFF));
        }

        return config;
    }

    @Override
    public void writeCorneringLightConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte flags = 0;
        if ("Enabled".equals(settings.get("static_cornering_via_fog"))) flags |= 0x01;
        if ("Enabled".equals(settings.get("dynamic_cornering"))) flags |= 0x02;

        byte speed = 0x28; // 40 km/h default
        byte angle = 0x14; // 20 degrees default
        if (settings.containsKey("cornering_activation_speed_kph")) {
            speed = (byte) Integer.parseInt(settings.get("cornering_activation_speed_kph"));
        }
        if (settings.containsKey("cornering_steering_threshold_deg")) {
            angle = (byte) Integer.parseInt(settings.get("cornering_steering_threshold_deg"));
        }

        writeDid(proto, DID_CORNERING_CONFIG, new byte[]{flags, speed, angle});
    }

    @Override
    public Map<String, String> readFogLightConfig() throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        byte[] data = readDid(conn, DID_FOG_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 5) {
            config.put("front_fog_lights", (data[3] & 0x01) != 0 ? "Present" : "Not present");
            config.put("rear_fog_light", (data[3] & 0x02) != 0 ? "Present" : "Not present");
            config.put("auto_fog_deactivation", (data[4] & 0x01) != 0 ? "Enabled" : "Disabled");
        }

        return config;
    }

    @Override
    public void writeFogLightConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte config = 0;
        if ("Present".equals(settings.get("front_fog_lights"))) config |= 0x01;
        if ("Present".equals(settings.get("rear_fog_light"))) config |= 0x02;
        byte autoDeact = "Enabled".equals(settings.get("auto_fog_deactivation")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_FOG_CONFIG, new byte[]{config, autoDeact});
    }

    /**
     * Reads the follow-me-home lighting configuration.
     *
     * @return a map of follow-me-home settings
     * @throws IOException if reading fails
     */
    public Map<String, String> readFollowMeHomeConfig() throws IOException {
        EcuConnection conn = connections.get(BSI_ID);
        byte[] data = readDid(conn, DID_FOLLOW_HOME);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 5) {
            config.put("follow_me_home_enabled", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("duration_seconds", String.valueOf(data[4] & 0xFF));
        }

        return config;
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
        proto.sendRequest(new byte[]{0x27, 0x03});
        proto.readResponse();
    }
}
