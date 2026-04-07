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
 * programming, steering calibration, torque reading, and speed-dependent assist.
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
                "Steering Assist Mode Read",
                "Steering Assist Level Configuration",
                "Active Steering Configuration",
                "Steering Angle Calibration",
                "Steering Torque Read",
                "Speed-Dependent Assist Toggle",
                "Servotronic Assist Curves"
        );
    }

    @Override
    public String getSystemInfo() {
        return "BMW ELV/EPS Steering System - CAN 0x6C3/0x6CB";
    }

    @Override
    public String readSteeringAssistMode() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x8001));
        byte[] response = conn.getProtocol().readResponse();

        if (response.length > 3) {
            switch (response[3] & 0xFF) {
                case 0x01: return "Comfort";
                case 0x02: return "Normal";
                case 0x03: return "Sport";
                default: return "Unknown";
            }
        }
        return "Unknown";
    }

    @Override
    public void setSteeringAssistLevel(int level) throws IOException {
        if (level < 0 || level > 10) {
            throw new IllegalArgumentException("Level must be 0-10, got: " + level);
        }

        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x80, 0x03, (byte) level});
        conn.getProtocol().readResponse();
    }

    @Override
    public Map<String, String> readActiveSteering() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x8002));
        byte[] response = conn.getProtocol().readResponse();

        Map<String, String> config = new LinkedHashMap<>();
        if (response.length > 6) {
            config.put("Active Steering Installed", (response[3] & 0x01) != 0 ? "Yes" : "No");
            config.put("Variable Ratio", (response[3] & 0x02) != 0 ? "Enabled" : "Disabled");
            config.put("Servotronic Active", (response[4] & 0x01) != 0 ? "Yes" : "No");
            config.put("Sport Mode", (response[4] & 0x02) != 0 ? "Yes" : "No");
            config.put("Speed Dependent Assist", (response[5] & 0x01) != 0 ? "Enabled" : "Disabled");
            config.put("Return to Center Force", String.valueOf(response[6] & 0xFF));
        }
        return config;
    }

    @Override
    public void setActiveSteering(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x80, 0x04, val});
        conn.getProtocol().readResponse();
    }

    @Override
    public void calibrateSteeringAngle() throws IOException {
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
    public double readSteeringTorque() throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(buildReadDid(0x8010));
        byte[] response = conn.getProtocol().readResponse();

        if (response.length > 4) {
            int raw = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
            if (raw > 32767) raw -= 65536;
            return raw / 100.0; // Nm
        }
        return 0.0;
    }

    @Override
    public void setSpeedDependentAssist(boolean enabled) throws IOException {
        EcuConnection conn = connections.get(ELV_ID);
        conn.getProtocol().sendRequest(new byte[]{0x10, 0x03});
        conn.getProtocol().readResponse();
        conn.getProtocol().sendRequest(new byte[]{0x27, 0x05});
        conn.getProtocol().readResponse();

        byte val = enabled ? (byte) 0x01 : 0x00;
        conn.getProtocol().sendRequest(new byte[]{0x2E, (byte) 0x80, 0x05, val});
        conn.getProtocol().readResponse();
    }

    private static byte[] buildReadDid(int did) {
        return new byte[]{0x22, (byte) ((did >> 8) & 0xFF), (byte) (did & 0xFF)};
    }
}
