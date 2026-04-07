package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * Electric Power Steering (EPS) ECU definition for Citroen vehicles.
 *
 * <p>Manages the column-mounted or rack-mounted electric power steering assist,
 * including speed-sensitive assist curves, steering angle sensor calibration,
 * and variable assist modes. CAN address 0x762/0x662.</p>
 */
public class CitroenEps extends EcuDefinition {

    private static final String ECU_NAME = "Electric Power Steering";
    private static final EcuAddress ADDRESS = new EcuAddress(0x762, 0x7DF, 0x662, "EPS");
    private static final String PROTOCOL = "UDS";

    public CitroenEps() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("eps_type", 0x01);                        // 0=column, 1=rack-mounted
        map.put("assist_mode_comfort", 0x01);             // comfort mode available
        map.put("assist_mode_normal", 0x01);              // normal mode
        map.put("assist_mode_sport", 0x01);               // sport mode
        map.put("speed_sensitive_assist", 0x01);          // speed-dependent assist
        map.put("assist_at_park_speed", 0x64);            // assist level at park (0-100%)
        map.put("assist_at_highway_speed", 0x28);         // assist level at highway (0-100%)
        map.put("steering_angle_sensor", 0x01);           // SAS present
        map.put("active_return", 0x01);                   // active return to center
        map.put("lane_keep_assist_torque", 0x01);         // LKA torque overlay support
        map.put("pull_drift_compensation", 0x01);         // drift compensation
        map.put("variable_ratio_steering", 0x00);         // variable ratio (if equipped)
        return Collections.unmodifiableMap(map);
    }

    @Override
    public int getSecurityLevel() {
        return 0x03;
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
                0x8001,  // EPS configuration
                0x8002,  // Steering angle
                0x8003,  // Motor current
                0x8004,  // Assist torque
                0x8005,  // Motor temperature
                0x8010   // Assist curve parameters
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("EPS_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x01, 0x01});
        blocks.put("ASSIST_CURVE", new byte[]{0x64, 0x50, 0x3C, 0x28});
        blocks.put("COMPENSATION_CONFIG", new byte[]{0x01, 0x01, 0x00});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Steering angle sensor calibration");
        channels.put(0x02, "Assist level at park speed");
        channels.put(0x03, "Assist level at highway speed");
        channels.put(0x04, "Active return strength");
        channels.put(0x05, "Pull drift compensation offset");
        return channels;
    }
}
