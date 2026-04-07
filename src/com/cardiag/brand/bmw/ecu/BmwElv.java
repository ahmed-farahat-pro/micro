package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * ELV (Elektronische Lenksauleverriegelung) - BMW electronic steering lock
 * and power steering ECU. Controls steering column lock, EPS assist curves,
 * active steering, and servotronic configuration.
 * CAN ID 0x6C3/0x6CB.
 */
public class BmwElv extends EcuDefinition {

    public BmwElv() {
        super(
                "ELV - Electronic Steering Lock",
                BmwEcuMap.ELV_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("STEERING_LOCK_ACTIVE", 0x01);
        map.put("EPS_VARIANT", 0x01);                 // Standard EPS
        map.put("SERVOTRONIC_ACTIVE", 0x01);
        map.put("SERVOTRONIC_SPORT_MODE", 0x00);
        map.put("ACTIVE_STEERING_ACTIVE", 0x00);
        map.put("ASSIST_CURVE_COMFORT", 0x01);
        map.put("ASSIST_CURVE_SPORT", 0x02);
        map.put("VARIABLE_RATIO_STEERING", 0x00);
        map.put("SPEED_DEPENDENT_ASSIST", 0x01);
        map.put("STEERING_WHEEL_VIBRATION", 0x01);    // Lane departure feedback
        map.put("PULL_COMPENSATION", 0x01);
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
                0x8000, // Steering lock status
                0x8001, // EPS configuration
                0x8002  // Assist curve parameters
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("LOCK_CONFIG", new byte[]{0x01});
        blocks.put("EPS_CONFIG", new byte[]{0x01, 0x01, 0x00, 0x01, 0x02});
        blocks.put("ACTIVE_STEERING", new byte[]{0x00, 0x00});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Steering center position");
        channels.put(2, "Assist force low speed");
        channels.put(3, "Assist force high speed");
        channels.put(4, "Return-to-center force");
        return channels;
    }
}
