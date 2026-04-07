package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * KAFAS (Kamera-basiertes Fahrerassistenzsystem) - BMW camera-based ADAS ECU.
 * Controls lane departure warning, forward collision warning, pedestrian
 * detection, high-beam assistant, traffic sign recognition, and camera
 * calibration routines. CAN ID 0x6D0/0x6D8.
 */
public class BmwKafas extends EcuDefinition {

    public BmwKafas() {
        super(
                "KAFAS - Camera ADAS Module",
                BmwEcuMap.KAFAS_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("LDW_ACTIVE", 0x01);                 // Lane Departure Warning
        map.put("LDW_SENSITIVITY", 0x02);             // Medium
        map.put("LDW_WARNING_TYPE", 0x01);            // Steering vibration
        map.put("FCW_ACTIVE", 0x01);                  // Forward Collision Warning
        map.put("FCW_SENSITIVITY", 0x02);             // Medium
        map.put("CITY_BRAKING_ACTIVE", 0x01);
        map.put("PEDESTRIAN_WARNING", 0x01);
        map.put("TRAFFIC_SIGN_RECOGNITION", 0x01);
        map.put("SPEED_LIMIT_INFO", 0x01);
        map.put("HIGH_BEAM_ASSISTANT", 0x01);
        map.put("CAMERA_CALIBRATION_STATUS", 0x01);   // Calibrated
        map.put("HEAD_UP_DISPLAY_ACTIVE", 0x00);
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
                0xB000, // Camera calibration data
                0xB001, // ADAS function status
                0xB002, // Camera lens parameters
                0xB003  // Horizon detection reference
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("LDW_CONFIG", new byte[]{0x01, 0x02, 0x01});
        blocks.put("FCW_CONFIG", new byte[]{0x01, 0x02, 0x01, 0x01});
        blocks.put("TSR_CONFIG", new byte[]{0x01, 0x01});
        blocks.put("CAMERA_CONFIG", new byte[]{0x01, 0x00});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Camera vertical alignment");
        channels.put(2, "Camera horizontal alignment");
        channels.put(3, "Lane detection threshold");
        channels.put(4, "Object detection range");
        channels.put(5, "Calibration target distance");
        return channels;
    }
}
