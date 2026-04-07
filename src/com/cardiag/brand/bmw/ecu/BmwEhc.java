package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * EHC (Electronic Height Control) - BMW air suspension management ECU.
 * Controls ride height, air spring pressure, suspension modes, and
 * leveling for vehicles equipped with air or adaptive M suspension.
 * CAN ID 0x6C4/0x6CC.
 */
public class BmwEhc extends EcuDefinition {

    public BmwEhc() {
        super(
                "EHC - Electronic Height Control",
                BmwEcuMap.EHC_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("SUSPENSION_TYPE", 0x01);              // Air suspension
        map.put("RIDE_HEIGHT_NORMAL_FRONT", 0x00);     // Factory default
        map.put("RIDE_HEIGHT_NORMAL_REAR", 0x00);
        map.put("RIDE_HEIGHT_LOWERED", 0xF6);          // -10mm
        map.put("RIDE_HEIGHT_RAISED", 0x14);            // +20mm
        map.put("AUTO_LEVELING_ACTIVE", 0x01);
        map.put("SPEED_DEPENDENT_LOWERING", 0x01);
        map.put("LOWERING_SPEED_THRESHOLD", 0x78);     // 120 km/h
        map.put("ADAPTIVE_DAMPING", 0x01);
        map.put("SPORT_MODE_FIRMNESS", 0x02);           // Medium-firm
        map.put("COMFORT_MODE_SOFTNESS", 0x01);         // Soft
        map.put("LOAD_COMPENSATION_ACTIVE", 0x01);
        map.put("JACK_MODE_ACTIVE", 0x00);
        map.put("TRANSPORT_MODE_ACTIVE", 0x00);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int getSecurityLevel() {
        return 0x03;
    }

    @Override
    public List<Integer> getIdentificationDids() {
        return Arrays.asList(
                0xF190, // VIN
                0xF191, // ECU hardware number
                0xF187, // Part number
                0xF189, // Software version
                0x9000, // Ride height current values
                0x9001, // Air spring pressure readings
                0x9002, // Damper valve positions
                0x9003  // Level sensor readings
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("HEIGHT_CONFIG", new byte[]{0x01, 0x00, 0x00, (byte) 0xF6, 0x14});
        blocks.put("DAMPING_CONFIG", new byte[]{0x01, 0x02, 0x01});
        blocks.put("LEVELING", new byte[]{0x01, 0x01, 0x78});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Front left height sensor offset");
        channels.put(2, "Front right height sensor offset");
        channels.put(3, "Rear left height sensor offset");
        channels.put(4, "Rear right height sensor offset");
        channels.put(5, "Compressor run-time calibration");
        return channels;
    }
}
