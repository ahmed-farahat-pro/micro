package com.cardiag.system.adas;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for Advanced Driver-Assistance Systems (ADAS),
 * including camera calibration, radar configuration, lane departure warning,
 * collision warning, adaptive cruise control, and parking assist.
 *
 * <p>Provides operations for reading and calibrating cameras and radar
 * sensors, and for enabling or disabling individual ADAS features.</p>
 */
public abstract class AdasSystem extends VehicleSystem {

    /**
     * Creates a new ADAS subsystem and sets the system type to
     * {@link SystemType#ADAS}.
     */
    protected AdasSystem() {
        super();
        this.systemType = SystemType.ADAS;
    }

    /**
     * Reads the current camera calibration data including alignment
     * parameters and calibration status.
     *
     * @return a map of calibration parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readCameraCalibration() throws IOException;

    /**
     * Performs a camera calibration procedure. The vehicle should be on a
     * level surface with a calibration target positioned according to the
     * manufacturer's specifications.
     *
     * @throws IOException if the calibration procedure fails
     */
    public abstract void calibrateCamera() throws IOException;

    /**
     * Reads the current radar sensor configuration.
     *
     * @return a map of radar parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readRadarConfig() throws IOException;

    /**
     * Enables or disables the lane departure warning system.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setLaneDepartureWarning(boolean enabled) throws IOException;

    /**
     * Enables or disables the forward collision warning system and sets
     * its sensitivity level.
     *
     * @param enabled     {@code true} to enable, {@code false} to disable
     * @param sensitivity sensitivity level (higher values trigger earlier
     *                    warnings); ignored when disabling
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code sensitivity} is out of range
     */
    public abstract void setCollisionWarning(boolean enabled, int sensitivity) throws IOException;

    /**
     * Enables or disables the adaptive cruise control system.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setAdaptiveCruise(boolean enabled) throws IOException;

    /**
     * Reads the current parking assist system configuration including
     * sensor status and feature flags.
     *
     * @return a map of parking assist parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readParkingAssistConfig() throws IOException;

    /**
     * Performs a radar sensor calibration procedure. The vehicle should be
     * on a level surface with an unobstructed view ahead according to the
     * manufacturer's specifications.
     *
     * @throws IOException if the calibration procedure fails
     */
    public abstract void calibrateRadar() throws IOException;
}
