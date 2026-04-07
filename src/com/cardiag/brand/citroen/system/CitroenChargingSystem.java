package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.charging.ChargingSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen charging system implementation for stop/start battery management
 * and e-CMP electric vehicle charging.
 *
 * <p>Manages the IBS (Intelligent Battery Sensor) for stop/start vehicles,
 * battery registration after replacement, alternator configuration, and
 * on e-CMP models: EV charging schedules, charge limits, regenerative
 * braking energy recovery settings, and on-board charger status.</p>
 */
public class CitroenChargingSystem extends ChargingSystem {

    private static final String ECU_ID = "BSM";
    private static final String ECU_NAME = "Battery & Charging Management";
    private static final int BSM_LOGICAL = 0x7B0;
    private static final int BSM_PHYSICAL = 0x7B8;

    private static final int DID_BATTERY_STATUS = 0xD001;
    private static final int DID_ALTERNATOR_CONFIG = 0xD010;
    private static final int DID_CHARGING_HISTORY = 0xD020;
    private static final int DID_ENERGY_RECOVERY = 0xD030;
    private static final int DID_EV_CHARGE_STATUS = 0xD040;
    private static final int DID_EV_CHARGE_LIMIT = 0xD041;
    private static final int DID_EV_OBC_STATUS = 0xD042;

    private static final int ROUTINE_REGISTER_BATTERY = 0xDF10;

