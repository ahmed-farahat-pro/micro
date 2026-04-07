package com.cardiag.system.body;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for the body control module (BCM).
 *
 * <p>Manages central locking behaviour, power windows, mirror configuration,
 * and wiper / rain-sensor settings.</p>
 */
public abstract class BodyControlSystem extends VehicleSystem {

    /**
     * Creates a new body-control subsystem and sets the system type to
     * {@link SystemType#BODY_CONTROL}.
     */
    protected BodyControlSystem() {
        super();
        this.systemType = SystemType.BODY_CONTROL;
    }

    /**
     * Reads the current central-locking configuration flags.
     *
     * @return a map of configuration key to its boolean state
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Boolean> readCentralLockConfig() throws IOException;

    /**
     * Programs central-lock behaviour options such as speed-lock,
     * selective unlock, and re-lock timeout.
     *
     * @param config a map of behaviour key to desired state
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code config} is null or empty
     */
    public abstract void setCentralLockBehavior(Map<String, Boolean> config) throws IOException;

    /**
     * Reads the current power-window configuration.
     *
     * @return a map of window name to its configuration value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readWindowConfig() throws IOException;

    /**
     * Enables or disables automatic window closing on lock.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setAutoCloseWindows(boolean enabled) throws IOException;

    /**
     * Reads the current exterior-mirror configuration.
     *
     * @return a map of mirror parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readMirrorConfig() throws IOException;

    /**
     * Enables or disables automatic mirror folding when the vehicle is locked.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setMirrorFoldOnLock(boolean enabled) throws IOException;

    /**
     * Reads the current windshield-wiper and rain-sensor configuration.
     *
     * @return a map of wiper parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readWiperConfig() throws IOException;

    /**
     * Sets the rain-sensor sensitivity level.
     *
     * @param level sensitivity level (typically 1 to 5)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code level} is out of range
     */
    public abstract void setRainSensorSensitivity(int level) throws IOException;
}
