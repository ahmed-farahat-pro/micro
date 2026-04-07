package com.cardiag.core.ecu;

import com.cardiag.core.io.DataReader;

import java.io.IOException;
import java.util.Objects;

/**
 * Immutable model holding the identification data read from an ECU.
 *
 * <p>Standard UDS Data Identifiers (DIDs) used:
 * <ul>
 *   <li>0xF191 - ECU Hardware Version Number</li>
 *   <li>0xF189 - ECU Software Version Number</li>
 *   <li>0xF187 - Part Number</li>
 *   <li>0xF18C - ECU Serial Number</li>
 *   <li>0xF190 - Vehicle Identification Number (VIN)</li>
 *   <li>0xF18B - ECU Manufacturing Date</li>
 * </ul>
 */
public final class EcuIdentification {

    /** Standard UDS DIDs for ECU identification. */
    public static final int DID_HARDWARE_VERSION = 0xF191;
    public static final int DID_SOFTWARE_VERSION = 0xF189;
    public static final int DID_PART_NUMBER = 0xF187;
    public static final int DID_SERIAL_NUMBER = 0xF18C;
    public static final int DID_VIN = 0xF190;
    public static final int DID_MANUFACTURER_DATE = 0xF18B;

    private final String hardwareVersion;
    private final String softwareVersion;
    private final String partNumber;
    private final String serialNumber;
    private final String vin;
    private final String manufacturerDate;

    /**
     * Constructs a new {@code EcuIdentification}.
     *
     * @param hardwareVersion  the hardware version string
     * @param softwareVersion  the software version string
     * @param partNumber       the part number string
     * @param serialNumber     the serial number string
     * @param vin              the vehicle identification number
     * @param manufacturerDate the manufacturing date string
     */
    public EcuIdentification(String hardwareVersion,
                             String softwareVersion,
                             String partNumber,
                             String serialNumber,
                             String vin,
                             String manufacturerDate) {
        this.hardwareVersion = Objects.requireNonNull(hardwareVersion, "hardwareVersion must not be null");
        this.softwareVersion = Objects.requireNonNull(softwareVersion, "softwareVersion must not be null");
        this.partNumber = Objects.requireNonNull(partNumber, "partNumber must not be null");
        this.serialNumber = Objects.requireNonNull(serialNumber, "serialNumber must not be null");
        this.vin = Objects.requireNonNull(vin, "vin must not be null");
        this.manufacturerDate = Objects.requireNonNull(manufacturerDate, "manufacturerDate must not be null");
    }

    /**
     * Reads the standard identification DIDs from an active ECU connection and
     * returns a populated {@code EcuIdentification}.
     *
     * <p>If a particular DID cannot be read (e.g. because the ECU does not support it),
     * the corresponding field is set to an empty string rather than failing the
     * entire operation.</p>
     *
     * @param connection the active ECU connection (must be connected)
     * @return a new {@code EcuIdentification} populated with the ECU's data
     * @throws IOException if all reads fail or the connection is broken
     */
    public static EcuIdentification fromEcuConnection(EcuConnection connection) throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");

        String hw = readDidAsAscii(connection, DID_HARDWARE_VERSION);
        String sw = readDidAsAscii(connection, DID_SOFTWARE_VERSION);
        String pn = readDidAsAscii(connection, DID_PART_NUMBER);
        String sn = readDidAsAscii(connection, DID_SERIAL_NUMBER);
        String vinStr = readDidAsAscii(connection, DID_VIN);
        String date = readDidAsAscii(connection, DID_MANUFACTURER_DATE);

        return new EcuIdentification(hw, sw, pn, sn, vinStr, date);
    }

    /**
     * Attempts to read a DID and parse its payload as an ASCII string.
     * Returns an empty string if the read fails.
     */
    private static String readDidAsAscii(EcuConnection connection, int did) {
        try {
            byte[] data = connection.readData(did);
            if (data == null || data.length == 0) {
                return "";
            }
            return DataReader.parseAscii(data, 0, data.length);
        } catch (IOException e) {
            return "";
        }
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /** Returns the ECU hardware version number. */
    public String getHardwareVersion() {
        return hardwareVersion;
    }

    /** Returns the ECU software version number. */
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    /** Returns the ECU part number. */
    public String getPartNumber() {
        return partNumber;
    }

    /** Returns the ECU serial number. */
    public String getSerialNumber() {
        return serialNumber;
    }

    /** Returns the Vehicle Identification Number (VIN). */
    public String getVin() {
        return vin;
    }

    /** Returns the ECU manufacturing date. */
    public String getManufacturerDate() {
        return manufacturerDate;
    }

    @Override
    public String toString() {
        return String.format(
                "EcuIdentification{hw='%s', sw='%s', partNo='%s', serial='%s', vin='%s', mfgDate='%s'}",
                hardwareVersion, softwareVersion, partNumber, serialNumber, vin, manufacturerDate);
    }
}
