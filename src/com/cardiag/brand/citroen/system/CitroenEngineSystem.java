package com.cardiag.brand.citroen.system;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.engine.EngineSystem;

import java.io.IOException;
import java.util.*;

/**
 * Citroen engine system implementation for MEV17.4 and MED17.4 engine ECUs.
 *
 * <p>Provides fuel trim reading, turbo boost monitoring, DPF regeneration control,
 * injector IMA coding, and PSA-specific engine adaptation routines.</p>
 */
public class CitroenEngineSystem extends EngineSystem {

    private static final String ECU_ID = "ECM";
    private static final String ECU_NAME = "Engine Control Module (MEV17/MED17)";
    private static final int ECM_LOGICAL = 0x7E0;
    private static final int ECM_PHYSICAL = 0x7E8;

    // PSA DID addresses for engine data
    private static final int DID_RPM = 0x1001;
    private static final int DID_COOLANT_TEMP = 0x1002;
    private static final int DID_INTAKE_TEMP = 0x1003;
    private static final int DID_FUEL_RAIL_PRESSURE = 0x1010;
    private static final int DID_TURBO_BOOST_ACTUAL = 0x1011;
    private static final int DID_TURBO_BOOST_TARGET = 0x1012;
    private static final int DID_DPF_SOOT_MASS = 0x1020;
    private static final int DID_DPF_TEMP_UPSTREAM = 0x1021;
    private static final int DID_DPF_DIFF_PRESSURE = 0x1022;
    private static final int DID_STFT_BANK1 = 0x1040;
    private static final int DID_LTFT_BANK1 = 0x1041;
    private static final int DID_LAMBDA_UPSTREAM = 0x1050;
    private static final int DID_LAMBDA_DOWNSTREAM = 0x1051;
    private static final int DID_OIL_TEMP = 0x1060;
    private static final int DID_INJECTOR_CORRECTION_BASE = 0x1030;

    // Routine IDs
    private static final int ROUTINE_DPF_REGEN = 0xDF01;
    private static final int ROUTINE_IDLE_ADAPT = 0xDF02;
    private static final int ROUTINE_INJECTOR_CODE = 0xDF03;

