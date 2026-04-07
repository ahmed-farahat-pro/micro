package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * DSC (Dynamic Stability Control) - BMW stability and brake control ECU.
 * Controls ABS, ESP, traction control, DTC mode, brake bleed routines,
 * and CBS brake pad reset. CAN ID 0x6C2/0x6CA.
 */
public class BmwDsc extends EcuDefinition {

    public BmwDsc() {
        super(
                "DSC - Dynamic Stability Control",
                BmwEcuMap.DSC_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("DSC_ACTIVE", 0x01);
        map.put("DTC_MODE_AVAILABLE", 0x01);
        map.put("ABS_ACTIVE", 0x01);
        map.put("CBC_ACTIVE", 0x01);              // Cornering Brake Control
        map.put("DBC_ACTIVE", 0x01);              // Dynamic Brake Control
        map.put("HILL_START_ASSIST", 0x01);
        map.put("BRAKE_DRYING", 0x01);
        map.put("BRAKE_STANDBY", 0x01);
        map.put("FADING_COMPENSATION", 0x01);
        map.put("EPB_ACTIVE", 0x01);               // Electronic Parking Brake
        map.put("EPB_AUTO_HOLD", 0x01);
        map.put("BRAKE_PAD_WEAR_FRONT", 0x00);     // CBS counter
        map.put("BRAKE_PAD_WEAR_REAR", 0x00);
        map.put("PERFORMANCE_CONTROL", 0x00);       // M-specific
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
                0x7000, // DSC configuration
                0x7001, // ABS sensor data
                0x7002, // Brake pad wear counters
                0x7003  // Hydraulic unit data
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("DSC_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x01, 0x01});
        blocks.put("BRAKE_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x01});
        blocks.put("EPB_CONFIG", new byte[]{0x01, 0x01});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "ABS wheel speed sensor FL offset");
        channels.put(2, "ABS wheel speed sensor FR offset");
        channels.put(3, "ABS wheel speed sensor RL offset");
        channels.put(4, "ABS wheel speed sensor RR offset");
        channels.put(5, "Brake pressure sensor calibration");
        channels.put(6, "Steering angle sensor center");
        return channels;
    }
}
