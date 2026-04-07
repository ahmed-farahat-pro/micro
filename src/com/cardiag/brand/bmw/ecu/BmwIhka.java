package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * IHKA (Integrierte Heizung und Klima Automatik) - BMW climate control ECU.
 * Controls automatic climate, A/C compressor, seat heating stages,
 * and auxiliary heater. CAN ID 0x6C1/0x6C9.
 */
public class BmwIhka extends EcuDefinition {

    public BmwIhka() {
        super(
                "IHKA - Climate Control",
                BmwEcuMap.IHKA_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("AUTO_CLIMATE_ACTIVE", 0x01);
        map.put("DUAL_ZONE", 0x01);
        map.put("AC_COMPRESSOR_AUTO_OFF_WITH_ENGINE_STOP", 0x01);
        map.put("RESIDUAL_HEAT_ACTIVE", 0x01);
        map.put("RESIDUAL_HEAT_DURATION", 0x0F);    // 15 minutes
        map.put("SEAT_HEATING_STAGES", 0x03);        // 3 stages
        map.put("SEAT_HEATING_AUTO_OFF", 0x01);
        map.put("STEERING_WHEEL_HEATING", 0x00);
        map.put("MAX_AC_MODE", 0x01);
        map.put("RECIRCULATION_AUTO", 0x01);
        map.put("AUX_VENTILATION", 0x00);
        map.put("SOLAR_SENSOR_ACTIVE", 0x01);
        map.put("FOOTWELL_HEATING_BIAS", 0x32);      // 50%
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
                0x6000, // Climate zone configuration
                0x6001, // Compressor status
                0x6002  // Temperature sensor readings
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("CLIMATE_MAIN", new byte[]{0x01, 0x01, 0x01, 0x01, 0x0F});
        blocks.put("SEAT_HEATING", new byte[]{0x03, 0x01});
        blocks.put("AUX_SYSTEMS", new byte[]{0x00, 0x01, 0x00, 0x01});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Interior temperature sensor offset");
        channels.put(2, "Exterior temperature sensor offset");
        channels.put(3, "Evaporator temperature target");
        channels.put(4, "Blower motor calibration");
        return channels;
    }
}
