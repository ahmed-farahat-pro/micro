package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * FRM (Footwell Module) - BMW body electronics module.
 * Controls lighting, windows, mirrors, wipers, and central locking.
 * The most commonly coded BMW ECU with an extensive coding map.
 * CAN ID 0x6C0/0x6C8.
 */
public class BmwFrm extends EcuDefinition {

    public BmwFrm() {
        super(
                "FRM - Footwell Module",
                BmwEcuMap.FRM_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        // Lighting coding
        map.put("DRL_ACTIVE", 0x01);
        map.put("DRL_BRIGHTNESS", 0x64);           // 100%
        map.put("ANGEL_EYES_ACTIVE", 0x01);
        map.put("ANGEL_EYES_AS_DRL", 0x00);
        map.put("WELCOME_LIGHTS", 0x01);
        map.put("WELCOME_LIGHT_DURATION", 0x0A);   // 10 seconds
        map.put("CORNERING_LIGHTS", 0x01);
        map.put("FOG_AS_CORNERING", 0x00);
        map.put("ADAPTIVE_HEADLIGHTS", 0x01);
        map.put("LED_TAIL_CODING", 0x01);
        map.put("BRAKE_FORCE_DISPLAY", 0x00);
        map.put("AMBIENT_LIGHTING_ACTIVE", 0x01);
        map.put("AMBIENT_BRIGHTNESS", 0x50);        // 80%
        // Window coding
        map.put("ONE_TOUCH_WINDOW_UP", 0x01);
        map.put("ONE_TOUCH_WINDOW_DOWN", 0x01);
        map.put("COMFORT_CLOSE_VIA_REMOTE", 0x01);
        map.put("WINDOW_ANTI_TRAP_FORCE", 0x03);
        // Mirror coding
        map.put("AUTO_FOLD_MIRRORS", 0x00);
        map.put("AUTO_DIM_MIRRORS", 0x01);
        map.put("MIRROR_TILT_ON_REVERSE", 0x01);
        // Wiper coding
        map.put("RAIN_SENSOR_ACTIVE", 0x01);
        map.put("RAIN_SENSOR_SENSITIVITY", 0x03);
        // Central locking
        map.put("SPEED_LOCK", 0x01);
        map.put("SELECTIVE_UNLOCK", 0x01);
        map.put("RELOCK_AFTER_TIMEOUT", 0x01);
        map.put("LOCK_CONFIRMATION_HORN", 0x01);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int getSecurityLevel() {
        return 0x01;
    }

    @Override
    public List<Integer> getIdentificationDids() {
        return Arrays.asList(
                0xF190, // VIN
                0xF191, // ECU hardware number
                0xF187, // Part number
                0xF189, // Software version
                0x3000, // FRM coding index
                0x3001, // Lighting configuration
                0x3002, // Window configuration
                0x3003  // Central locking configuration
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("LIGHTING", new byte[]{0x01, 0x64, 0x01, 0x00, 0x01, 0x0A, 0x01, 0x00, 0x01, 0x01});
        blocks.put("WINDOWS", new byte[]{0x01, 0x01, 0x01, 0x03});
        blocks.put("MIRRORS", new byte[]{0x00, 0x01, 0x01});
        blocks.put("WIPERS", new byte[]{0x01, 0x03});
        blocks.put("CENTRAL_LOCKING", new byte[]{0x01, 0x01, 0x01, 0x01});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "DRL brightness level");
        channels.put(2, "Welcome light duration");
        channels.put(3, "Interior ambient brightness");
        channels.put(4, "Rain sensor sensitivity base");
        channels.put(5, "Anti-trap force threshold");
        channels.put(6, "Mirror fold angle");
        channels.put(7, "Cornering light activation speed");
        channels.put(8, "Auto-relock timeout");
        return channels;
    }
}
