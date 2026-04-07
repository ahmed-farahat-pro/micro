package com.cardiag.system.security;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for vehicle security, immobilizer, key management,
 * alarm system, and keyless entry operations.
 *
 * <p>Provides operations for reading immobilizer status, programming and
 * deleting keys, configuring alarm sensitivity and tilt sensor, and
 * managing keyless entry settings.</p>
 */
public abstract class SecuritySystem extends VehicleSystem {

    /**
     * Creates a new security subsystem and sets the system type to
     * {@link SystemType#SECURITY}.
     */
    protected SecuritySystem() {
        super();
        this.systemType = SystemType.SECURITY;
    }

    /**
     * Reads the current immobilizer status including synchronisation state
     * and authentication counters.
     *
     * @return a map of immobilizer parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readImmobilizerStatus() throws IOException;

    /**
     * Returns the number of keys currently registered with the vehicle.
     *
     * @return the count of programmed keys
     * @throws IOException if the read operation fails
     */
    public abstract int readKeyCount() throws IOException;

    /**
     * Programs a new transponder key into the immobilizer system.
     *
     * @param keyData raw key transponder data to program
     * @throws IOException              if the programming operation fails
     * @throws IllegalArgumentException if {@code keyData} is null or empty
     */
    public abstract void programNewKey(byte[] keyData) throws IOException;

    /**
     * Deletes a previously programmed key by its slot index.
     *
     * @param keyIndex zero-based index of the key to delete
     * @throws IOException              if the deletion fails
     * @throws IllegalArgumentException if {@code keyIndex} is out of range
     */
    public abstract void deleteKey(int keyIndex) throws IOException;

    /**
     * Reads the current alarm system configuration including sensor
     * sensitivity and tilt sensor state.
     *
     * @return a map of alarm parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readAlarmConfig() throws IOException;

    /**
     * Sets the interior movement sensor sensitivity level for the alarm.
     *
     * @param level sensitivity level (typically 1 to 10)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code level} is out of range
     */
    public abstract void setAlarmSensitivity(int level) throws IOException;

    /**
     * Enables or disables the tilt / inclination sensor that triggers the
     * alarm when the vehicle is jacked up or towed.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setTiltSensor(boolean enabled) throws IOException;

    /**
     * Reads the current keyless-entry (comfort access) configuration.
     *
     * @return a map of keyless-entry parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readKeylessEntryConfig() throws IOException;

    /**
     * Enables or disables the keyless-entry (comfort access) system.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setKeylessEntry(boolean enabled) throws IOException;
}
