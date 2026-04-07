package com.cardiag.system.steering;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for steering assist and active steering systems,
 * including EPS, servotronic, and active steering coding.
 *
 * <p>Provides operations for reading and setting steering assist levels,
 * active steering configuration, angle calibration, torque reading,
 * and speed-dependent assist.</p>
 */
public abstract class SteeringSystem extends VehicleSystem {

    /**
     * Creates a new steering subsystem and sets the system type to
     * {@link SystemType#STEERING}.
     */
    protected SteeringSystem() {
        super();
        this.systemType = SystemType.STEERING;
    }

    /**
     * Reads the current steering assist mode (e.g. Comfort, Normal, Sport).
     *
     * @return the active steering assist mode name
     * @throws IOException if the read operation fails
     */
    public abstract String readSteeringAssistMode() throws IOException;

    /**
     * Sets the steering assist level.
     *
     * @param level assist level (higher values provide more assistance)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code level} is out of range
     */
    public abstract void setSteeringAssistLevel(int level) throws IOException;

    /**
     * Reads the current active steering configuration and status.
     *
     * @return a map of active steering parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readActiveSteering() throws IOException;

    /**
     * Enables or disables the active steering (variable ratio) system.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setActiveSteering(boolean enabled) throws IOException;

    /**
     * Performs a steering angle sensor calibration procedure. The steering
     * wheel should be centred and the vehicle stationary before calling
     * this method.
     *
     * @throws IOException if the calibration procedure fails
     */
    public abstract void calibrateSteeringAngle() throws IOException;

    /**
     * Reads the current steering column torque sensor value.
     *
     * @return the torque value in Newton-metres (Nm)
     * @throws IOException if the read operation fails
     */
    public abstract double readSteeringTorque() throws IOException;

    /**
     * Enables or disables speed-dependent steering assist, which reduces
     * assistance at higher vehicle speeds.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setSpeedDependentAssist(boolean enabled) throws IOException;
}
