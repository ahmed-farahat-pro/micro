package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * CIC/NBT (Car Information Computer / Next Big Thing) - BMW head unit ECU.
 * Controls region coding, video in motion unlock, voice control, navigation,
 * and Bluetooth pairing. CAN ID 0x6F1/0x6F9.
 */
public class BmwCic extends EcuDefinition {

    public BmwCic() {
        super(
                "CIC - Head Unit",
                BmwEcuMap.CIC_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("REGION_CODE", 0x01);              // Europe
        map.put("VIDEO_IN_MOTION", 0x00);           // Locked
        map.put("DVD_REGION", 0x02);                // Europe
        map.put("VOICE_CONTROL_ACTIVE", 0x01);
        map.put("VOICE_LANGUAGE", 0x01);            // English
        map.put("NAVIGATION_ACTIVE", 0x01);
        map.put("NAV_VOICE_GUIDANCE", 0x01);
        map.put("BLUETOOTH_ACTIVE", 0x01);
        map.put("BLUETOOTH_AUDIO_STREAMING", 0x01);
        map.put("USB_AUDIO_ACTIVE", 0x01);
        map.put("AUX_INPUT_ACTIVE", 0x01);
        map.put("REAR_VIEW_CAMERA", 0x00);
        map.put("PDC_GRAPHIC_DISPLAY", 0x01);
        map.put("SPLIT_SCREEN", 0x01);
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
                0x5000, // Head unit type (CIC/NBT/EVO)
                0x5001, // Navigation database version
                0x5002  // Bluetooth module version
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("REGION", new byte[]{0x01, 0x02});
        blocks.put("MEDIA", new byte[]{0x01, 0x01, 0x01, 0x01});
        blocks.put("VOICE", new byte[]{0x01, 0x01});
        blocks.put("CONNECTIVITY", new byte[]{0x01, 0x01, 0x00});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Display brightness offset");
        channels.put(2, "Audio balance front-rear");
        channels.put(3, "Audio balance left-right");
        channels.put(4, "Speed-dependent volume");
        return channels;
    }
}
