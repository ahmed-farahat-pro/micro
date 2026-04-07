package com.cardiag.system.climate;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for climate control (HVAC), including automatic climate,
 * compressor management, seat heating, steering wheel heating, and residual
 * heat functions.
 */
public abstract class ClimateSystem extends VehicleSystem {

    /**
     * Creates a new climate subsystem and sets the system type to
     * {@link SystemType#CLIMATE}.
     */
    protected ClimateSystem() {
        super();
        this.systemType = SystemType.CLIMATE;
    }

    /**
     * Reads the current climate control configuration.
     *
     * @return a map of climate parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readClimateConfig() throws IOException;

    /**
     * Enables or disables the automatic climate control mode.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setAutoClimate(boolean enabled) throws IOException;

    /**
     * Reads the current A/C compressor status including pressure and
     * engagement state.
     *
     * @return a map of compressor parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readCompressorStatus() throws IOException;

    /**
     * Sets the maximum cooling power limit for the A/C compressor.
     *
     * @param percent maximum cooling power as a percentage (0-100)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code percent} is out of range
     */
    public abstract void setMaxCoolingPower(int percent) throws IOException;

    /**
     * Reads the current seat heating levels for all equipped seats.
     *
     * @return a map of seat position name to its heating level
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Integer> readSeatHeatingLevels() throws IOException;

    /**
     * Sets the number of seat heating stages available.
     *
     * @param stages the number of heating stages (typically 3 or 4)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code stages} is out of range
     */
    public abstract void setSeatHeatingStages(int stages) throws IOException;

    /**
     * Reads the current steering wheel heating status and configuration.
     *
     * @return a map of steering wheel heating parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readSteeringWheelHeating() throws IOException;

    /**
     * Enables or disables the steering wheel heating feature.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setSteeringWheelHeating(boolean enabled) throws IOException;

    /**
     * Enables or disables residual heat (using engine heat after shutdown)
     * and sets its maximum duration.
     *
     * @param enabled     {@code true} to enable, {@code false} to disable
     * @param durationMin maximum residual heat duration in minutes
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code durationMin} is not positive
     */
    public abstract void setResidualHeat(boolean enabled, int durationMin) throws IOException;
}
