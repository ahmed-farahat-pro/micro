package com.cardiag.protocol.uds;

import java.util.Arrays;

/**
 * Represents a parsed UDS (Unified Diagnostic Services) response message.
 *
 * <p>A positive response has a service ID equal to the request service ID + 0x40.
 * A negative response always has service ID 0x7F, followed by the rejected
 * service ID and a {@link NegativeResponseCode}.</p>
 *
 * <p>Use the static {@link #parse(byte[])} factory method to create instances.</p>
 */
public final class UdsResponse {

    /** Service ID byte value for all negative responses. */
    public static final int NEGATIVE_RESPONSE_SID = 0x7F;

    private final int serviceId;
    private final int subFunction;
    private final byte[] data;
    private final boolean positive;
    private final int negativeResponseCode;

    private UdsResponse(int serviceId, int subFunction, byte[] data,
                        boolean positive, int negativeResponseCode) {
        this.serviceId = serviceId;
        this.subFunction = subFunction;
        this.data = data != null ? data : new byte[0];
        this.positive = positive;
        this.negativeResponseCode = negativeResponseCode;
    }

    /**
     * Parses raw response bytes into a {@code UdsResponse}.
     *
     * @param bytes the raw response payload
     * @return the parsed response
     * @throws IllegalArgumentException if the byte array is {@code null} or empty
     */
    public static UdsResponse parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Response bytes must not be null or empty");
        }

        int responseSid = bytes[0] & 0xFF;

        // Negative response: 0x7F <rejected SID> <NRC>
        if (responseSid == NEGATIVE_RESPONSE_SID) {
            int rejectedSid = bytes.length > 1 ? (bytes[1] & 0xFF) : 0x00;
            int nrc = bytes.length > 2 ? (bytes[2] & 0xFF) : 0x00;
            byte[] remaining = bytes.length > 3
                    ? Arrays.copyOfRange(bytes, 3, bytes.length)
                    : new byte[0];
            return new UdsResponse(rejectedSid, -1, remaining, false, nrc);
        }

        // Positive response: <SID + 0x40> [subFunction] [data...]
        int originalSid = responseSid - 0x40;
        int subFunction = -1;
        byte[] data;

        if (bytes.length > 1) {
            subFunction = bytes[1] & 0xFF;
            data = bytes.length > 2
                    ? Arrays.copyOfRange(bytes, 2, bytes.length)
                    : new byte[0];
        } else {
            data = new byte[0];
        }

        return new UdsResponse(originalSid, subFunction, data, true, -1);
    }

    /**
     * Returns whether this is a positive response.
     *
     * @return {@code true} for a positive response, {@code false} for negative
     */
    public boolean isPositiveResponse() {
        return positive;
    }

    /**
     * Returns the original request service ID.
     * For positive responses this is derived by subtracting 0x40 from the
     * response SID. For negative responses this is the rejected service ID.
     *
     * @return the service ID
     */
    public int getServiceId() {
        return serviceId;
    }

    /**
     * Returns the sub-function byte from a positive response, or {@code -1}
     * if not present or if this is a negative response.
     *
     * @return the sub-function value, or {@code -1}
     */
    public int getSubFunction() {
        return subFunction;
    }

    /**
     * Returns the response data bytes (excluding service ID and sub-function).
     *
     * @return a copy of the data bytes
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Returns the Negative Response Code for a negative response, or {@code -1}
     * for a positive response.
     *
     * @return the NRC byte value, or {@code -1}
     */
    public int getNrc() {
        return negativeResponseCode;
    }

    /**
     * Returns the {@link NegativeResponseCode} enum for this negative response,
     * or {@code null} if this is a positive response or the code is unknown.
     *
     * @return the NRC enum constant, or {@code null}
     */
    public NegativeResponseCode getNegativeResponseCode() {
        if (positive) {
            return null;
        }
        return NegativeResponseCode.fromCode(negativeResponseCode);
    }

    @Override
    public String toString() {
        if (positive) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Positive response for SID 0x%02X", serviceId));
            if (subFunction >= 0) {
                sb.append(String.format(", subFunction=0x%02X", subFunction));
            }
            if (data.length > 0) {
                sb.append(", data=");
                for (int i = 0; i < data.length; i++) {
                    if (i > 0) sb.append(' ');
                    sb.append(String.format("%02X", data[i] & 0xFF));
                }
            }
            return sb.toString();
        } else {
            NegativeResponseCode nrcEnum = NegativeResponseCode.fromCode(negativeResponseCode);
            String nrcDesc = nrcEnum != null ? nrcEnum.getDescription()
                    : String.format("Unknown (0x%02X)", negativeResponseCode);
            return String.format("Negative response for SID 0x%02X: %s", serviceId, nrcDesc);
        }
    }
}
