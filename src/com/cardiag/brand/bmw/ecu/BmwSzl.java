package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * SZL (Schaltzentrum Lenksaule) - BMW steering column switch cluster ECU.
 * Controls cruise control, turn signals, wiper stalk, and high-beam assistant.
 * CAN ID 0x6C5/0x6CD.
 */
public class BmwSzl extends EcuDefinition {

    public BmwSzl() {
        super(
                "SZL - Steering Column Switch",
                BmwEcuMap.SZL_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("CRUISE_CONTROL_ACTIVE", 0x01);
        map.put("CRUISE_CONTROL_TYPE", 0x01);        // Standard (not ACC)
        map.put("TURN_SIGNAL_ONE_TOUCH_COUNT", 0x03); // 3 flashes
        map.put("TURN_SIGNAL_COMFORT_ACTIVE", 0x01);
        map.put("HIGH_BEAM_ASSISTANT", 0x00);
        map.put("WIPER_INTERMITTENT_STAGES", 0x04);
        map.put("WIPER_AUTO_ON_RAIN", 0x01);
        map.put("PADDLE_SHIFT_ACTIVE", 0x00);
        map.put("SPORT_BUTTON_ACTIVE", 0x00);
        map.put("MULTI_FUNCTION_WHEEL_TYPE", 0x01);  // Standard
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
                0xA000, // Switch configuration
                0xA001  // Stalk function assignments
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("CRUISE_CONFIG", new byte[]{0x01, 0x01});
        blocks.put("TURN_SIGNAL_CONFIG", new byte[]{0x03, 0x01});
        blocks.put("WIPER_CONFIG", new byte[]{0x04, 0x01});
        blocks.put("MFL_CONFIG", new byte[]{0x01, 0x00, 0x00});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Steering angle sensor zero");
        channels.put(2, "Turn signal relay timing");
        channels.put(3, "Cruise control resume behavior");
        return channels;
    }
}
