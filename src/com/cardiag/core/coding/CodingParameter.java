package com.cardiag.core.coding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes a single coding field within an ECU's coding block.
 *
 * <p>A coding parameter maps to a specific bit range within the raw coding byte array.
 * It defines the field's position, length, allowed values, and holds the current decoded
 * value.</p>
 *
 * <p>Example: A parameter "Daytime Running Lights" at byte 0, bit offset 2, length 1
 * with allowed values {0="Off", 1="On"}.</p>
 */
public class CodingParameter {

    private final String name;
    private final int bytePosition;
    private final int bitOffset;
    private final int bitLength;
    private final Map<Integer, String> allowedValues;
    private final String description;
    private int currentValue;

    /**
     * Constructs a new {@code CodingParameter}.
     *
     * @param name          the human-readable parameter name
     * @param bytePosition  the zero-based byte position in the coding block
     * @param bitOffset     the bit offset within the byte (0 = LSB)
     * @param bitLength     the number of bits this parameter occupies
     * @param allowedValues a map of valid integer values to their descriptions (may be empty)
     * @param description   a human-readable description of the parameter's function
     */
    public CodingParameter(String name, int bytePosition, int bitOffset, int bitLength,
                           Map<Integer, String> allowedValues, String description) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        if (bytePosition < 0) {
            throw new IllegalArgumentException("bytePosition must be non-negative");
        }
        if (bitOffset < 0 || bitOffset > 7) {
            throw new IllegalArgumentException("bitOffset must be between 0 and 7");
        }
        if (bitLength < 1 || bitLength > 32) {
            throw new IllegalArgumentException("bitLength must be between 1 and 32");
        }
        this.bytePosition = bytePosition;
        this.bitOffset = bitOffset;
        this.bitLength = bitLength;
        this.allowedValues = allowedValues != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(allowedValues))
                : Collections.emptyMap();
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.currentValue = 0;
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /** Returns the human-readable name of this coding parameter. */
    public String getName() {
        return name;
    }

    /** Returns the zero-based byte position within the coding block. */
    public int getBytePosition() {
        return bytePosition;
    }

    /** Returns the bit offset within the byte (0 = LSB). */
    public int getBitOffset() {
        return bitOffset;
    }

    /** Returns the number of bits this parameter occupies. */
    public int getBitLength() {
        return bitLength;
    }

    /** Returns an unmodifiable map of allowed values to their descriptions. */
    public Map<Integer, String> getAllowedValues() {
        return allowedValues;
    }

    /** Returns the human-readable description of this parameter. */
    public String getDescription() {
        return description;
    }

    /** Returns the current decoded value of this parameter. */
    public int getCurrentValue() {
        return currentValue;
    }

    // ── Setters ─────────────────────────────────────────────────────────

    /**
     * Sets the current value of this parameter.
     *
     * @param currentValue the value to set
     * @throws IllegalArgumentException if the value exceeds the bit-field capacity
     */
    public void setCurrentValue(int currentValue) {
        int maxValue = (1 << bitLength) - 1;
        if (currentValue < 0 || currentValue > maxValue) {
            throw new IllegalArgumentException(String.format(
                    "Value %d out of range for %d-bit field [0..%d]", currentValue, bitLength, maxValue));
        }
        this.currentValue = currentValue;
    }

    /**
     * Returns the maximum value that fits in this parameter's bit field.
     *
     * @return the maximum representable value
     */
    public int getMaxValue() {
        return (1 << bitLength) - 1;
    }

    /**
     * Checks whether the given value is in the allowed-values map.
     * If the allowed-values map is empty, any value within range is considered valid.
     *
     * @param value the value to check
     * @return {@code true} if the value is allowed
     */
    public boolean isValueAllowed(int value) {
        if (allowedValues.isEmpty()) {
            return value >= 0 && value <= getMaxValue();
        }
        return allowedValues.containsKey(value);
    }

    /**
     * Returns the global bit offset (byte position * 8 + bit offset) for use with
     * {@link com.cardiag.core.io.DataReader#extractBits} and
     * {@link com.cardiag.core.io.DataWriter#setBits}.
     *
     * @return the absolute bit offset from the start of the coding block
     */
    public int getAbsoluteBitOffset() {
        return bytePosition * 8 + bitOffset;
    }

    @Override
    public String toString() {
        String valueLabel = allowedValues.getOrDefault(currentValue, String.valueOf(currentValue));
        return String.format("CodingParameter{name='%s', byte=%d, bit=%d:%d, value=%d (%s), desc='%s'}",
                name, bytePosition, bitOffset, bitLength, currentValue, valueLabel, description);
    }
}
