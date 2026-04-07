package com.cardiag.system.charging;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for charging, battery management, and energy recovery
 * systems. Supports both conventional 12V batteries and high-voltage EV
 * battery packs.
 *
 * <p>Provides operations for reading battery management data, charging status,
 * alternator output, regenerative braking levels, HV battery health, and
 * charging schedule configuration.</p>
 */
public abstract class ChargingSystem extends VehicleSystem {

    /**
     * Creates a new charging subsystem and sets the system type to
     * {@link SystemType#CHARGING}.
     */
    protected ChargingSystem() {
        super();
        this.systemType = SystemType.CHARGING;
    }

    /**
     * Reads the current battery management system data including state of
     * charge, voltage, current, and temperature.
     *
     * @return a map of battery management parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readBatteryManagement() throws IOException;

    /**
     * Reads the current charging status including charge rate, connector
     * state, and estimated time to full.
     *
     * @return a map of charging status parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readChargingStatus() throws IOException;

    /**
     * Sets the maximum charging limit as a percentage of total battery
     * capacity. Useful for extending battery longevity by avoiding
     * full charges.
     *
     * @param percent the charging limit percentage (typically 50-100)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code percent} is out of range
     */
    public abstract void setChargingLimit(int percent) throws IOException;

    /**
     * Reads the current alternator / DC-DC converter output parameters.
     *
     * @return a map of alternator parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readAlternatorOutput() throws IOException;

    /**
     * Sets the regenerative braking intensity level.
     *
     * @param level regenerative braking level (0 = off, higher = stronger
     *              regeneration)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code level} is out of range
     */
    public abstract void setRegenerativeBraking(int level) throws IOException;

    /**
     * Reads the high-voltage battery health data including state of health
     * (SOH), cycle count, and degradation metrics.
     *
     * @return a map of HV battery health parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readHvBatteryHealth() throws IOException;

    /**
     * Sets the charging schedule for timed charging. The schedule string
     * defines when the vehicle should start and stop charging.
     *
     * @param schedule the charging schedule definition (e.g.
     *                 "weekdays:23:00-06:00" or "daily:01:00-05:00")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code schedule} is null or
     *                                  incorrectly formatted
     */
    public abstract void setChargingSchedule(String schedule) throws IOException;
}