    public CitroenChargingSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, BSM_LOGICAL, BSM_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read battery status (voltage, current, SOC, SOH)",
                "Register new battery after replacement",
                "Read/configure alternator smart charging",
                "Read charging history and cycle count",
                "Read energy recovery data (regenerative braking)",
                "Read EV charging status (e-CMP models)",
                "Configure EV charge limit",
                "Read on-board charger status"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Charging System - Stop/Start Battery & e-CMP EV Charging";
    }

    @Override
    public Map<String, String> readBatteryStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_BATTERY_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 10) {
            int voltage = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
            status.put("voltage_v", String.format("%.1f", voltage / 100.0));

            int current = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
            status.put("current_a", String.format("%.1f", (current - 32768) / 100.0));

            status.put("state_of_charge_percent", String.valueOf(data[7] & 0xFF));
            status.put("state_of_health_percent", String.valueOf(data[8] & 0xFF));

            int temp = data[9] & 0xFF;
            status.put("temperature_c", String.valueOf(temp - 40));

            // Determine battery type
            if (data.length >= 11) {
                int type = data[10] & 0xFF;
                status.put("battery_type", type == 0x01 ? "Lead-acid (conventional)" :
                        type == 0x02 ? "AGM (stop/start)" :
                        type == 0x03 ? "EFB (Enhanced Flooded)" :
                        type == 0x04 ? "Li-ion (EV traction)" : "Unknown");
            }
        }

        return status;
    }

    @Override
    public void registerBattery(String partNumber, int capacityAh) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte[] pnBytes = partNumber.getBytes();
        byte[] routineData = new byte[pnBytes.length + 2];
        routineData[0] = (byte) ((capacityAh >> 8) & 0xFF);
        routineData[1] = (byte) (capacityAh & 0xFF);
        System.arraycopy(pnBytes, 0, routineData, 2, pnBytes.length);

        executeRoutine(proto, ROUTINE_REGISTER_BATTERY, routineData);
    }

    @Override
    public Map<String, String> readAlternatorConfig() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_ALTERNATOR_CONFIG);

        Map<String, String> config = new LinkedHashMap<>();
        if (data != null && data.length >= 7) {
            config.put("smart_charging", (data[3] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("target_voltage_v", String.format("%.1f", ((data[4] & 0xFF) << 8 | (data[5] & 0xFF)) / 100.0));
            config.put("load_response_mode", data[6] == 0x01 ? "Eco" : "Standard");
        }

        return config;
    }

    @Override
    public void writeAlternatorConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        byte smartCharging = "Enabled".equals(settings.get("smart_charging")) ? (byte) 0x01 : 0x00;
        int targetVoltage = 1440; // 14.40V default
        if (settings.containsKey("target_voltage_v")) {
            targetVoltage = (int) (Double.parseDouble(settings.get("target_voltage_v")) * 100);
        }
        byte loadResponse = "Eco".equals(settings.get("load_response_mode")) ? (byte) 0x01 : 0x00;

        writeDid(proto, DID_ALTERNATOR_CONFIG, new byte[]{
                smartCharging,
                (byte) ((targetVoltage >> 8) & 0xFF),
                (byte) (targetVoltage & 0xFF),
                loadResponse
        });
    }

    @Override
    public Map<String, String> readChargingHistory() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_CHARGING_HISTORY);

        Map<String, String> history = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            int cycles = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
            history.put("charge_cycles", String.valueOf(cycles));
            int deepDischarges = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
            history.put("deep_discharge_events", String.valueOf(deepDischarges));
            int minVoltage = ((data[7] & 0xFF) << 8) | (data[8] & 0xFF);
            history.put("minimum_voltage_recorded_v", String.format("%.2f", minVoltage / 100.0));
        }

        // EV-specific charging history
        byte[] evData = readDid(conn, DID_EV_CHARGE_STATUS);
        if (evData != null && evData.length >= 7) {
            int totalKwh = ((evData[3] & 0xFF) << 8) | (evData[4] & 0xFF);
            history.put("total_energy_charged_kwh", String.valueOf(totalKwh));
            int acSessions = ((evData[5] & 0xFF) << 8) | (evData[6] & 0xFF);
            history.put("ac_charge_sessions", String.valueOf(acSessions));
        }

        return history;
    }

    @Override
    public Map<String, Double> readEnergyRecoveryData() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_ENERGY_RECOVERY);

        Map<String, Double> recovery = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            int regenEnergy = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
            recovery.put("regen_energy_wh", (double) regenEnergy);

            int regenPower = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
            recovery.put("current_regen_power_w", (double) regenPower);

            recovery.put("regen_efficiency_percent", (double) (data[7] & 0xFF));

            int coastEnergy = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
            recovery.put("coast_recovery_wh", (double) coastEnergy);
        }

        return recovery;
    }

    /**
     * Reads the EV on-board charger status (e-CMP models only).
     *
     * @return a map of charger status parameters
     * @throws IOException if reading fails
     */
    public Map<String, String> readOnBoardChargerStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_EV_OBC_STATUS);

        Map<String, String> status = new LinkedHashMap<>();
        if (data != null && data.length >= 8) {
            int chargeState = data[3] & 0xFF;
            status.put("charge_state",
                    chargeState == 0x00 ? "Not charging" :
                    chargeState == 0x01 ? "AC charging" :
                    chargeState == 0x02 ? "DC fast charging" :
                    chargeState == 0x03 ? "Charge complete" : "Error");
            int power = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
            status.put("charging_power_w", String.valueOf(power));
            status.put("traction_battery_soc_percent", String.valueOf(data[6] & 0xFF));
            status.put("charge_limit_percent", String.valueOf(data[7] & 0xFF));
        }

        return status;
    }

    // -- Helpers --

    private byte[] readDid(EcuConnection conn, int did) throws IOException {
        DiagnosticProtocol proto = conn.getProtocol();
        byte[] request = new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
        proto.sendRequest(request);
        return proto.readResponse();
    }

    private void writeDid(DiagnosticProtocol proto, int did, byte[] data) throws IOException {
        byte[] request = new byte[3 + data.length];
        request[0] = 0x2E;
        request[1] = (byte) ((did >> 8) & 0xFF);
        request[2] = (byte) (did & 0xFF);
        System.arraycopy(data, 0, request, 3, data.length);
        proto.sendRequest(request);
        proto.readResponse();
    }

    private void sendSessionControl(DiagnosticProtocol proto, int session) throws IOException {
        proto.sendRequest(new byte[]{0x10, (byte) session});
        proto.readResponse();
    }

    private void sendSecurityAccess(DiagnosticProtocol proto) throws IOException {
        proto.sendRequest(new byte[]{0x27, 0x03});
        proto.readResponse();
    }

    private void executeRoutine(DiagnosticProtocol proto, int routineId, byte[] data) throws IOException {
        byte[] request = new byte[4 + data.length];
        request[0] = 0x31;
        request[1] = 0x01;
        request[2] = (byte) ((routineId >> 8) & 0xFF);
        request[3] = (byte) (routineId & 0xFF);
        System.arraycopy(data, 0, request, 4, data.length);
        proto.sendRequest(request);
        proto.readResponse();
    }
}
