package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * ABS/ESP ECU definition for Citroen vehicles.
 *
 * <p>Manages the Anti-lock Braking System, Electronic Stability Programme (ESP),
 * traction control (ASR), hill start assist, and the electric parking brake.
 * CAN address 0x760/0x660.</p>
 */
public class CitroenAbs extends EcuDefinition {

    private static final String ECU_NAME = "ABS/ESP Control Unit";
    private static final EcuAddress ADDRESS = new EcuAddress(0x760, 0x7DF, 0x660, "ABS");
    private static final String PROTOCOL = "UDS";

    public CitroenAbs() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("abs_variant", 0x03);                    // ABS generation/variant
        map.put("esp_enabled", 0x01);                    // ESP active
        map.put("esp_sport_mode", 0x01);                 // ESP sport mode available
        map.put("traction_control_enabled", 0x01);       // ASR/TCS
        map.put("hill_start_assist", 0x01);              // hill hold
        map.put("hill_descent_control", 0x00);           // hill descent (SUV models)
        map.put("electric_parking_brake", 0x01);         // EPB present
        map.put("epb_auto_hold", 0x01);                  // auto-hold function
        map.put("epb_auto_release", 0x01);               // auto release on drive away
        map.put("brake_pad_wear_sensor", 0x01);          // pad wear monitoring
        map.put("brake_disc_wipe", 0x01);                // wet brake disc drying
        map.put("emergency_brake_assist", 0x01);         // EBA/BAS
        map.put("wheel_sensor_count", 0x04);             // number of wheel speed sensors
        map.put("brake_force_distribution", 0x01);       // EBD electronic brake distribution
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
                0x7001,  // ABS configuration
                0x7002,  // ESP status
                0x7003,  // Wheel speed FL
                0x7004,  // Wheel speed FR
                0x7005,  // Wheel speed RL
                0x7006,  // Wheel speed RR
                0x7010,  // EPB status
                0x7011,  // Brake pad wear level
                0x7020   // Yaw rate sensor
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("ABS_CONFIG", new byte[]{0x03, 0x01, 0x01, 0x01, 0x01});
        blocks.put("ESP_CONFIG", new byte[]{0x01, 0x01, 0x00, 0x01});
        blocks.put("EPB_CONFIG", new byte[]{0x01, 0x01, 0x01});
        blocks.put("BRAKE_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x04, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "ESP intervention threshold");
        channels.put(0x02, "Hill start assist hold time");
        channels.put(0x03, "EPB clamp force calibration");
        channels.put(0x04, "Auto-hold engage sensitivity");
        channels.put(0x05, "Brake pad wear reset");
        channels.put(0x06, "Yaw rate sensor zero calibration");
        return channels;
    }
}
