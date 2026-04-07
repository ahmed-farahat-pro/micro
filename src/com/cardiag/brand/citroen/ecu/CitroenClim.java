package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * Climate control ECU definition for Citroen vehicles.
 *
 * <p>Manages the HVAC system including dual-zone automatic climate control,
 * A/C compressor engagement, residual heating, auto-defog, heated seats,
 * and cabin air quality sensors. CAN address 0x768/0x668.</p>
 */
public class CitroenClim extends EcuDefinition {

    private static final String ECU_NAME = "Climate Control";
    private static final EcuAddress ADDRESS = new EcuAddress(0x768, 0x7DF, 0x668, "CLIM");
    private static final String PROTOCOL = "UDS";

    public CitroenClim() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("climate_type", 0x02);                    // 0=manual, 1=single-zone auto, 2=dual-zone auto
        map.put("ac_compressor_type", 0x01);              // 0=fixed, 1=variable displacement
        map.put("residual_heat_enabled", 0x01);           // rest heat after engine off
        map.put("residual_heat_duration_min", 15);        // rest heat max duration
        map.put("auto_defog_enabled", 0x01);              // automatic windscreen defog
        map.put("auto_defog_sensitivity", 0x02);          // 1=low, 2=medium, 3=high
        map.put("heated_seats_present", 0x01);            // heated front seats
        map.put("heated_seats_levels", 0x03);             // number of heat levels
        map.put("ventilated_seats_present", 0x00);        // ventilated seats
        map.put("heated_windscreen", 0x00);               // heated windscreen
        map.put("air_quality_sensor", 0x01);              // AQS auto recirc
        map.put("pollen_filter_type", 0x01);              // 0=standard, 1=activated carbon
        map.put("rear_ac_enabled", 0x00);                 // rear AC controls
        map.put("max_blower_speed", 0x07);                // max fan speed level
        map.put("heat_pump_present", 0x00);               // heat pump (EV models)
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
                0x6001,  // Climate configuration
                0x6002,  // Compressor status
                0x6003,  // Cabin temperature
                0x6004,  // Evaporator temperature
                0x6005,  // Ambient temperature
                0x6006   // Humidity level
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("CLIMATE_CONFIG", new byte[]{0x02, 0x01, 0x01, 0x0F, 0x01, 0x02});
        blocks.put("SEAT_HEAT_CONFIG", new byte[]{0x01, 0x03, 0x00});
        blocks.put("DEFOG_CONFIG", new byte[]{0x01, 0x02, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Defog sensitivity");
        channels.put(0x02, "Rest heat duration");
        channels.put(0x03, "Max blower speed limit");
        channels.put(0x04, "AQS sensitivity threshold");
        return channels;
    }
}
