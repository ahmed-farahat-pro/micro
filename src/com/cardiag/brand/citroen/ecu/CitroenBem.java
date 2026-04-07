package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * BEM (Built-in Equipment Module) definition for Citroen vehicles.
 *
 * <p>The BEM manages dashboard switches, interior equipment controls,
 * steering column switches, and driver interface inputs. CAN address 0x765/0x665.</p>
 */
public class CitroenBem extends EcuDefinition {

    private static final String ECU_NAME = "BEM (Built-in Equipment Module)";
    private static final EcuAddress ADDRESS = new EcuAddress(0x765, 0x7DF, 0x665, "BEM");
    private static final String PROTOCOL = "UDS";

    public CitroenBem() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("steering_column_type", 0x02);            // 0=fixed, 1=tilt, 2=tilt+telescopic
        map.put("cruise_control_type", 0x02);             // 0=none, 1=CC, 2=ACC
        map.put("wiper_stalk_type", 0x01);                // 0=basic, 1=auto rain
        map.put("light_switch_type", 0x02);               // 0=manual, 1=auto, 2=auto+high beam assist
        map.put("hazard_switch_present", 0x01);
        map.put("esp_switch_present", 0x01);
        map.put("parking_sensor_switch", 0x01);
        map.put("lane_keep_switch", 0x01);
        map.put("eco_mode_switch", 0x01);
        map.put("sport_mode_switch", 0x01);
        map.put("heated_steering_wheel", 0x00);
        map.put("start_button_type", 0x01);               // 0=key, 1=push button start
        map.put("electric_handbrake_switch", 0x01);
        map.put("autohold_switch", 0x01);
        map.put("multifunction_display_controls", 0x01);
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
                0x3001,  // Switch configuration word
                0x3002,  // Column switch status
                0x3003   // Equipment options bitmap
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("COLUMN_CONFIG", new byte[]{0x02, 0x02, 0x01, 0x02});
        blocks.put("SWITCH_CONFIG", new byte[]{0x01, 0x01, 0x01, 0x01, 0x01, 0x01});
        blocks.put("EQUIPMENT_CONFIG", new byte[]{0x00, 0x01, 0x01, 0x01, 0x01});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Cruise control type selection");
        channels.put(0x02, "Light switch mode");
        channels.put(0x03, "Wiper stalk configuration");
        channels.put(0x04, "Start button mode");
        return channels;
    }
}
