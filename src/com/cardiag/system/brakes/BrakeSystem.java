package com.cardiag.system.brakes;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for brake system diagnostics and coding, including
 * ABS, ESP/DSC, brake bleed procedures, pad wear reset, and parking
 * brake calibration.
 *
 * <p>Provides operations for reading ABS and ESP configuration, performing
 * brake bleed, resetting pad wear indicators, calibrating the electronic
 * parking brake, reading fluid level, and setting brake pre-fill.</p>
 */
public abstract class BrakeSystem extends VehicleSystem {

    /**
     * Creates a new brake subsystem and sets the system type to
     * {@link SystemType#BRAKES}.
     */
    protected BrakeSystem() {
        super();
        this.systemType = SystemType.BRAKES;
    }

    /**
     * Reads the current ABS (Anti-lock Braking System) configuration.
     *
     * @return a map of ABS parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readAbsConfig() throws IOException;

    /**
     * Reads the current ESP (Electronic Stability Programme) configuration.
     *
     * @return a map of ESP parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readEspConfig() throws IOException;

    /**
     * Sets the ESP operating mode.
     *
     * @param mode the desired ESP mode (e.g. "On", "Sport", "Off")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code mode} is null or unknown
     */
    public abstract void setEspMode(String mode) throws IOException;

    /**
     * Performs an automated brake bleed procedure on the specified hydraulic
     * circuit. The vehicle must be stationary with the engine running and
     * a bleed bottle attached.
     *
     * @param circuit the hydraulic circuit number to bleed (typically 1 or 2)
     * @throws IOException              if the bleed procedure fails
     * @throws IllegalArgumentException if {@code circuit} is out of range
     */
    public abstract void performBrakeBleed(int circuit) throws IOException;

    /**
     * Resets the brake pad wear indicators to their initial values after
     * pad replacement.
     *
     * @throws IOException if the reset operation fails
     */
    public abstract void resetBrakePadWear() throws IOException;

    /**
     * Reads the current brake pad wear status for all wheels.
     *
     * @return a map of wheel position to pad wear percentage remaining
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Integer> readBrakePadStatus() throws IOException;

    /**
     * Calibrates the electronic parking brake after pad or actuator
     * replacement.
     *
     * @throws IOException if the calibration procedure fails
     */
    public abstract void calibrateParkingBrake() throws IOException;

    /**
     * Reads the current brake fluid level.
     *
     * @return a description of the fluid level status (e.g. "OK", "Low")
     * @throws IOException if the read operation fails
     */
    public abstract String readBrakeFluidLevel() throws IOException;

    /**
     * Enables or disables brake pre-fill, which lightly applies the brake
     * pads when the accelerator is released quickly.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setPrefillBrakes(boolean enabled) throws IOException;
}
