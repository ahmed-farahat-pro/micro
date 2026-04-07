package com.cardiag.core.adaptation;

import com.cardiag.core.ecu.EcuConnection;
import com.cardiag.core.io.DataReader;
import com.cardiag.core.io.DataWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * Service for reading, writing, and resetting adaptation channel values on an ECU.
 *
 * <p>Adaptation values are accessed via UDS RoutineControl (0x31) or
 * ReadDataByIdentifier/WriteDataByIdentifier (0x22/0x2E) depending on the ECU
 * implementation. This service abstracts those details behind a simple API.</p>
 */
public class AdaptationService {

    /** Routine ID for reading adaptation values. */
    private static final int ROUTINE_READ_ADAPTATION = 0x0200;

    /** Routine ID for writing adaptation values. */
    private static final int ROUTINE_WRITE_ADAPTATION = 0x0201;

    /** Routine ID for resetting adaptation values. */
    private static final int ROUTINE_RESET_ADAPTATION = 0x0202;

    /**
     * Reads the current value of an adaptation channel from the ECU.
     *
     * @param connection the active ECU connection
     * @param channel    the adaptation channel to read
     * @return the channel with its current value updated
     * @throws IOException if communication fails or the ECU returns a negative response
     */
    public AdaptationChannel readAdaptation(EcuConnection connection, AdaptationChannel channel)
            throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        byte[] routineParam = new byte[2];
        DataWriter.writeShort(routineParam, 0, channel.getChannelId());

        byte[] result = connection.executeRoutine(ROUTINE_READ_ADAPTATION, routineParam);

        if (result.length >= 2) {
            int value = DataReader.readShort(result, 0);
            channel.setCurrentValue(value);
        } else if (result.length == 1) {
            int value = DataReader.readByte(result, 0);
            channel.setCurrentValue(value);
        }

        return channel;
    }

    /**
     * Writes a new value to an adaptation channel on the ECU.
     *
     * @param connection the active ECU connection (must have appropriate security access)
     * @param channel    the adaptation channel to write
     * @param value      the value to write
     * @throws IOException              if communication fails or the ECU returns a negative response
     * @throws IllegalArgumentException if the value is outside the channel's allowed range
     */
    public void writeAdaptation(EcuConnection connection, AdaptationChannel channel, int value)
            throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        if (!channel.isValueInRange(value)) {
            throw new IllegalArgumentException(String.format(
                    "Value %d is outside the allowed range [%d..%d] for channel '%s'",
                    value, channel.getMinValue(), channel.getMaxValue(), channel.getName()));
        }

        byte[] routineParam = new byte[4];
        DataWriter.writeShort(routineParam, 0, channel.getChannelId());
        DataWriter.writeShort(routineParam, 2, value);

        connection.executeRoutine(ROUTINE_WRITE_ADAPTATION, routineParam);
        channel.setCurrentValue(value);
    }

    /**
     * Resets an adaptation channel to its factory default value.
     *
     * @param connection the active ECU connection (must have appropriate security access)
     * @param channel    the adaptation channel to reset
     * @throws IOException if communication fails or the ECU returns a negative response
     */
    public void resetAdaptation(EcuConnection connection, AdaptationChannel channel)
            throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        byte[] routineParam = new byte[2];
        DataWriter.writeShort(routineParam, 0, channel.getChannelId());

        byte[] result = connection.executeRoutine(ROUTINE_RESET_ADAPTATION, routineParam);

        // Update the channel with the reset value returned by the ECU
        if (result.length >= 2) {
            int resetValue = DataReader.readShort(result, 0);
            channel.setCurrentValue(resetValue);
        }
    }
}
