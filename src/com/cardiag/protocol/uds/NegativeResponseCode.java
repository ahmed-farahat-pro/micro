package com.cardiag.protocol.uds;

/**
 * Enumeration of UDS Negative Response Codes (NRC) as defined in ISO 14229-1.
 *
 * <p>When an ECU cannot fulfil a diagnostic request it returns a Negative
 * Response message (service ID 0x7F) containing one of these codes.</p>
 */
public enum NegativeResponseCode {

    /** General reject – the request was rejected for an unspecified reason. */
    GENERAL_REJECT(0x10, "General reject"),

    /** Service not supported in the active session. */
    SERVICE_NOT_SUPPORTED(0x11, "Service not supported"),

    /** Sub-function not supported. */
    SUB_FUNCTION_NOT_SUPPORTED(0x12, "Sub-function not supported"),

    /** Incorrect message length or invalid format. */
    INCORRECT_MESSAGE_LENGTH(0x13, "Incorrect message length or invalid format"),

    /** The server is temporarily too busy; client should retry. */
    BUSY_REPEAT_REQUEST(0x21, "Busy – repeat request"),

    /** Conditions not correct for executing the requested action. */
    CONDITIONS_NOT_CORRECT(0x22, "Conditions not correct"),

    /** The request was received in the wrong sequence. */
    REQUEST_SEQUENCE_ERROR(0x24, "Request sequence error"),

    /** The request contains a parameter value that is out of range. */
    REQUEST_OUT_OF_RANGE(0x31, "Request out of range"),

    /** Security access was denied. */
    SECURITY_ACCESS_DENIED(0x33, "Security access denied"),

    /** The security key sent by the client is invalid. */
    INVALID_KEY(0x35, "Invalid key"),

    /** The maximum number of security access attempts has been exceeded. */
    EXCEEDED_NUMBER_OF_ATTEMPTS(0x36, "Exceeded number of attempts"),

    /** The required time delay before retrying has not yet elapsed. */
    REQUIRED_TIME_DELAY(0x37, "Required time delay not expired"),

    /** Upload/download request not accepted. */
    UPLOAD_DOWNLOAD_NOT_ACCEPTED(0x70, "Upload/download not accepted"),

    /** Data transfer has been suspended. */
    TRANSFER_SUSPENDED(0x71, "Transfer data suspended"),

    /** A general programming failure occurred. */
    GENERAL_PROGRAMMING_FAILURE(0x72, "General programming failure"),

    /** The ECU is not yet ready to send a final response; client should wait. */
    RESPONSE_PENDING(0x78, "Response pending"),

    /** The requested service is not supported in the current diagnostic session. */
    SERVICE_NOT_SUPPORTED_IN_SESSION(0x7F, "Service not supported in active session");

    private final int code;
    private final String description;

    NegativeResponseCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the numeric NRC value.
     *
     * @return the NRC byte value
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns a human-readable description of this NRC.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Looks up a {@code NegativeResponseCode} by its numeric value.
     *
     * @param code the NRC byte value
     * @return the matching enum constant, or {@code null} if no match is found
     */
    public static NegativeResponseCode fromCode(int code) {
        for (NegativeResponseCode nrc : values()) {
            if (nrc.code == code) {
                return nrc;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("NRC 0x%02X: %s", code, description);
    }
}
