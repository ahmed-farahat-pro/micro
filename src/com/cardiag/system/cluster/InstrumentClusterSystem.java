package com.cardiag.system.cluster;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for instrument cluster diagnostics and coding,
 * including language, units, service resets, needle sweep, speed warnings,
 * mileage reading, and date/time format configuration.
 */
public abstract class InstrumentClusterSystem extends VehicleSystem {

    /**
     * Creates a new instrument cluster subsystem and sets the system type to
     * {@link SystemType#INSTRUMENT_CLUSTER}.
     */
    protected InstrumentClusterSystem() {
        super();
        this.systemType = SystemType.INSTRUMENT_CLUSTER;
    }

    /**
     * Reads the current instrument cluster configuration.
     *
     * @return a map of cluster parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readClusterConfig() throws IOException;

    /**
     * Sets the display language for the instrument cluster.
     *
     * @param lang the language code (e.g. "en", "de", "fr")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code lang} is null or unsupported
     */
    public abstract void setLanguage(String lang) throws IOException;

    /**
     * Sets the unit system displayed in the instrument cluster.
     *
     * @param unitSystem the unit system identifier (e.g. "metric", "imperial")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code unitSystem} is null or unknown
     */
    public abstract void setUnits(String unitSystem) throws IOException;

    /**
     * Resets the oil service interval counter after an oil change.
     *
     * @throws IOException if the reset operation fails
     */
    public abstract void resetOilService() throws IOException;

    /**
     * Resets the vehicle inspection / maintenance service interval counter.
     *
     * @throws IOException if the reset operation fails
     */
    public abstract void resetInspectionService() throws IOException;

    /**
     * Performs a needle sweep test where all gauge needles sweep from
     * minimum to maximum and back to verify cluster operation.
     *
     * @throws IOException if the needle sweep cannot be performed
     */
    public abstract void performNeedleSweep() throws IOException;

    /**
     * Sets the speed warning threshold. The cluster will alert the driver
     * when the vehicle exceeds the specified speed.
     *
     * @param speed the warning speed in km/h; set to 0 to disable
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code speed} is negative
     */
    public abstract void setSpeedWarning(int speed) throws IOException;

    /**
     * Reads the current odometer mileage from the instrument cluster.
     *
     * @return the total mileage in kilometres
     * @throws IOException if the read operation fails
     */
    public abstract long readMileage() throws IOException;

    /**
     * Sets the date and time display format for the instrument cluster.
     *
     * @param format the format string (e.g. "24h", "12h", "dd/MM/yyyy")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code format} is null or unknown
     */
    public abstract void setDateTimeFormat(String format) throws IOException;
}
