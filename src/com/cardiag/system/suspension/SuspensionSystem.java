package com.cardiag.system.suspension;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for suspension and ride-height control, including
 * air suspension, adaptive damping, and self-levelling systems.
 *
 * <p>Provides operations for reading and setting ride height, damping modes,
 * calibration, self-levelling configuration, and air spring pressure
 * monitoring.</p>
 */
public abstract class SuspensionSystem extends VehicleSystem {

    /**
     * Creates a new suspension subsystem and sets the system type to
     * {@link SystemType#SUSPENSION}.
     */
    protected SuspensionSystem() {
        super();
        this.systemType = SystemType.SUSPENSION;
    }

    /**
     * Reads the current ride height values for each corner of the vehicle.
     *
     * @return a map of corner name to ride height in millimetres
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readRideHeight() throws IOException;

    /**
     * Sets the target ride height for the vehicle.
     *
     * @param mm target ride height in millimetres
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code mm} is out of range
     */
    public abstract void setRideHeight(int mm) throws IOException;

    /**
     * Reads the current damping mode (e.g. Comfort, Sport, Sport+).
     *
     * @return the active damping mode name
     * @throws IOException if the read operation fails
     */
    public abstract String readDampingMode() throws IOException;

    /**
     * Sets the damping mode for the adaptive suspension.
     *
     * @param mode the desired damping mode name (e.g. "Comfort", "Sport")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code mode} is null or unknown
     */
    public abstract void setDampingMode(String mode) throws IOException;

    /**
     * Performs a full suspension calibration procedure, recording the
     * current ride height as the reference baseline.
     *
     * @throws IOException if the calibration procedure fails
     */
    public abstract void calibrateSuspension() throws IOException;

    /**
     * Reads the current self-levelling system status.
     *
     * @return a map of self-levelling parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readSelfLevelingStatus() throws IOException;

    /**
     * Enables or disables the automatic self-levelling system.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setSelfLeveling(boolean enabled) throws IOException;

    /**
     * Reads the current air spring pressures for each corner.
     *
     * @return a map of corner name to air spring pressure in bar
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readAirSpringPressures() throws IOException;
}
