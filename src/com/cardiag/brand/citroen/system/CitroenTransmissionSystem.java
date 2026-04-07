package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.transmission.TransmissionSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen transmission system implementation for AL4 and EAT8 gearboxes.
 *
 * <p>Supports gear adaptation reading/resetting, shift point adjustment,
 * torque converter monitoring, and fluid level checks for both the legacy
 * AL4 4-speed and the modern Aisin EAT8 8-speed automatic transmissions.</p>
 */
public class CitroenTransmissionSystem extends TransmissionSystem {

    private static final String ECU_ID = "TCU";
    private static final String ECU_NAME = "Transmission Control Unit (AL4/EAT8)";
    private static final int TCU_LOGICAL = 0x7E1;
    private static final int TCU_PHYSICAL = 0x7E9;

    private static final int DID_GEAR_ADAPT_BASE = 0xB001;
    private static final int DID_SHIFT_POINT_BASE = 0xB010;
    private static final int DID_TORQUE_CONVERTER = 0xB020;
    private static final int DID_TRANS_TEMP = 0xB030;
    private static final int DID_FLUID_LEVEL = 0xB031;

    private static final int ROUTINE_RESET_ADAPT = 0xBF01;
    private static final int ROUTINE_FLUID_CHECK = 0xBF02;

    public CitroenTransmissionSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, TCU_LOGICAL, TCU_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read gear adaptation values (AL4/EAT8)",
                "Reset gear adaptation to factory defaults",
                "Read shift point definitions",
                "Adjust shift points per gear",
                "Read torque converter slip and lock-up data",
                "Read transmission fluid temperature",
                "Perform automated fluid level check"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Transmission System - AL4/EAT8 Automatic";
    }

    @Override
    public Map<String, Double> readGearAdaptation() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> adaptation = new LinkedHashMap<>();

        String[] channels = {
                "clutch_fill_time_ms", "shift_pressure_1_2", "shift_pressure_2_3",
                "shift_pressure_3_4", "shift_overlap_time_ms", "torque_reduction_factor",
                "lockup_slip_threshold_rpm", "eat8_shift_quality_index"
        };

        for (int i = 0; i < channels.length; i++) {
            byte[] data = readDid(conn, DID_GEAR_ADAPT_BASE + i);
            adaptation.put(channels[i], decodeValue16(data));
        }

        return adaptation;
    }

    @Override
    public void resetGearAdaptation() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_RESET_ADAPT, new byte[0]);
    }

    @Override
    public Map<Integer, Integer> readShiftPoints() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<Integer, Integer> shiftPoints = new LinkedHashMap<>();

        for (int gear = 1; gear <= 8; gear++) {
            byte[] data = readDid(conn, DID_SHIFT_POINT_BASE + gear - 1);
            shiftPoints.put(gear, decodeInt16(data));
        }

        return shiftPoints;
    }

    @Override
    public void adjustShiftPoints(Map<Integer, Integer> shiftPoints) throws IOException {
        if (shiftPoints == null || shiftPoints.isEmpty()) {
            throw new IllegalArgumentException("Shift points must not be null or empty");
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        for (Map.Entry<Integer, Integer> entry : shiftPoints.entrySet()) {
            int gear = entry.getKey();
            int speed = entry.getValue();
            int did = DID_SHIFT_POINT_BASE + gear - 1;

            byte[] writeData = new byte[]{
                    0x2E, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF),
                    (byte) ((speed >> 8) & 0xFF), (byte) (speed & 0xFF)
            };
            proto.sendRequest(writeData);
            proto.readResponse();
        }
    }

    @Override
    public Map<String, Double> readTorqueConverterData() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_TORQUE_CONVERTER);

        Map<String, Double> tc = new LinkedHashMap<>();
        if (data != null && data.length >= 9) {
            tc.put("slip_rpm", (double) (((data[3] & 0xFF) << 8) | (data[4] & 0xFF)));
            tc.put("lockup_state", (double) (data[5] & 0xFF));
            tc.put("applied_pressure_bar", (double) (((data[6] & 0xFF) << 8) | (data[7] & 0xFF)) / 10.0);
            tc.put("converter_temp_c", (double) (data[8] & 0xFF) - 40.0);
        }

        return tc;
    }

    @Override
    public double readTransmissionTemp() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_TRANS_TEMP);
        if (data != null && data.length >= 4) {
            return (data[3] & 0xFF) - 40.0;
        }
        return 0.0;
    }

    @Override
    public String performFluidLevelCheck() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        byte[] result = executeRoutineWithResult(proto, ROUTINE_FLUID_CHECK, new byte[0]);

        if (result != null && result.length > 0) {
            int status = result[0] & 0xFF;
            switch (status) {
                case 0x00: return "Fluid level OK";
                case 0x01: return "Fluid level LOW - top up required";
                case 0x02: return "Fluid level HIGH - drain required";
                case 0x03: return "Fluid temperature out of range for measurement";
                default: return "Unknown status: 0x" + Integer.toHexString(status);
            }
        }
        return "Unable to determine fluid level";
    }

    // -- Helpers --

    private byte[] readDid(EcuConnection conn, int did) throws IOException {
        DiagnosticProtocol proto = conn.getProtocol();
        byte[] request = new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
        proto.sendRequest(request);
        return proto.readResponse();
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

    private byte[] executeRoutineWithResult(DiagnosticProtocol proto, int routineId, byte[] data) throws IOException {
        byte[] request = new byte[4 + data.length];
        request[0] = 0x31;
        request[1] = 0x01;
        request[2] = (byte) ((routineId >> 8) & 0xFF);
        request[3] = (byte) (routineId & 0xFF);
        System.arraycopy(data, 0, request, 4, data.length);
        proto.sendRequest(request);
        byte[] response = proto.readResponse();
        if (response != null && response.length > 4) {
            return Arrays.copyOfRange(response, 4, response.length);
        }
        return new byte[0];
    }

    private double decodeValue16(byte[] data) {
        if (data == null || data.length < 5) return 0.0;
        return ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
    }

    private int decodeInt16(byte[] data) {
        if (data == null || data.length < 5) return 0;
        return ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
    }
}
