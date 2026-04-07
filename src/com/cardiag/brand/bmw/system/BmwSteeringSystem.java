package com.cardiag.brand.bmw.system;

import com.cardiag.brand.bmw.BmwEcuMap;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.system.EcuConnection;
import com.cardiag.system.EcuDefinition;
import com.cardiag.system.steering.SteeringSystem;

import java.io.IOException;
import java.util.*;

/**
 * BMW steering system implementation using the ELV/EPS ECU.
 * Provides active steering configuration, servotronic assist curve
 * programming, steering calibration, and angle sensor reads.
 */
public class BmwSteeringSystem extends SteeringSystem {

    private static final String ELV_ID = "ELV";

    public BmwSteeringSystem() {
        super();
        addEcu(new EcuDefinition(ELV_ID, "ELV - Electronic Steering Lock",
                BmwEcuMap.ELV_ADDRESS.getPhysicalId(), BmwEcuMap.ELV_ADDRESS.getResponseId()));
    }

    @Override
    public void initialize(DiagnosticProtocol protocol) throws IOException {
        connectAll(protocol);
    }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
                "Active Steering Configuration",
                "Servotronic Assist Curve Programming",
                "Speed-Dependent Assist Toggle",
                "Steering Calibration",
                "Steering Angle Read",
                "Variable Ratio Steering"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW ELV/EPS Steering System - CAN 0x6C3/0x6CB";
    }

    @Override
    public Map<String, String> readSteeringConfig() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x8001));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 6) {
            config.put("EPS Variant", decodeEpsVariant(response[3] & 0xFF));
            config.put("Servotronic Active", (response[4] & 0x01) != 0 ? "Yes" : "No");
            config.put("Servotronic Sport Mode", (response[4] & 0x02) != 0 ? "Yes" : "No");
            config.put("Active Steering", (response[5] & 0x01) != 0 ? "Yes" : "No");
            config.put("Speed Dependent Assist", (response[6] & 0x01) != 0 ? "Yes" : "No");
            config.put("Variable Ratio", (response[6] & 0x02) != 0 ? "Yes" : "No");
        }
        return config;
    }

    @Override
    public void writeSteeringConfig(Map<String, String> settings) throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte servoFlags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("Servotronic Active"))) servoFlags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("Servotronic Sport Mode"))) servoFlags |= 0x02;

        byte activeSteer = "Yes".equalsIgnoreCase(settings.get("Active Steering")) ? (byte) 0x01 : 0x00;

        byte assistFlags = 0;
        if ("Yes".equalsIgnoreCase(settings.get("Speed Dependent Assist"))) assistFlags |= 0x01;
        if ("Yes".equalsIgnoreCase(settings.get("Variable Ratio"))) assistFlags |= 0x02;

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0x80, 0x01,
                0x01, servoFlags, activeSteer, assistFlags
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readAssistCurve() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x8002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> curve = new LinkedHashMap<>();
        if (response.length > 8) {
            curve.put("Low Speed Assist %", String.valueOf(response[3] & 0xFF));
            curve.put("Mid Speed Assist %", String.valueOf(response[4] & 0xFF));
            curve.put("High Speed Assist %", String.valueOf(response[5] & 0xFF));
            curve.put("Comfort Curve ID", String.valueOf(response[6] & 0xFF));
            curve.put("Sport Curve ID", String.valueOf(response[7] & 0xFF));
            curve.put("Return to Center Force", String.valueOf(response[8] & 0xFF));
        }
        return curve;
    }

    @Override
    public void writeAssistCurve(Map<String, String> curve) throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte lowSpeed = parseByte(curve.getOrDefault("Low Speed Assist %", "100"));
        byte midSpeed = parseByte(curve.getOrDefault("Mid Speed Assist %", "70"));
        byte highSpeed = parseByte(curve.getOrDefault("High Speed Assist %", "40"));
        byte comfortId = parseByte(curve.getOrDefault("Comfort Curve ID", "1"));
        byte sportId = parseByte(curve.getOrDefault("Sport Curve ID", "2"));
        byte returnForce = parseByte(curve.getOrDefault("Return to Center Force", "50"));

        conn.getProtocol().sendRequest(new byte[]{
                0x2E, (byte) 0x80, 0x02,
                lowSpeed, midSpeed, highSpeed, comfortId, sportId, returnForce
        });
        conn.getProtocol().readResponse();
    }

    @Override
    public void calibrateSteering() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        // Steering angle sensor calibration routine
        conn.getProtocol().sendRequest(new byte[]{0x31, 0x01, (byte) 0x80, 0x10});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, Double> readSteeringAngles() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x8010));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, Double> angles = new LinkedHashMap<>();
        if (response.length > 8) {
            angles.put("Steering Wheel Angle deg", extractSignedAngle(response, 3));
            angles.put("Steering Wheel Rate deg/s", extractSignedAngle(response, 5));
            angles.put("Road Wheel Angle deg", extractSignedAngle(response, 7));
        }
        return angles;
    }

    private static double extractSignedAngle(byte[] data, int offset) {
        int raw = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        if (raw > 32767) raw -= 65536;
        return raw / 10.0;
    }

    private static String decodeEpsVariant(int val) {
        switch (val) {
            case 0x01: return "Standard EPS";
            case 0x02: return "Sport EPS";
            case 0x03: return "Active Steering";
            case 0x04: return "Integral Active Steering";
            default: return "Unknown (0x" + Integer.toHexString(val) + ")";
        }
    }

    private static byte parseByte(String value) {
        return (byte) (Integer.parseInt(value.replaceAll("[^0-9]", "")) & 0xFF);
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
