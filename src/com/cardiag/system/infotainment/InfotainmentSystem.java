package com.cardiag.system.infotainment;

import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Abstract subsystem for infotainment, navigation, and media system
 * diagnostics and coding.
 *
 * <p>Provides operations for region coding, video-in-motion control,
 * Bluetooth configuration, navigation voice volume, installed application
 * listing, and startup screen configuration.</p>
 */
public abstract class InfotainmentSystem extends VehicleSystem {

    /**
     * Creates a new infotainment subsystem and sets the system type to
     * {@link SystemType#INFOTAINMENT}.
     */
    protected InfotainmentSystem() {
        super();
        this.systemType = SystemType.INFOTAINMENT;
    }

    /**
     * Reads the current region coding from the infotainment unit.
     *
     * @return a map of region coding parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readRegionCoding() throws IOException;

    /**
     * Sets the region code for the infotainment system, which controls
     * available features such as DAB frequencies and navigation maps.
     *
     * @param region the region code (e.g. "EU", "US", "JP")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code region} is null or unknown
     */
    public abstract void setRegionCode(String region) throws IOException;

    /**
     * Enables or disables video playback while the vehicle is in motion.
     *
     * @param enabled {@code true} to allow video in motion, {@code false} to
     *                restrict playback to standstill
     * @throws IOException if the write operation fails
     */
    public abstract void setVideoInMotion(boolean enabled) throws IOException;

    /**
     * Reads the current Bluetooth module configuration including paired
     * device information.
     *
     * @return a map of Bluetooth parameter to its value
     * @throws IOException if the read operation fails
     */
    public abstract Map<String, String> readBluetoothConfig() throws IOException;

    /**
     * Resets all Bluetooth pairings, removing every paired device from
     * the infotainment system.
     *
     * @throws IOException if the reset operation fails
     */
    public abstract void resetBluetoothPairings() throws IOException;

    /**
     * Sets the navigation voice prompt volume level.
     *
     * @param volume volume level (typically 0-100)
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code volume} is out of range
     */
    public abstract void setNaviVoiceVolume(int volume) throws IOException;

    /**
     * Returns a list of applications currently installed on the
     * infotainment system.
     *
     * @return a list of installed application names or identifiers
     * @throws IOException if the read operation fails
     */
    public abstract List<String> readInstalledApps() throws IOException;

    /**
     * Sets the startup screen displayed when the infotainment system boots.
     *
     * @param screen the startup screen identifier (e.g. "navigation",
     *               "media", "home", "climate")
     * @throws IOException              if the write operation fails
     * @throws IllegalArgumentException if {@code screen} is null or unknown
     */
    public abstract void setStartupScreen(String screen) throws IOException;
}
