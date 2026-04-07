package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * KOMBI (Instrument Cluster) - BMW dashboard/instrument cluster ECU.
 * Controls display language, units, oil service resets, inspection resets,
 * needle sweep animation, and speed warning configuration.
 * CAN ID 0x720/0x728.
 */
public class BmwKombi extends EcuDefinition {

    public BmwKombi() {
        super(
                "KOMBI - Instrument Cluster",
                BmwEcuMap.KOMBI_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("LANGUAGE", 0x01);                    // English
        map.put("UNITS_SPEED", 0x00);                 // km/h
        map.put("UNITS_TEMPERATURE", 0x00);           // Celsius
        map.put("UNITS_FUEL_CONSUMPTION", 0x00);      // L/100km
        map.put("NEEDLE_SWEEP_ON_START", 0x01);       // Enabled
        map.put("DIGITAL_SPEEDO", 0x01);              // Enabled
        map.put("SPEED_WARNING_ACTIVE", 0x00);
        map.put("SPEED_WARNING_THRESHOLD", 0x0078);   // 120 km/h
        map.put("OIL_SERVICE_DISTANCE", 0x7530);      // 30000 km
        map.put("OIL_SERVICE_TIME", 0x0168);           // 360 days
        map.put("INSPECTION_DISTANCE", 0x7530);        // 30000 km
        map.put("INSPECTION_TIME", 0x02D0);            // 720 days
        map.put("DATE_FORMAT", 0x01);                  // DD.MM.YYYY
        map.put("TIME_FORMAT", 0x01);                  // 24h
        map.put("FUEL_GAUGE_TYPE", 0x01);              // Standard
        map.put("ECO_PRO_DISPLAY", 0x01);              // Enabled
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
                0x4000, // Service data
                0x4001, // Oil service counter
                0x4002, // Inspection counter
                0x4003  // Total mileage
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("DISPLAY_CONFIG", new byte[]{0x01, 0x00, 0x00, 0x00, 0x01, 0x01});
        blocks.put("SERVICE_CONFIG", new byte[]{0x75, 0x30, 0x01, 0x68, 0x75, 0x30, 0x02, (byte) 0xD0});
        blocks.put("WARNING_CONFIG", new byte[]{0x00, 0x00, 0x78});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Cluster brightness offset");
        channels.put(2, "Oil service distance reset");
        channels.put(3, "Oil service time reset");
        channels.put(4, "Inspection distance reset");
        channels.put(5, "Inspection time reset");
        channels.put(6, "Total mileage correction");
        return channels;
    }
}