    public CitroenEngineSystem() {
        super();
        addEcu(new EcuDefinition(ECU_ID, ECU_NAME, ECM_LOGICAL, ECM_PHYSICAL));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
                "Read fuel trims (short-term and long-term)",
                "Read ignition timing per cylinder",
                "Read injector correction data (IMA coding)",
                "Code injectors (MEV17/MED17)",
                "Read turbo boost pressure (actual/target)",
                "Read lambda sensor values",
                "Perform idle speed adaptation",
                "Read engine temperatures (coolant, intake, oil)",
                "Initiate DPF forced regeneration",
                "Read DPF soot mass and differential pressure"
        );
    }

    @Override
    public String getSystemInfo() {
        return "Citro\u00ebn Engine System - MEV17.4/MED17.4 (PureTech/BlueHDi)";
    }

    @Override
    public Map<String, Double> readFuelTrims() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> trims = new LinkedHashMap<>();

        byte[] stftData = readDid(conn, DID_STFT_BANK1);
        byte[] ltftData = readDid(conn, DID_LTFT_BANK1);

        trims.put("short_term_fuel_trim_bank1", decodePercentage(stftData));
        trims.put("long_term_fuel_trim_bank1", decodePercentage(ltftData));

        return trims;
    }

    @Override
    public Map<Integer, Double> readIgnitionTiming() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<Integer, Double> timing = new LinkedHashMap<>();

        // PSA engines typically 3 or 4 cylinders
        for (int cyl = 1; cyl <= 4; cyl++) {
            byte[] data = readDid(conn, 0x1070 + cyl - 1);
            timing.put(cyl, decodeAngle(data));
        }

        return timing;
    }

    @Override
    public Map<Integer, byte[]> readInjectorData() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<Integer, byte[]> injectors = new LinkedHashMap<>();

        for (int i = 0; i < 4; i++) {
            byte[] data = readDid(conn, DID_INJECTOR_CORRECTION_BASE + i);
            injectors.put(i + 1, data);
        }

        return injectors;
    }

    @Override
    public void codeInjectors(byte[][] injectorCodes) throws IOException {
        if (injectorCodes == null || injectorCodes.length == 0) {
            throw new IllegalArgumentException("Injector codes must not be null or empty");
        }

        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        // Enter extended session and security access for injector coding
        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);

        // Write each injector's IMA code via routine control
        for (int i = 0; i < injectorCodes.length; i++) {
            byte[] routineData = new byte[injectorCodes[i].length + 1];
            routineData[0] = (byte) (i + 1); // injector number
            System.arraycopy(injectorCodes[i], 0, routineData, 1, injectorCodes[i].length);
            executeRoutine(proto, ROUTINE_INJECTOR_CODE, routineData);
        }
    }

    @Override
    public double readBoostPressure() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        byte[] data = readDid(conn, DID_TURBO_BOOST_ACTUAL);
        return decodePressureMbar(data);
    }

    @Override
    public Map<String, Double> readLambdaValues() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> lambda = new LinkedHashMap<>();

        byte[] upstream = readDid(conn, DID_LAMBDA_UPSTREAM);
        byte[] downstream = readDid(conn, DID_LAMBDA_DOWNSTREAM);

        lambda.put("lambda_upstream_bank1", decodeLambda(upstream));
        lambda.put("lambda_downstream_bank1", decodeLambda(downstream));

        return lambda;
    }

    @Override
    public void performIdleAdaptation() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_IDLE_ADAPT, new byte[0]);
    }

    @Override
    public Map<String, Double> readEngineTemperatures() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> temps = new LinkedHashMap<>();

        byte[] coolant = readDid(conn, DID_COOLANT_TEMP);
        byte[] intake = readDid(conn, DID_INTAKE_TEMP);
        byte[] oil = readDid(conn, DID_OIL_TEMP);

        temps.put("coolant_temperature_c", decodeTemperature(coolant));
        temps.put("intake_air_temperature_c", decodeTemperature(intake));
        temps.put("oil_temperature_c", decodeTemperature(oil));

        return temps;
    }

    /**
     * Initiates a forced DPF regeneration via the engine ECU.
     *
     * @throws IOException if the routine cannot be executed
     */
    public void performDpfRegeneration() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        DiagnosticProtocol proto = conn.getProtocol();

        sendSessionControl(proto, 0x03);
        sendSecurityAccess(proto);
        executeRoutine(proto, ROUTINE_DPF_REGEN, new byte[0]);
    }

    /**
     * Reads the current DPF status including soot mass and temperatures.
     *
     * @return a map of DPF parameter names to their values
     * @throws IOException if reading fails
     */
    public Map<String, Double> readDpfStatus() throws IOException {
        EcuConnection conn = connections.get(ECU_ID);
        Map<String, Double> dpf = new LinkedHashMap<>();

        byte[] soot = readDid(conn, DID_DPF_SOOT_MASS);
        byte[] temp = readDid(conn, DID_DPF_TEMP_UPSTREAM);
        byte[] diffP = readDid(conn, DID_DPF_DIFF_PRESSURE);

        dpf.put("soot_mass_grams", decodeSootMass(soot));
        dpf.put("temperature_upstream_c", decodeTemperature(temp));
        dpf.put("differential_pressure_mbar", decodePressureMbar(diffP));

        return dpf;
    }

    // -- Helper methods for UDS communication --

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

    private double decodePercentage(byte[] data) {
        if (data == null || data.length < 4) return 0.0;
        int raw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        return (raw / 128.0) - 100.0;
    }

    private double decodeAngle(byte[] data) {
        if (data == null || data.length < 4) return 0.0;
        int raw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        return raw / 100.0;
    }

    private double decodePressureMbar(byte[] data) {
        if (data == null || data.length < 4) return 0.0;
        return ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
    }

    private double decodeLambda(byte[] data) {
        if (data == null || data.length < 4) return 0.0;
        int raw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        return raw / 32768.0;
    }

    private double decodeTemperature(byte[] data) {
        if (data == null || data.length < 4) return 0.0;
        return (data[3] & 0xFF) - 40.0;
    }

    private double decodeSootMass(byte[] data) {
        if (data == null || data.length < 4) return 0.0;
        int raw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        return raw / 10.0;
    }
}
