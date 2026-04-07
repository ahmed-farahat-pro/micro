package com.cardiag.protocol.obd;

/**
 * Enumeration of standard OBD-II Parameter IDs (PIDs) for Mode 01 (current data)
 * as defined in SAE J1979 / ISO 15031-5.
 *
 * <p>Each PID includes its numeric identifier, human-readable name, measurement
 * unit, valid range, and a description of the formula used to convert the raw
 * response bytes into engineering units.</p>
 */
public enum ObdPid {

    /** PID 0x00 – Bit-encoded PIDs supported in range 01-20. */
    SUPPORTED_PIDS(0x00, "Supported PIDs [01-20]", "", 0, 0,
            "Bit-encoded; each bit indicates support for a PID"),

    /** PID 0x04 – Calculated engine load. */
    ENGINE_LOAD(0x04, "Calculated Engine Load", "%", 0, 100,
            "A / 2.55, where A is byte 0"),

    /** PID 0x05 – Engine coolant temperature. */
    COOLANT_TEMP(0x05, "Engine Coolant Temperature", "\u00B0C", -40, 215,
            "A - 40, where A is byte 0"),

    /** PID 0x0A – Fuel pressure (gauge). */
    FUEL_PRESSURE(0x0A, "Fuel Pressure", "kPa", 0, 765,
            "A * 3, where A is byte 0"),

    /** PID 0x0B – Intake manifold absolute pressure. */
    INTAKE_PRESSURE(0x0B, "Intake Manifold Absolute Pressure", "kPa", 0, 255,
            "A, where A is byte 0"),

    /** PID 0x0C – Engine RPM. */
    ENGINE_RPM(0x0C, "Engine RPM", "rpm", 0, 16383.75,
            "(256 * A + B) / 4, where A is byte 0 and B is byte 1"),

    /** PID 0x0D – Vehicle speed. */
    VEHICLE_SPEED(0x0D, "Vehicle Speed", "km/h", 0, 255,
            "A, where A is byte 0"),

    /** PID 0x0E – Timing advance. */
    TIMING_ADVANCE(0x0E, "Timing Advance", "\u00B0 before TDC", -64, 63.5,
            "(A / 2) - 64, where A is byte 0"),

    /** PID 0x0F – Intake air temperature. */
    INTAKE_TEMP(0x0F, "Intake Air Temperature", "\u00B0C", -40, 215,
            "A - 40, where A is byte 0"),

    /** PID 0x10 – Mass air flow rate. */
    MAF_RATE(0x10, "MAF Air Flow Rate", "g/s", 0, 655.35,
            "(256 * A + B) / 100, where A is byte 0 and B is byte 1"),

    /** PID 0x11 – Throttle position. */
    THROTTLE_POS(0x11, "Throttle Position", "%", 0, 100,
            "A / 2.55, where A is byte 0"),

    /** PID 0x14 – Oxygen sensor 1 voltage and short-term fuel trim. */
    O2_VOLTAGE(0x14, "O2 Sensor 1 Voltage", "V", 0, 1.275,
            "A / 200, where A is byte 0; trim = (B / 1.28) - 100"),

    /** PID 0x1C – OBD standards this vehicle conforms to. */
    OBD_STANDARD(0x1C, "OBD Standards Compliance", "", 1, 250,
            "Enumerated value A; see SAE J1979 Table B4"),

    /** PID 0x1F – Run time since engine start. */
    ENGINE_RUN_TIME(0x1F, "Run Time Since Engine Start", "s", 0, 65535,
            "256 * A + B, where A is byte 0 and B is byte 1"),

    /** PID 0x2F – Fuel tank level input. */
    FUEL_LEVEL(0x2F, "Fuel Tank Level Input", "%", 0, 100,
            "A / 2.55, where A is byte 0"),

    /** PID 0x33 – Absolute barometric pressure. */
    BARO_PRESSURE(0x33, "Absolute Barometric Pressure", "kPa", 0, 255,
            "A, where A is byte 0"),

    /** PID 0x3C – Catalyst temperature bank 1, sensor 1. */
    CATALYST_TEMP(0x3C, "Catalyst Temperature B1S1", "\u00B0C", -40, 6513.5,
            "(256 * A + B) / 10 - 40, where A is byte 0 and B is byte 1"),

    /** PID 0x42 – Control module voltage. */
    CONTROL_MODULE_VOLTAGE(0x42, "Control Module Voltage", "V", 0, 65.535,
            "(256 * A + B) / 1000, where A is byte 0 and B is byte 1"),

    /** PID 0x51 – Fuel type. */
    FUEL_TYPE(0x51, "Fuel Type", "", 0, 23,
            "Enumerated value A; see SAE J1979 Table B8"),

    /** PID 0x5C – Engine oil temperature. */
    OIL_TEMP(0x5C, "Engine Oil Temperature", "\u00B0C", -40, 210,
            "A - 40, where A is byte 0"),

    /** PID 0x5D – Fuel injection timing. */
    FUEL_INJECTION_TIMING(0x5D, "Fuel Injection Timing", "\u00B0", -210, 301.992,
            "(256 * A + B) / 128 - 210, where A is byte 0 and B is byte 1"),

    /** PID 0x5E – Engine fuel rate. */
    FUEL_RATE(0x5E, "Engine Fuel Rate", "L/h", 0, 3276.75,
            "(256 * A + B) / 20, where A is byte 0 and B is byte 1");

    private final int pid;
    private final String name;
    private final String unit;
    private final double min;
    private final double max;
    private final String formula;

    ObdPid(int pid, String name, String unit, double min, double max, String formula) {
        this.pid = pid;
        this.name = name;
        this.unit = unit;
        this.min = min;
        this.max = max;
        this.formula = formula;
    }

    /**
     * Returns the numeric PID value.
     *
     * @return the PID byte value
     */
    public int getPid() {
        return pid;
    }

    /**
     * Returns the human-readable name of this PID.
     *
     * @return the PID name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the measurement unit (e.g. "rpm", "km/h", "%").
     *
     * @return the unit string, or an empty string if unitless
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns the minimum possible value in engineering units.
     *
     * @return the minimum value
     */
    public double getMin() {
        return min;
    }

    /**
     * Returns the maximum possible value in engineering units.
     *
     * @return the maximum value
     */
    public double getMax() {
        return max;
    }

    /**
     * Returns a description of the formula used to convert raw bytes to
     * engineering units.
     *
     * @return the formula description
     */
    public String getFormula() {
        return formula;
    }

    /**
     * Looks up an {@code ObdPid} by its numeric value.
     *
     * @param pid the PID byte value
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static ObdPid fromPid(int pid) {
        for (ObdPid p : values()) {
            if (p.pid == pid) {
                return p;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown OBD-II PID: 0x%02X", pid));
    }

    @Override
    public String toString() {
        return String.format("PID 0x%02X: %s [%s]", pid, name, unit.isEmpty() ? "n/a" : unit);
    }
}
