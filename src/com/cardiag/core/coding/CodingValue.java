package com.cardiag.core.coding;

import java.util.Objects;

/**
 * Holds the original and desired values for a single {@link CodingParameter},
 * tracking whether a modification has been made.
 *
 * <p>This class is used by coding editors to track changes before committing
 * them to the ECU. It validates that any new value is within the parameter's
 * allowed-value set (if one is defined).</p>
 */
public class CodingValue {

    private final CodingParameter parameter;
    private final int originalValue;
    private int newValue;

    /**
     * Constructs a new {@code CodingValue} snapshot from the parameter's current value.
     *
     * @param parameter the coding parameter this value belongs to
     */
    public CodingValue(CodingParameter parameter) {
        this.parameter = Objects.requireNonNull(parameter, "parameter must not be null");
        this.originalValue = parameter.getCurrentValue();
        this.newValue = this.originalValue;
    }

    /**
     * Constructs a new {@code CodingValue} with explicit original and new values.
     *
     * @param parameter     the coding parameter this value belongs to
     * @param originalValue the original (baseline) value
     * @param newValue      the desired new value
     */
    public CodingValue(CodingParameter parameter, int originalValue, int newValue) {
        this.parameter = Objects.requireNonNull(parameter, "parameter must not be null");
        this.originalValue = originalValue;
        this.newValue = newValue;
    }

    /**
     * Returns the associated coding parameter.
     *
     * @return the parameter
     */
    public CodingParameter getParameter() {
        return parameter;
    }

    /**
     * Returns the original value that was read from the ECU.
     *
     * @return the original value
     */
    public int getOriginalValue() {
        return originalValue;
    }

    /**
     * Returns the current desired value.
     *
     * @return the new value
     */
    public int getNewValue() {
        return newValue;
    }

    /**
     * Returns {@code true} if the new value differs from the original.
     *
     * @return whether this value has been modified
     */
    public boolean isModified() {
        return newValue != originalValue;
    }

    /**
     * Sets the desired new value.
     *
     * @param value the new value to set
     * @throws IllegalArgumentException if the value exceeds the bit-field capacity
     */
    public void setValue(int value) {
        int maxValue = parameter.getMaxValue();
        if (value < 0 || value > maxValue) {
            throw new IllegalArgumentException(String.format(
                    "Value %d out of range for parameter '%s' [0..%d]",
                    value, parameter.getName(), maxValue));
        }
        this.newValue = value;
    }

    /**
     * Sets the desired new value with validation against the parameter's allowed values.
     *
     * @param value the new value to set
     * @throws IllegalArgumentException if the value is not in the allowed-values set
     */
    public void setValidatedValue(int value) {
        if (!parameter.isValueAllowed(value)) {
            throw new IllegalArgumentException(String.format(
                    "Value %d is not allowed for parameter '%s'. Allowed: %s",
                    value, parameter.getName(), parameter.getAllowedValues()));
        }
        setValue(value);
    }

    /**
     * Validates the current new value against the parameter's allowed values.
     *
     * @return {@code true} if the new value is valid
     */
    public boolean isValid() {
        return parameter.isValueAllowed(newValue);
    }

    /**
     * Resets the new value back to the original.
     */
    public void reset() {
        this.newValue = this.originalValue;
    }

    /**
     * Applies the new value to the underlying parameter's current value.
     */
    public void apply() {
        parameter.setCurrentValue(newValue);
    }

    @Override
    public String toString() {
        String label = parameter.getAllowedValues().getOrDefault(newValue, String.valueOf(newValue));
        return String.format("CodingValue{param='%s', original=%d, new=%d (%s), modified=%s}",
                parameter.getName(), originalValue, newValue, label, isModified());
    }
}
