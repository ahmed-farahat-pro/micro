package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * SMEG/NAC infotainment ECU definition for Citroen vehicles.
 *
 * <p>Covers the SMEG+ (Systeme Multimedia Embarque de Generacion) and NAC
 * (Network Audio Controller) head units used in Citroen vehicles.
 * CAN address 0x767/0x667.</p>
 *
 * <p>Manages region settings, Bluetooth configuration, navigation activation,
 * Apple CarPlay / Android Auto, and audio presets.</p>
 */
public class CitroenSmeg extends EcuDefinition {

    private static final String ECU_NAME = "SMEG/NAC Infotainment";
    private static final EcuAddress ADDRESS = new EcuAddress(0x767, 0x7DF, 0x667, "SMEG");
    private static final String PROTOCOL = "UDS";

    public CitroenSmeg() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("region_code", 0x01);                   // 0=Europe, 1=UK, 2=Middle East, 3=China
        map.put("navigation_enabled", 0x01);            // navigation system activation
        map.put("navigation_map_update_auto", 0x00);    // auto map updates
        map.put("bluetooth_enabled", 0x01);             // Bluetooth module
        map.put("bluetooth_streaming", 0x01);            // A2DP audio streaming
        map.put("bluetooth_phonebook_sync", 0x01);       // automatic phonebook sync
        map.put("apple_carplay", 0x01);                  // Apple CarPlay support
        map.put("android_auto", 0x01);                   // Android Auto support
        map.put("mirrorlink", 0x00);                    // MirrorLink support
        map.put("dab_radio", 0x01);                     // DAB/DAB+ digital radio
        map.put("hd_radio", 0x00);                      // HD Radio (North America)
        map.put("rear_camera_display", 0x01);            // reversing camera on screen
        map.put("rear_camera_guidelines", 0x01);         // dynamic guidelines
        map.put("parking_sensor_display", 0x01);         // parking sensor on screen
        map.put("audio_preset_count", 0x06);             // number of audio presets
        map.put("volume_speed_compensation", 0x01);      // speed-dependent volume
        map.put("screen_size", 0x0A);                    // screen size in inches
        map.put("touchscreen_enabled", 0x01);            // touchscreen input
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
                0x5001,  // Region configuration
                0x5002,  // Navigation status
                0x5003,  // Bluetooth status
                0x5004,  // Connected services status
                0x5005,  // Audio configuration
                0x5010   // Media source list
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("REGION_CONFIG", new byte[]{0x01, 0x01, 0x00});
        blocks.put("CONNECTIVITY_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x01, 0x01, 0x00});
        blocks.put("AUDIO_CONFIG", new byte[]{0x01, 0x06, 0x01});
        blocks.put("CAMERA_CONFIG", new byte[]{0x01, 0x01, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Region code");
        channels.put(0x02, "Navigation activation");
        channels.put(0x03, "CarPlay/Android Auto toggle");
        channels.put(0x04, "DAB radio activation");
        channels.put(0x05, "Volume speed compensation level");
        channels.put(0x06, "Rear camera guidelines type");
        return channels;
    }
}
