package com.cardiag.system.engine;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for engine management and powertrain diagnostics.
 *
 * <p>Provides operations for reading fuel trims, ignition timing, injector
 * data, boost pressure, lambda values, and engine temperatures as well as
 * coding injectors and performing idle adaptation.</p>
 */
public abstract class EngineSystem extends VehicleSystem {

    /**
     * Creates a new engine subsystem and sets the system type to
     * {@link SystemType#ENGINE}.
     */
    protected EngineSystem() {
        super();
        this.systemType = SystemType.ENGINE;
    }

    /**
     * Reads the current short-term and long-term fuel trim values.
     *
     * @return a map of fuel trim names to their current values
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readFuelTrims() throws IOException;

    /**
     * Reads the current ignition timing advance for each cylinder.
     *
     * @return a map of cylinder number to timing advance in degrees
     * @throws IOException if the read operation fails
     */
    public abstract Map<Integer, Double> readIgnitionTiming() throws IOException;

    /**
     * Reads live injector diagnostic data such as opening time and flow rate.
     *
     * @return a map of injector index to raw data bytes
     * @throws IOException if the read operation fails
     */
    public abstract Map<Integer, byte[]> readInjectorData() throws IOException;

    /**
     * Programs injector calibration data into the engine ECU.
     *
     * @param injectorCodes calibration data for each injector; each element
     *                      corresponds to one injector in cylinder order
     * @throws IOException              if the coding operation fails
     * @throws IllegalArgumentException if {@code injectorCodes} is null or empty
     */
    public abstract void codeInjectors(byte[][] injectorCodes) throws IOException;

    /**
     * Reads the current boost / intake manifold pressure.
     *
     * @return boost pressure in millibar (mbar)
     * @throws IOException if the read operation fails
     */
    public abstract double readBoostPressure() throws IOException;

    /**
     * Reads the current lambda (oxygen sensor) values.
     *
     * @return a map of sensor position description to lambda value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readLambdaValues() throws IOException;

    /**
     * Initiates the idle-speed adaptation / learning procedure.
     *
     * @throws IOException if the adaptation procedure cannot be executed
     */
    public abstract void performIdleAdaptation() throws IOException;

    /**
     * Reads all engine-related temperature sensors (coolant, oil, intake, etc.).
     *
     * @return a map of sensor name to temperature in degrees Celsius
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, Double> readEngineTemperatures() throws IOException;
}
