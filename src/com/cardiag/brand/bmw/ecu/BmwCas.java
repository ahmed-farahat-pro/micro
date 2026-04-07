package com.cardiag.brand.bmw.ecu;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * CAS (Car Access System) - BMW key management and immobilizer ECU.
 * Controls key programming, remote functions, EWS alignment, and engine
 * start authorization. Requires ENGINEERING security level for key programming.
 * CAN ID 0x640/0x648.
 */
public class BmwCas extends EcuDefinition {

    public BmwCas() {
        super(
                "CAS - Car Access System",
                BmwEcuMap.CAS_ADDRESS,
                "UDS",
                Arrays.asList(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                initCodingBlocks(),
                initAdaptationChannels()
        );
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("KEY_COUNT", 0x02);
        map.put("KEY_MEMORY_SLOT1", 0x01);
        map.put("KEY_MEMORY_SLOT2", 0x01);
        map.put("KEY_MEMORY_SLOT3", 0x00);
        map.put("KEY_MEMORY_SLOT4", 0x00);
        map.put("REMOTE_LOCK_FEEDBACK", 0x02);    // Horn + lights
        map.put("REMOTE_UNLOCK_MODE", 0x01);       // Driver door only
        map.put("COMFORT_ACCESS", 0x01);            // Enabled
        map.put("KEYLESS_GO", 0x01);                // Enabled
        map.put("PANIC_ALARM_ENABLED", 0x01);
        map.put("AUTO_RELOCK_TIME", 0x1E);         // 30 seconds
        map.put("EWS_ALIGNMENT_STATUS", 0x01);     // Aligned
        map.put("STARTER_LOCKOUT", 0x01);
        map.put("TILT_ALARM", 0x01);
        map.put("INTERIOR_MOTION_SENSOR", 0x01);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int getSecurityLevel() {
        return 0x61; // ENGINEERING level required for key programming
    }

    @Override
    public List<Integer> getIdentificationDids() {
        return Arrays.asList(
                0xF190, // VIN
                0xF191, // ECU hardware number
                0xF187, // Part number
                0xF189, // Software version
                0xD000, // Key status register
                0xD001, // Key slot 1 ID
                0xD002, // Key slot 2 ID
                0xD003, // Key slot 3 ID
                0xD004, // Key slot 4 ID
                0xD010  // EWS ISN synchronization status
        );
    }

    private static Map<String, byte[]> initCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("KEY_MANAGEMENT", new byte[]{0x02, 0x01, 0x01, 0x00, 0x00});
        blocks.put("REMOTE_CONFIG", new byte[]{0x02, 0x01});
        blocks.put("ALARM_CONFIG", new byte[]{0x01, 0x01, 0x01});
        blocks.put("EWS_DATA", new byte[]{0x01, 0x00, 0x00, 0x00});
        return blocks;
    }

    private static Map<Integer, String> initAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(1, "Key programming mode");
        channels.put(2, "EWS-DME ISN alignment");
        channels.put(3, "Remote frequency calibration");
        channels.put(4, "Comfort access range");
        channels.put(5, "Alarm sensitivity");
        return channels;
    }
}
