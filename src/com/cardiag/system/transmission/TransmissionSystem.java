package com.cardiag.system.transmission;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for automatic and manual transmission diagnostics
 * and programming.
 *
 * <p>Covers gear adaptation values, shift-point adjustment, torque-converter
 * monitoring, transmission temperature, and fluid-level checks.</p>
 */
public abstract class TransmissionSystem extends VehicleSystem {

    /**
     * Creates a new transmission subsystem and sets the system type to
     * {@link SystemType#TRANSMISSION}.
     */
    protected TransmissionSystem() {
        super();
        this.systemType = SystemType.TRANSMISSION;
    }

    /**
     * Reads the current gear adaptation / learning values from the TCU.
     *
     * @return a map of adaptation channel name to its current value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readGearAdaptation() throws IOException;

    /**
     * Resets all gear adaptation values to factory defaults, forcing
     * the TCU to re-learn shift behaviour.
     *
     * @throws IOException if the reset operation fails
     */
    public abstract void resetGearAdaptation() throws IOException;

    /**
     * Reads the current shift-point definitions (speed thresholds per gear).
     *
     * @return a map of gear number to shift-point speed in km/h
     * @throws IOException if the read operation fails
     */
    public abstract Map<Integer, Integer> readShiftPoints() throws IOException;

    /**
     * Adjusts individual shift-point speeds.
     *
     * @param shiftPoints a map of gear number to desired shift-point speed in km/h
     * @throws IOException              if the adjustment fails
     * @throws IllegalArgumentException if {@code shiftPoints} is null or empty
     */
    public abstract void adjustShiftPoints(Map<Integer, Integer> shiftPoints) throws IOException;

    /**
     * Reads torque-converter status data such as slip, lock-up state, and
     * applied pressure.
     *
     * @return a map of parameter name to its current value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readTorqueConverterData() throws IOException;

    /**
     * Reads the current transmission fluid temperature.
     *
     * @return temperature in degrees Celsius
     * @throws IOException if the read operation fails
     */
    public abstract double readTransmissionTemp() throws IOException;

    /**
     * Performs an automated transmission-fluid level check using the
     * mechatronic unit's internal sensor.
     *
     * @return a human-readable result describing the fluid level status
     * @throws IOException if the check cannot be performed
     */
    public abstract String performFluidLevelCheck() throws IOException;
}
