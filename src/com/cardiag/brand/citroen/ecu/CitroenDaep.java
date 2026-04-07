package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * DAEP (Directional Adaptive Headlights) ECU definition for Citroen vehicles.
 *
 * <p>Controls the adaptive front lighting system including directional headlamps,
 * automatic high beam, dynamic cornering lights, and the Citroen C-shaped
 * LED signature lighting. CAN address 0x770/0x670.</p>
 */
public class CitroenDaep extends EcuDefinition {

    private static final String ECU_NAME = "DAEP (Directional Adaptive Headlights)";
    private static final EcuAddress ADDRESS = new EcuAddress(0x770, 0x7DF, 0x670, "DAEP");
    private static final String PROTOCOL = "UDS";

    public CitroenDaep() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("headlight_type", 0x02);                  // 0=halogen, 1=xenon, 2=LED, 3=matrix LED
        map.put("adaptive_directional", 0x01);            // directional headlights
        map.put("auto_high_beam", 0x01);                  // automatic high beam
        map.put("auto_leveling", 0x01);                   // automatic headlight leveling
        map.put("dynamic_cornering", 0x01);               // dynamic cornering light
        map.put("static_cornering_via_fog", 0x01);        // static cornering via fog light
        map.put("welcome_lighting_enabled", 0x01);        // welcome light sequence
        map.put("welcome_light_duration_s", 10);          // welcome light duration
        map.put("c_shape_led_drl", 0x01);                 // C-shaped DRL signature
        map.put("led_drl_intensity", 0x64);               // DRL intensity (0-100%)
        map.put("follow_me_home_enabled", 0x01);          // follow-me-home headlights
        map.put("follow_me_home_duration_s", 30);         // follow-me-home duration
        map.put("headlight_washer_present", 0x01);        // headlight washers
        map.put("traffic_side", 0x00);                    // 0=RHT (right-hand traffic), 1=LHT
        map.put("tourist_mode_available", 0x01);          // tourist beam pattern switch
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int getSecurityLevel() {
        return 0x01;
    }

    @Override
    public List<Integer> getIdentificationDids() {
        return List.of(
                0xF187,  // Spare part number
                0xF188,  // Software version
                0xF18C,  // ECU serial number
                0xF190,  // VIN
                0xF191,  // Hardware version
                0xF1A0,  // PSA traceability
                0xA001,  // Headlight configuration
                0xA002,  // Left headlight status
                0xA003,  // Right headlight status
                0xA004,  // Leveling motor position
                0xA005,  // Ambient light sensor value
                0xA010   // Adaptive beam pattern status
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("HEADLIGHT_CONFIG", new byte[]{0x02, 0x01, 0x01, 0x01, 0x01});
        blocks.put("CORNERING_CONFIG", new byte[]{0x01, 0x01});
        blocks.put("WELCOME_CONFIG", new byte[]{0x01, 0x0A, 0x01, 0x64});
        blocks.put("FOLLOW_HOME_CONFIG", new byte[]{0x01, 0x1E});
        blocks.put("TRAFFIC_CONFIG", new byte[]{0x00, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Headlight leveling calibration");
        channels.put(0x02, "DRL intensity");
        channels.put(0x03, "Welcome light duration");
        channels.put(0x04, "Follow-me-home duration");
        channels.put(0x05, "Auto high beam sensitivity");
        channels.put(0x06, "Cornering light activation angle");
        return channels;
    }
}
