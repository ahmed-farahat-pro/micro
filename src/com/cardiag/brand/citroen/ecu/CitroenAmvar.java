package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * AMVAR (Amortisseurs a Variation) suspension ECU definition for Citroen vehicles.
 *
 * <p>Controls the Citroen Advanced Comfort suspension with Progressive Hydraulic
 * Cushions (PHC). Available on C4 Cactus, C5 Aircross, C5 X, and other models
 * with the adaptive damping option. CAN address 0x769/0x669.</p>
 *
 * <p>On C5 Aircross, also manages ride height adjustment between
 * normal, raised, and lowered positions.</p>
 */
public class CitroenAmvar extends EcuDefinition {

    private static final String ECU_NAME = "AMVAR Suspension";
    private static final EcuAddress ADDRESS = new EcuAddress(0x769, 0x7DF, 0x669, "AMVAR");
    private static final String PROTOCOL = "UDS";

    public CitroenAmvar() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("suspension_type", 0x02);               // 0=standard, 1=PHC, 2=AMVAR adaptive
        map.put("ride_height_adjustable", 0x00);         // 0=no, 1=yes (C5 Aircross)
        map.put("ride_height_normal_mm", 180);           // normal ride height
        map.put("ride_height_raised_mm", 210);           // raised position
        map.put("ride_height_lowered_mm", 155);          // lowered position
        map.put("damping_mode_comfort", 0x01);           // comfort damping mode
        map.put("damping_mode_normal", 0x01);            // normal damping mode
        map.put("damping_mode_sport", 0x01);             // sport damping mode
        map.put("auto_leveling_rear", 0x01);             // rear auto-leveling
        map.put("load_compensation", 0x01);              // load-dependent damping
        map.put("speed_dependent_lowering", 0x01);       // lower at high speed
        map.put("speed_lowering_threshold_kph", 170);    // speed for auto lowering
        map.put("phc_bump_stop_type", 0x01);             // hydraulic bump stop variant
        map.put("corner_sensor_count", 0x04);            // ride height sensors per corner
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
                0x9001,  // Suspension mode
                0x9002,  // Ride height FL
                0x9003,  // Ride height FR
                0x9004,  // Ride height RL
                0x9005,  // Ride height RR
                0x9010,  // Damper current FL
                0x9011,  // Damper current FR
                0x9012,  // Damper current RL
                0x9013   // Damper current RR
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("SUSPENSION_CONFIG", new byte[]{0x02, 0x00, 0x01, 0x01, 0x01});
        blocks.put("RIDE_HEIGHT_CONFIG", new byte[]{(byte) 0xB4, (byte) 0xD2, (byte) 0x9B});
        blocks.put("DAMPING_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x01});
        blocks.put("AUTO_LEVEL_CONFIG", new byte[]{0x01, 0x01, (byte) 0xAA});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Ride height sensor FL calibration");
        channels.put(0x02, "Ride height sensor FR calibration");
        channels.put(0x03, "Ride height sensor RL calibration");
        channels.put(0x04, "Ride height sensor RR calibration");
        channels.put(0x05, "Normal ride height offset");
        channels.put(0x06, "Comfort damping softness level");
        channels.put(0x07, "Sport damping firmness level");
        channels.put(0x08, "Auto lowering speed threshold");
        return channels;
    }
}
