package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * EGS (Elektronisches Getriebe Steuerung) - BMW automatic transmission ECU.
 * Controls shift points, torque converter lockup, and adaptation learning.
 * CAN ID 0x6F1/0x6F9.
 */
public class BmwEgs extends EcuDefinition {

    public BmwEgs() {
        super(
                "EGS - Automatic Transmission",
                BmwEcuMap.EGS_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("TRANSMISSION_TYPE", 0x01);         // 8HP automatic
        map.put("SPORT_MODE_SHIFT_POINT", 0x02);    // Medium aggression
        map.put("COMFORT_MODE_SHIFT_POINT", 0x01);  // Early upshift
        map.put("LAUNCH_CONTROL", 0x00);             // Disabled
        map.put("TORQUE_CONVERTER_LOCKUP_TEMP", 0x50); // 80 degrees C
        map.put("SHIFT_PADDLE_TIMEOUT", 0x0A);      // 10 seconds
        map.put("DOWNSHIFT_REV_MATCH", 0x01);        // Enabled
        map.put("ADAPTIVE_SHIFT", 0x01);              // Enabled
        map.put("GEAR_DISPLAY", 0x01);                // Show current gear
        map.put("MAX_GEAR_LIMIT", 0x08);              // 8th gear
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
                0x2100, // Shift adaptation values
                0x2101, // Torque converter data
                0x2102  // Transmission temperature
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("TRANS_VARIANT", new byte[]{0x08, 0x45});
        blocks.put("DRIVE_MODE_CONFIG", new byte[]{0x03});
        blocks.put("SHIFT_STRATEGY", new byte[]{0x01, 0x02});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Clutch pack A fill pressure");
        channels.put(2, "Clutch pack B fill pressure");
        channels.put(3, "Torque converter slip target");
        channels.put(4, "Shift time 1-2");
        channels.put(5, "Shift time 2-3");
        channels.put(6, "Shift time 3-4");
        channels.put(7, "Shift time 4-5");
        channels.put(8, "Shift time 5-6");
        return channels;
    }
}
