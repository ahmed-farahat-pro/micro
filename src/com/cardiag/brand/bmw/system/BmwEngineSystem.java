package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.engine.EngineSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW engine system implementation using the DME (Digital Motor Electronics).
 * Provides ISN reading, injector coding, boost pressure map access, VANOS
 * adaptation, and standard engine diagnostic functions.
 */
public class BmwEngineSystem extends EngineSystem {

    private static final String DME_ID = "DME";
    private static final int DID_ISN = 0x2001;
    private static final int DID_INJECTOR_DATA = 0x2002;
    private static final int DID_BOOST_MAP = 0x2003;
    private static final int DID_VANOS = 0x2504;

    public BmwEngineSystem() {
        super();
        addEcu(new EcuDefinition(DME_ID, "DME - Digital Motor Electronics",
                BmwEcuMap.DME_ADDRESS.getPhysicalId(), BmwEcuMap.DME_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "ISN Read/Write",
                "Injector Coding",
                "Boost Pressure Map Read",
                "VANOS Adaptation Reset",
                "Fuel Trim Read",
                "Ignition Timing Read",
                "Idle Adaptation",
                "Engine Temperature Monitoring",
                "Lambda Sensor Read"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW DME Engine Management System - UDS protocol via CAN 0x7E0/0x7E8";
    }

    @Override
    public Map<String, Double> readFuelTrims() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> trims = new LinkedHashMap<>();
        trims.put("Short Term Bank 1", extractDouble(response, 0));
        trims.put("Long Term Bank 1", extractDouble(response, 2));
        trims.put("Short Term Bank 2", extractDouble(response, 4));
        trims.put("Long Term Bank 2", extractDouble(response, 6));
        return trims;
    }

    @Override
    public Map<Integer, Double> readIgnitionTiming() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2011));
        byte[] response = conn.getProtocol().readResponse();

        Map<Integer, Double> timing = new LinkedHashMap<>();
        for (int cyl = 1; cyl <= 6; cyl++) {
            timing.put(cyl, extractDouble(response, (cyl - 1) * 2));
        }
        return timing;
    }

    @Override
    public Map<Integer, byte[]> readInjectorData() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(DID_INJECTOR_DATA));
        byte[] response = conn.getProtocol().readResponse();

        Map<Integer, byte[]> injectors = new LinkedHashMap<>();
        int injectorDataLen = 6;
        for (int i = 0; i < 6; i++) {
            int offset = 3 + (i * injectorDataLen);
            byte[] data = new byte[injectorDataLen];
            if (offset + injectorDataLen <= response.length) {
                System.arraycopy(response, offset, data, 0, injectorDataLen);
            }
            injectors.put(i + 1, data);
        }
        return injectors;
    }

    @Override
    public void codeInjectors(byte[][] injectorCodes) throws IOException {
        if (injectorCodes == null || injectorCodes.length == 0) {
            throw new IllegalArgumentException("Injector codes must not be null or empty");
        }

        EcuConnection conn = connections.get(DME_ID);

        // Enter extended diagnostic session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        // Security access
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x03});
        conn.getProtocol().readResponse();

        // Write each injector's calibration data
        for (int i = 0; i < injectorCodes.length; i++) {
            byte[] request = new byte[3 + injectorCodes[i].length];
            request[0] = 0x2E; // WriteDataByIdentifier
            request[1] = (byte) ((DID_INJECTOR_DATA >> 8) & 0xFF);
            request[2] = (byte) (DID_INJECTOR_DATA & 0xFF);
            System.arraycopy(injectorCodes[i], 0, request, 3, injectorCodes[i].length);
            conn.getProtocol().sendRequest(request);
            conn.getProtocol().readResponse();
        }
    }

    @Override
    public double readBoostPressure() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(DID_BOOST_MAP));
        byte[] response = conn.getProtocol().readResponse();
        return extractDouble(response, 0) * 10.0; // Convert to mbar
    }

    @Override
    public Map<String, Double> readLambdaValues() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2012));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> lambdas = new LinkedHashMap<>();
        lambdas.put("Bank 1 Pre-Cat", extractDouble(response, 0));
        lambdas.put("Bank 1 Post-Cat", extractDouble(response, 2));
        lambdas.put("Bank 2 Pre-Cat", extractDouble(response, 4));
        lambdas.put("Bank 2 Post-Cat", extractDouble(response, 6));
        return lambdas;
    }

    @Override
    public void performIdleAdaptation() throws IOException {
        EcuConnection conn = connections.get(DME_ID);

        // Enter extended session
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();

        // Start idle adaptation routine (RoutineControl 0x31)
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x10, 0x00});
        conn.getProtocol().readResponse();

        // Also reset VANOS adaptation values
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, 0x10, 0x01});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Double> readEngineTemperatures() throws IOException {
        EcuConnection conn = connections.get(DME_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x2013));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> temps = new LinkedHashMap<>();
        temps.put("Coolant", extractDouble(response, 0));
        temps.put("Oil", extractDouble(response, 2));
        temps.put("Intake Air", extractDouble(response, 4));
        temps.put("Exhaust Gas Bank 1", extractDouble(response, 6));
        temps.put("Exhaust Gas Bank 2", extractDouble(response, 8));
        return temps;
    }

    // ── Helper methods ──────────────────────────────────────────────────

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }

    private static double extractDouble(byte[] data, int offset) {
        if (data == null || offset + 1 >= data.length) {
            return 0.0;
        }
        int raw = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        return raw / 100.0;
    }
}
