package com.cardiag.protocol.uds;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Builder for constructing UDS (Unified Diagnostic Services) request payloads.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * byte[] request = new UdsRequest.Builder()
 *         .withServiceId(UdsServiceId.READ_DATA_BY_ID)
 *         .withData(new byte[]{0xF1, 0x90})
 *         .build();
 * }</pre>
 *
 * <p>Static helper methods are also provided for the most common request types.</p>
 */
public final class UdsRequest {

    private UdsRequest() {
        // utility class – use Builder or static helpers
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Builder for assembling a UDS request byte array.
     */
    public static final class Builder {

        private UdsServiceId serviceId;
        private int subFunction = -1;
        private byte[] data;

        /**
         * Sets the UDS service identifier for this request.
         *
         * @param serviceId the service ID
         * @return this builder
         */
        public Builder withServiceId(UdsServiceId serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        /**
         * Sets the sub-function byte. Not all services use a sub-function;
         * leave unset if the service does not require one.
         *
         * @param subFunction the sub-function value (0x00-0xFF)
         * @return this builder
         */
        public Builder withSubFunction(int subFunction) {
            this.subFunction = subFunction & 0xFF;
            return this;
        }

        /**
         * Sets additional request data bytes that follow the service ID
         * (and optional sub-function).
         *
         * @param data the data bytes
         * @return this builder
         */
        public Builder withData(byte[] data) {
            this.data = data != null ? Arrays.copyOf(data, data.length) : null;
            return this;
        }

        /**
         * Builds the UDS request byte array.
         *
         * @return the request bytes
         * @throws IllegalStateException if no service ID has been set
         */
        public byte[] build() {
            if (serviceId == null) {
                throw new IllegalStateException("Service ID must be set");
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(serviceId.getId());

            if (subFunction >= 0) {
                bos.write(subFunction);
            }

            if (data != null && data.length > 0) {
                bos.write(data, 0, data.length);
            }

            return bos.toByteArray();
        }
    }

    // -------------------------------------------------------------------------
    // Static helpers for common requests
    // -------------------------------------------------------------------------

    /**
     * Creates a DiagnosticSessionControl request.
     *
     * @param sessionType the desired session type (e.g. 0x01 default, 0x02 programming, 0x03 extended)
     * @return the request bytes
     */
    public static byte[] diagnosticSessionControl(int sessionType) {
        return new Builder()
                .withServiceId(UdsServiceId.DIAGNOSTIC_SESSION_CONTROL)
                .withSubFunction(sessionType)
                .build();
    }

    /**
     * Creates an ECUReset request.
     *
     * @param resetType the reset type (e.g. 0x01 hard reset, 0x02 key off/on, 0x03 soft reset)
     * @return the request bytes
     */
    public static byte[] ecuReset(int resetType) {
        return new Builder()
                .withServiceId(UdsServiceId.ECU_RESET)
                .withSubFunction(resetType)
                .build();
    }

    /**
     * Creates a ReadDataByIdentifier request.
     *
     * @param dataId the two-byte data identifier (e.g. 0xF190 for VIN)
     * @return the request bytes
     */
    public static byte[] readDataById(int dataId) {
        return new Builder()
                .withServiceId(UdsServiceId.READ_DATA_BY_ID)
                .withData(new byte[]{
                        (byte) ((dataId >> 8) & 0xFF),
                        (byte) (dataId & 0xFF)
                })
                .build();
    }

    /**
     * Creates a WriteDataByIdentifier request.
     *
     * @param dataId the two-byte data identifier
     * @param value  the value bytes to write
     * @return the request bytes
     */
    public static byte[] writeDataById(int dataId, byte[] value) {
        byte[] payload = new byte[2 + value.length];
        payload[0] = (byte) ((dataId >> 8) & 0xFF);
        payload[1] = (byte) (dataId & 0xFF);
        System.arraycopy(value, 0, payload, 2, value.length);

        return new Builder()
                .withServiceId(UdsServiceId.WRITE_DATA_BY_ID)
                .withData(payload)
                .build();
    }

    /**
     * Creates a SecurityAccess request (request seed).
     *
     * @param accessLevel the security access level (odd number for seed request)
     * @return the request bytes
     */
    public static byte[] securityAccessRequestSeed(int accessLevel) {
        return new Builder()
                .withServiceId(UdsServiceId.SECURITY_ACCESS)
                .withSubFunction(accessLevel)
                .build();
    }

    /**
     * Creates a SecurityAccess request (send key).
     *
     * @param accessLevel the security access level (even number for key send)
     * @param key         the security key bytes
     * @return the request bytes
     */
    public static byte[] securityAccessSendKey(int accessLevel, byte[] key) {
        return new Builder()
                .withServiceId(UdsServiceId.SECURITY_ACCESS)
                .withSubFunction(accessLevel)
                .withData(key)
                .build();
    }

    /**
     * Creates a TesterPresent request.
     *
     * @param suppressPositiveResponse if {@code true}, sets the suppress-positive-response bit
     * @return the request bytes
     */
    public static byte[] testerPresent(boolean suppressPositiveResponse) {
        return new Builder()
                .withServiceId(UdsServiceId.TESTER_PRESENT)
                .withSubFunction(suppressPositiveResponse ? 0x80 : 0x00)
                .build();
    }

    /**
     * Creates a ClearDiagnosticInformation request for all DTCs (0xFFFFFF).
     *
     * @return the request bytes
     */
    public static byte[] clearAllDtcs() {
        return new Builder()
                .withServiceId(UdsServiceId.CLEAR_DTC)
                .withData(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF})
                .build();
    }

    /**
     * Creates a ReadDTCInformation request.
     *
     * @param subFunction the report type sub-function
     * @param statusMask  the DTC status mask
     * @return the request bytes
     */
    public static byte[] readDtcInformation(int subFunction, int statusMask) {
        return new Builder()
                .withServiceId(UdsServiceId.READ_DTC)
                .withSubFunction(subFunction)
                .withData(new byte[]{(byte) (statusMask & 0xFF)})
                .build();
    }

    /**
     * Creates a RoutineControl request.
     *
     * @param controlType the routine control type (0x01 start, 0x02 stop, 0x03 request results)
     * @param routineId   the two-byte routine identifier
     * @param optionData  optional routine option data (may be {@code null})
     * @return the request bytes
     */
    public static byte[] routineControl(int controlType, int routineId, byte[] optionData) {
        int dataLen = 2 + (optionData != null ? optionData.length : 0);
        byte[] payload = new byte[dataLen];
        payload[0] = (byte) ((routineId >> 8) & 0xFF);
        payload[1] = (byte) (routineId & 0xFF);
        if (optionData != null) {
            System.arraycopy(optionData, 0, payload, 2, optionData.length);
        }

        return new Builder()
                .withServiceId(UdsServiceId.ROUTINE_CONTROL)
                .withSubFunction(controlType)
                .withData(payload)
                .build();
    }
}
