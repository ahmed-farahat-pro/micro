package com.cardiag.system.comfort;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for comfort and convenience features.
 *
 * <p>Covers seat memory positions, ambient lighting, convenience closing,
 * key configuration, and panic-alarm duration.</p>
 */
public abstract class ComfortSystem extends VehicleSystem {

    /**
     * Creates a new comfort subsystem and sets the system type to
     * {@link SystemType#COMFORT}.
     */
    protected ComfortSystem() {
        super();
        this.systemType = SystemType.COMFORT;
    }

    /**
     * Reads all stored seat-memory positions.
     *
     * @return a map of memory slot number to the raw position data
     * @throws IOException if the read operation fails
     */
    public abstract Map<Integer, byte[]> readSeatMemoryPositions() throws IOException;

    /**
     * Stores a seat position into the specified memory slot.
     *
     * @param slot         the memory slot index (typically 1-3)
     * @param positionData raw position data to store
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if the slot is out of range or data is null
     */
    public abstract void setSeatMemory(int slot, byte[] positionData) throws IOException;

    /**
     * Reads the current ambient-lighting configuration.
     *
     * @return a map of zone name to its colour / brightness settings
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readAmbientLightingConfig() throws IOException;

    /**
     * Sets the ambient lighting colour using RGB components.
     *
     * @param red   red channel (0-255)
     * @param green green channel (0-255)
     * @param blue  blue channel (0-255)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if any colour value is out of range
     */
    public abstract void setAmbientLightColor(int red, int green, int blue) throws IOException;

    /**
     * Reads the current convenience-feature configuration.
     *
     * @return a map of feature name to its current state description
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readConvenienceConfig() throws IOException;

    /**
     * Enables or disables comfort closing (windows and sunroof close via
     * key-hold on the remote).
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setComfortClosing(boolean enabled) throws IOException;

    /**
     * Reads the remote-key configuration (number of keys, functions, etc.).
     *
     * @return a map of configuration parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readKeyConfig() throws IOException;

    /**
     * Sets the duration of the panic-alarm activation.
     *
     * @param seconds duration in seconds
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code seconds} is not positive
     */
    public abstract void setPanicAlarmDuration(int seconds) throws IOException;
}
