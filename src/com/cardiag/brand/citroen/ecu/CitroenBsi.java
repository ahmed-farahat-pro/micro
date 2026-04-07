package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * BSI (Boitier de Servitude Intelligent) - the central body controller for Citroen vehicles.
 *
 * <p>The BSI is the heart of the PSA electrical architecture, managing central locking,
 * window lifts, exterior and interior lighting, alarm system, immobilizer interface,
 * wiper control, and inter-ECU communication routing. CAN address 0x764/0x664.</p>
 *
 * <p>This ECU has the most extensive coding map of any Citroen module, controlling
 * virtually all body electrical functions.</p>
 */
public class CitroenBsi extends EcuDefinition {

    private static final String ECU_NAME = "BSI (Boitier de Servitude Intelligent)";
    private static final EcuAddress ADDRESS = new EcuAddress(0x764, 0x7DF, 0x664, "BSI");
    private static final String PROTOCOL = "UDS";

    public CitroenBsi() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        // Central locking
        map.put("central_lock_speed_lock", 0x01);        // auto lock at speed
        map.put("central_lock_speed_threshold_kph", 10);  // speed lock threshold
        map.put("central_lock_selective_unlock", 0x01);   // driver door only first press
        map.put("central_lock_relock_timeout_s", 30);     // auto relock after N seconds
        map.put("central_lock_deadlocking", 0x01);        // deadlocking enabled
        map.put("central_lock_confirmation_horn", 0x00);  // horn chirp on lock
        map.put("central_lock_confirmation_lights", 0x01); // lights flash on lock

        // Windows
        map.put("window_one_touch_up", 0x01);             // one-touch close all
        map.put("window_one_touch_down", 0x01);            // one-touch open all
        map.put("window_close_on_lock", 0x00);            // close windows on lock
        map.put("window_open_on_unlock", 0x00);           // open windows on unlock
        map.put("window_anti_pinch_sensitivity", 0x03);    // 1-5

        // Mirrors
        map.put("mirror_fold_on_lock", 0x00);              // auto fold on lock
        map.put("mirror_unfold_on_unlock", 0x00);          // auto unfold on unlock
        map.put("mirror_tilt_on_reverse", 0x01);           // passenger mirror dips in reverse
        map.put("mirror_heating_auto", 0x01);              // auto heated mirrors

        // Lighting
        map.put("drl_enabled", 0x01);                      // daytime running lights
        map.put("drl_intensity", 0x64);                    // DRL intensity (0-100%)
        map.put("follow_me_home_enabled", 0x01);           // follow-me-home lights
        map.put("follow_me_home_duration_s", 30);          // follow-me-home duration
        map.put("cornering_lights", 0x01);                 // cornering fog lights
        map.put("welcome_lighting", 0x01);                 // welcome light sequence
        map.put("interior_dimming_level", 0x05);           // interior light dimming 0-10
        map.put("courtesy_light_delay_s", 20);             // courtesy lights off delay
        map.put("ambient_lighting_color", 0x01);           // ambient color preset
        map.put("led_signature_c_shape", 0x01);            // C-shaped LED DRL signature

        // Alarm
        map.put("alarm_type", 0x02);                       // 0=none, 1=basic, 2=Thatcham
        map.put("alarm_volumetric_sensor", 0x01);          // interior volumetric sensor
        map.put("alarm_perimetric", 0x01);                 // perimetric alarm
        map.put("alarm_tilt_sensor", 0x00);                // tilt/inclination sensor
        map.put("alarm_siren_duration_s", 30);             // siren duration

        // Wipers
        map.put("wiper_auto_rear_in_reverse", 0x01);       // rear wiper in reverse
        map.put("rain_sensor_sensitivity", 0x03);           // 1-5
        map.put("wiper_service_position", 0x01);            // wiper park at service pos

        // Plip / remote key
        map.put("plip_count", 0x02);                       // number of registered keys
        map.put("plip_boot_open", 0x01);                   // remote boot opening

        return Collections.unmodifiableMap(map);
    }

    @Override
    public int getSecurityLevel() {
        return 0x03;
    }

    @Override
    public List<Integer> getIdentificationDids() {
        return List.of(
                0xF187,  // Spare part number
                0xF188,  // Software version
                0xF189,  // Software version date
                0xF18A,  // System supplier ID
                0xF18C,  // ECU serial number
                0xF190,  // VIN
                0xF191,  // Hardware version
                0xF1A0,  // PSA traceability
                0xF1A2,  // PSA diagnostic ID
                0x2001,  // BSI configuration word 1
                0x2002,  // BSI configuration word 2
                0x2003,  // BSI configuration word 3
                0x2010,  // Locking configuration
                0x2011,  // Lighting configuration
                0x2012,  // Alarm configuration
                0x2020,  // Key count
                0x2021   // BSI options bitmap
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("LOCK_CONFIG", new byte[]{0x01, 0x0A, 0x01, 0x1E, 0x01, 0x00, 0x01});
        blocks.put("WINDOW_CONFIG", new byte[]{0x01, 0x01, 0x00, 0x00, 0x03});
        blocks.put("MIRROR_CONFIG", new byte[]{0x00, 0x00, 0x01, 0x01});
        blocks.put("LIGHT_CONFIG", new byte[]{0x01, 0x64, 0x01, 0x1E, 0x01, 0x01, 0x05, 0x14, 0x01, 0x01});
        blocks.put("ALARM_CONFIG", new byte[]{0x02, 0x01, 0x01, 0x00, 0x1E});
        blocks.put("WIPER_CONFIG", new byte[]{0x01, 0x03, 0x01});
        blocks.put("PLIP_CONFIG", new byte[]{0x02, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Central lock speed threshold");
        channels.put(0x02, "Relock timeout");
        channels.put(0x03, "Follow-me-home duration");
        channels.put(0x04, "Courtesy light delay");
        channels.put(0x05, "DRL intensity");
        channels.put(0x06, "Rain sensor sensitivity");
        channels.put(0x07, "Alarm siren duration");
        channels.put(0x10, "Window anti-pinch sensitivity");
        channels.put(0x11, "Interior dimming level");
        channels.put(0x12, "Ambient lighting color preset");
        channels.put(0x20, "Plip programming mode");
        channels.put(0x21, "Key learning counter");
        return channels;
    }
}
