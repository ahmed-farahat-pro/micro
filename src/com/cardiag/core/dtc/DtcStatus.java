package com.cardiag.core.dtc;

/**
 * Decodes the UDS DTC status byte (ISO 14229-1, Table 352) into individual
 * boolean flags.
 *
 * <p>The status byte is an 8-bit mask with the following layout:
 * <pre>
 *   Bit 0: testFailed
 *   Bit 1: testFailedThisOperationCycle
 *   Bit 2: pendingDTC
 *   Bit 3: confirmedDTC
 *   Bit 4: testNotCompletedSinceLastClear
 *   Bit 5: testFailedSinceLastClear
 *   Bit 6: testNotCompletedThisOperationCycle
 *   Bit 7: warningIndicatorRequested (MIL)
 * </pre>
 */
public final class DtcStatus {

    private final boolean testFailed;
    private final boolean testFailedThisOperation;
    private final boolean pendingDtc;
    private final boolean confirmedDtc;
    private final boolean testNotCompletedSinceLastClear;
    private final boolean testFailedSinceLastClear;
    private final boolean testNotCompletedThisOperation;
    private final boolean warningIndicatorRequested;
    private final int rawByte;

    private DtcStatus(int statusByte) {
        this.rawByte = statusByte & 0xFF;
        this.testFailed = (rawByte & 0x01) != 0;
        this.testFailedThisOperation = (rawByte & 0x02) != 0;
        this.pendingDtc = (rawByte & 0x04) != 0;
        this.confirmedDtc = (rawByte & 0x08) != 0;
        this.testNotCompletedSinceLastClear = (rawByte & 0x10) != 0;
        this.testFailedSinceLastClear = (rawByte & 0x20) != 0;
        this.testNotCompletedThisOperation = (rawByte & 0x40) != 0;
        this.warningIndicatorRequested = (rawByte & 0x80) != 0;
    }

    /**
     * Parses a DTC status byte into a {@code DtcStatus} instance.
     *
     * @param statusByte the raw status byte (only the low 8 bits are used)
     * @return a new {@code DtcStatus}
     */
    public static DtcStatus fromByte(int statusByte) {
        return new DtcStatus(statusByte);
    }

    // ── Flag accessors ──────────────────────────────────────────────────

    /**
     * Bit 0: {@code true} if the most recent test execution has failed.
     *
     * @return testFailed flag
     */
    public boolean isTestFailed() {
        return testFailed;
    }

    /**
     * Bit 1: {@code true} if the test has failed during the current operation cycle.
     *
     * @return testFailedThisOperationCycle flag
     */
    public boolean isTestFailedThisOperation() {
        return testFailedThisOperation;
    }

    /**
     * Bit 2: {@code true} if the DTC is pending (not yet confirmed).
     *
     * @return pendingDTC flag
     */
    public boolean isPendingDtc() {
        return pendingDtc;
    }

    /**
     * Bit 3: {@code true} if the DTC is confirmed (stored in permanent memory).
     *
     * @return confirmedDTC flag
     */
    public boolean isConfirmedDtc() {
        return confirmedDtc;
    }

    /**
     * Bit 4: {@code true} if the test has not completed since the last DTC clear.
     *
     * @return testNotCompletedSinceLastClear flag
     */
    public boolean isTestNotCompletedSinceLastClear() {
        return testNotCompletedSinceLastClear;
    }

    /**
     * Bit 5: {@code true} if the test has failed at least once since the last DTC clear.
     *
     * @return testFailedSinceLastClear flag
     */
    public boolean isTestFailedSinceLastClear() {
        return testFailedSinceLastClear;
    }

    /**
     * Bit 6: {@code true} if the test has not completed during this operation cycle.
     *
     * @return testNotCompletedThisOperationCycle flag
     */
    public boolean isTestNotCompletedThisOperation() {
        return testNotCompletedThisOperation;
    }

    /**
     * Bit 7: {@code true} if the warning indicator (MIL / check engine light) is
     * requested for this DTC.
     *
     * @return warningIndicatorRequested flag
     */
    public boolean isWarningIndicatorRequested() {
        return warningIndicatorRequested;
    }

    /**
     * Returns the raw status byte.
     *
     * @return the original status byte value (0x00-0xFF)
     */
    public int getRawByte() {
        return rawByte;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DtcStatus{0x");
        sb.append(String.format("%02X", rawByte));
        sb.append(" [");
        if (testFailed) sb.append("TEST_FAILED ");
        if (testFailedThisOperation) sb.append("FAILED_THIS_OP ");
        if (pendingDtc) sb.append("PENDING ");
        if (confirmedDtc) sb.append("CONFIRMED ");
        if (testNotCompletedSinceLastClear) sb.append("NOT_COMPLETE_SINCE_CLEAR ");
        if (testFailedSinceLastClear) sb.append("FAILED_SINCE_CLEAR ");
        if (testNotCompletedThisOperation) sb.append("NOT_COMPLETE_THIS_OP ");
        if (warningIndicatorRequested) sb.append("MIL ");
        return sb.toString().trim() + "]}";
    }
}
