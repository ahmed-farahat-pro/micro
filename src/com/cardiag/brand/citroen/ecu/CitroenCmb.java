package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * CMB (Combined Instrument/Cluster) definition for Citroen vehicles.
 *
 * <p>The CMB manages the instrument cluster display including speedometer,
 * tachometer, trip computer, warning lamps, language settings, and
 * unit configuration. CAN address 0x766/0x666.</p>
 */
public class CitroenCmb extends EcuDefinition {

    private static final String ECU_NAME = "CMB (Combined Instrument Cluster)";
    private static final EcuAddress ADDRESS = new EcuAddress(0x766, 0x7DF, 0x666, "CMB");
    private static final String PROTOCOL = "UDS";

    public CitroenCmb() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("language", 0x03);                        // 0=FR, 1=DE, 2=ES, 3=EN, 4=IT, 5=PT, 6=NL
        map.put("speed_units", 0x00);                     // 0=km/h, 1=mph
        map.put("temperature_units", 0x00);               // 0=Celsius, 1=Fahrenheit
        map.put("fuel_consumption_units", 0x00);          // 0=L/100km, 1=mpg, 2=km/L
        map.put("distance_units", 0x00);                  // 0=km, 1=miles
        map.put("pressure_units", 0x00);                  // 0=bar, 1=psi
        map.put("tachometer_present", 0x01);              // tachometer display
        map.put("digital_speed_display", 0x01);           // digital speed overlay
        map.put("speed_alert_enabled", 0x00);             // speed warning
        map.put("speed_alert_threshold_kph", 130);        // speed alert threshold
        map.put("trip_computer_pages", 0x03);             // number of trip pages
        map.put("fuel_gauge_segments", 0x08);             // fuel gauge resolution
        map.put("cluster_brightness_auto", 0x01);         // auto brightness
        map.put("cluster_brightness_day", 0x0A);          // day brightness 0-15
        map.put("cluster_brightness_night", 0x05);        // night brightness 0-15
        map.put("service_indicator_type", 0x02);          // 0=none, 1=km, 2=flexible
        map.put("gear_indicator", 0x01);                  // gear shift indicator
        map.put("oil_temp_display", 0x00);                // oil temperature on cluster
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
                0x4001,  // Display configuration
                0x4002,  // Language setting
                0x4003,  // Units configuration
                0x4004,  // Total odometer
                0x4005,  // Trip A distance
                0x4006,  // Trip B distance
                0x4010   // Service interval remaining
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("DISPLAY_CONFIG", new byte[]{0x03, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01});
        blocks.put("UNITS_CONFIG", new byte[]{0x00, 0x00, 0x00, 0x00});
        blocks.put("SPEED_ALERT_CONFIG", new byte[]{0x00, (byte) 0x82});
        blocks.put("BRIGHTNESS_CONFIG", new byte[]{0x01, 0x0A, 0x05});
        blocks.put("SERVICE_CONFIG", new byte[]{0x02, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Language selection");
        channels.put(0x02, "Speed units");
        channels.put(0x03, "Temperature units");
        channels.put(0x04, "Consumption units");
        channels.put(0x05, "Speed alert threshold");
        channels.put(0x06, "Day brightness level");
        channels.put(0x07, "Night brightness level");
        channels.put(0x10, "Service interval distance");
        channels.put(0x11, "Service interval time");
        return channels;
    }
}
