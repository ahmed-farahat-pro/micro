package com.cardiag.system.lighting;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract subsystem for interior and exterior lighting control.
 *
 * <p>Provides operations for configuring headlights (DRL, adaptive, welcome,
 * cornering), tail lights (brake intensity), fog lights, and interior
 * lighting (footwell LEDs).</p>
 */
public abstract class LightingSystem extends VehicleSystem {

    /**
     * Creates a new lighting subsystem and sets the system type to
     * {@link SystemType#LIGHTING}.
     */
    protected LightingSystem() {
        super();
        this.systemType = SystemType.LIGHTING;
    }

    /**
     * Reads the current headlight configuration including DRL, adaptive,
     * and welcome light settings.
     *
     * @return a map of headlight parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readHeadlightConfig() throws IOException;

    /**
     * Enables or disables daytime running lights and sets their brightness.
     *
     * @param enabled    {@code true} to enable DRLs, {@code false} to disable
     * @param brightness brightness level (typically 0-100 percent)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code brightness} is out of range
     */
    public abstract void setDaytimeRunningLights(boolean enabled, int brightness) throws IOException;

    /**
     * Enables or disables adaptive (swivelling) headlights.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setAdaptiveHeadlights(boolean enabled) throws IOException;

    /**
     * Enables or disables welcome lights (headlights illuminate on approach).
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setWelcomeLights(boolean enabled) throws IOException;

    /**
     * Enables or disables cornering lights that illuminate when turning.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setCorneringLights(boolean enabled) throws IOException;

    /**
     * Reads the current tail light configuration including brake light
     * intensity settings.
     *
     * @return a map of tail light parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readTailLightConfig() throws IOException;

    /**
     * Sets the brake light intensity level.
     *
     * @param intensity intensity level (typically 0-100 percent)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code intensity} is out of range
     */
    public abstract void setBrakeLightIntensity(int intensity) throws IOException;

    /**
     * Enables or disables using the front fog lights as cornering lights.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws IOException if the write operation fails
     */
    public abstract void setFogLightAsCorneringLight(boolean enabled) throws IOException;

    /**
     * Reads the current interior lighting configuration including footwell
     * and ambient light settings.
     *
     * @return a map of interior light parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readInteriorLightConfig() throws IOException;

    /**
     * Enables or disables footwell lights and sets their colour.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     * @param color   colour value as a packed RGB integer (e.g. 0xFF0000 for red)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code color} is out of valid range
     */
    public abstract void setFootwellLights(boolean enabled, int color) throws IOException;
}
