package com.cardiag.brand.citroen.ecu;

import com.cardiag.core.ecu.EcuAddress;
import com.cardiag.core.ecu.EcuDefinition;

import java.util.*;

/**
 * Engine Control Module (ECM) definition for Citroen vehicles.
 *
 * <p>Covers MEV17.4 and MED17.4 Bosch engine management ECUs used across
 * the PureTech and BlueHDi engine ranges. CAN address 0x7E0/0x7E8.</p>
 *
 * <p>DIDs cover fuel injection parameters, turbo boost control, emissions
 * system status, DPF regeneration counters, and injector coding values.</p>
 */
public class CitroenEcm extends EcuDefinition {

    private static final String ECU_NAME = "Engine Control Module (MEV17/MED17)";
    private static final EcuAddress ADDRESS = new EcuAddress(0x7E0, 0x7DF, 0x7E8, "ECM");
    private static final String PROTOCOL = "UDS";

    public CitroenEcm() {
        super(ECU_NAME, ADDRESS, PROTOCOL,
                List.of(0x10, 0x11, 0x14, 0x19, 0x22, 0x27, 0x2E, 0x31, 0x34, 0x36, 0x37, 0x3E),
                buildCodingBlocks(),
                buildAdaptationChannels());
    }

    @Override
    public Map<String, Integer> getDefaultCodingMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("fuel_system_type", 0x01);           // 0=port injection, 1=direct injection
        map.put("turbo_present", 0x01);              // 0=NA, 1=turbo
        map.put("turbo_max_boost_mbar", 1450);       // max boost in mbar
        map.put("turbo_overboost_mbar", 1550);       // overboost limit
        map.put("injector_type", 0x03);              // solenoid/piezo variant
        map.put("dpf_present", 0x01);                // 0=no DPF, 1=DPF fitted
        map.put("dpf_regen_interval_km", 450);       // regen interval target
        map.put("dpf_soot_limit_g", 45);             // soot mass limit
        map.put("lambda_sensor_count", 0x02);        // number of lambda sensors
        map.put("egr_valve_type", 0x01);             // 0=none, 1=electric, 2=pneumatic
        map.put("start_stop_enabled", 0x01);         // stop/start system
        map.put("emission_standard", 0x06);          // Euro 6d
        map.put("idle_speed_rpm", 750);              // target idle RPM
        map.put("rev_limiter_rpm", 6000);            // rev limiter
        map.put("fuel_cutoff_rpm", 6200);            // fuel cut RPM
        map.put("engine_fan_threshold_c", 100);      // fan activation temp
        map.put("adblue_present", 0x01);             // AdBlue/SCR system
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
                0xF188,  // ECU software version
                0xF189,  // ECU software version date
                0xF18A,  // System supplier identifier
                0xF18C,  // ECU serial number
                0xF190,  // VIN
                0xF191,  // ECU hardware version
                0xF192,  // System supplier ECU hardware number
                0xF194,  // Calibration software version
                0xF1A0,  // PSA traceability code
                0xF1A2,  // PSA diagnostic identification
                0x1001,  // Engine RPM
                0x1002,  // Coolant temperature
                0x1003,  // Intake air temperature
                0x1010,  // Fuel rail pressure
                0x1011,  // Turbo boost actual
                0x1012,  // Turbo boost target
                0x1020,  // DPF soot mass
                0x1021,  // DPF temperature upstream
                0x1022,  // DPF differential pressure
                0x1030,  // Injector 1 correction
                0x1031,  // Injector 2 correction
                0x1032,  // Injector 3 correction
                0x1033   // Injector 4 correction (if present)
        );
    }

    private static Map<String, byte[]> buildCodingBlocks() {
        Map<String, byte[]> blocks = new LinkedHashMap<>();
        blocks.put("ENGINE_CONFIG", new byte[]{0x01, 0x03, 0x01, 0x06, 0x01});
        blocks.put("INJECTION_CONFIG", new byte[]{0x03, 0x02, 0x01, (byte) 0xC2});
        blocks.put("TURBO_CONFIG", new byte[]{0x01, 0x05, (byte) 0xAA, 0x06, 0x0E});
        blocks.put("EMISSIONS_CONFIG", new byte[]{0x06, 0x01, 0x01, 0x01});
        blocks.put("DPF_CONFIG", new byte[]{0x01, 0x01, (byte) 0xC2, 0x2D});
        return blocks;
    }

    private static Map<Integer, String> buildAdaptationChannels() {
        Map<Integer, String> channels = new LinkedHashMap<>();
        channels.put(0x01, "Idle speed offset");
        channels.put(0x02, "Injector 1 IMA coding");
        channels.put(0x03, "Injector 2 IMA coding");
        channels.put(0x04, "Injector 3 IMA coding");
        channels.put(0x05, "Injector 4 IMA coding");
        channels.put(0x10, "Long-term fuel trim bank 1");
        channels.put(0x11, "Long-term fuel trim bank 2");
        channels.put(0x20, "DPF regeneration counter reset");
        channels.put(0x21, "DPF soot mass reset");
        channels.put(0x30, "Turbo wastegate adaptation");
        channels.put(0x31, "EGR valve position learning");
        return channels;
    }
}
