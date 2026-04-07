package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * DME (Digital Motor Electronics) - BMW engine management ECU.
 * Controls fuel injection, ignition timing, boost pressure, VANOS,
 * and Valvetronic. CAN ID 0x7E0/0x7E8.
 */
public class BmwDme extends EcuDefinition {

    public BmwDme() {
        super(
                "DME - Digital Motor Electronics",
                BmwEcuMap.DME_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x11, 0x14, 0x19, 0x22, 0x23, 0x27, 0x2E, 0x31, 0x34, 0x36, 0x37, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("ISN_STATUS", 0x00);
        map.put("INJECTOR_BANK1_CYL1", 0x00);
        map.put("INJECTOR_BANK1_CYL2", 0x00);
        map.put("INJECTOR_BANK1_CYL3", 0x00);
        map.put("INJECTOR_BANK2_CYL4", 0x00);
        map.put("INJECTOR_BANK2_CYL5", 0x00);
        map.put("INJECTOR_BANK2_CYL6", 0x00);
        map.put("BOOST_PRESSURE_MAP", 0x01);
        map.put("VANOS_INTAKE_OFFSET", 0x00);
        map.put("VANOS_EXHAUST_OFFSET", 0x00);
        map.put("IDLE_SPEED_TARGET", 0x02BC);   // 700 RPM
        map.put("REV_LIMITER", 0x1A2C);          // 6700 RPM
        map.put("SPEED_LIMITER_ACTIVE", 0x01);
        map.put("SPEED_LIMITER_VALUE", 0x00FA);  // 250 km/h
        map.put("LAMBDA_CONTROL_BANK1", 0x01);
        map.put("LAMBDA_CONTROL_BANK2", 0x01);
        map.put("MISFIRE_DETECTION", 0x01);
        map.put("SECONDARY_AIR_PUMP", 0x01);
        map.put("EXHAUST_FLAP_CONTROL", 0x01);
        map.put("EGR_ACTIVE", 0x01);
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
                0xF19E, // Manufacturing date
                0x2001, // ISN (Individual Serial Number)
                0x2002, // Injector coding data
                0x2003, // Boost pressure map identifier
                0x2504  // VANOS adaptation values
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("ENGINE_VARIANT", new byte[]{0x00, 0x01});
        blocks.put("EMISSION_STANDARD", new byte[]{0x04});
        blocks.put("FUEL_TYPE", new byte[]{0x01});
        blocks.put("COUNTRY_VARIANT", new byte[]{0x00, 0x15});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Idle speed offset");
        channels.put(2, "Fuel trim bank 1 long-term");
        channels.put(3, "Fuel trim bank 2 long-term");
        channels.put(4, "Throttle adaptation");
        channels.put(5, "VANOS intake adaptation");
        channels.put(6, "VANOS exhaust adaptation");
        channels.put(7, "Knock sensor adaptation cyl 1-3");
        channels.put(8, "Knock sensor adaptation cyl 4-6");
        return channels;
    }
}
