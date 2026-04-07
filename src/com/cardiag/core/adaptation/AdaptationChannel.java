package com.cardiag.core.adaptation;

import java.util.Objects;

/**
 * Model representing a single adaptation channel on an ECU.
 *
 * <p>Adaptation channels allow fine-tuning of ECU parameters such as idle speed,
 * injection timing, throttle position offsets, and other calibration values. Each
 * channel has a defined value range and unit.</p>
 */
public class AdaptationChannel {

    private final int channelId;
    private final String name;
    private final int minValue;
    private final int maxValue;
    private final String unit;
    private final String description;
    private int currentValue;

    /**
     * Constructs a new {@code AdaptationChannel}.
     *
     * @param channelId   the numeric channel identifier
     * @param name        a human-readable name for this channel
     * @param minValue    the minimum allowed value
     * @param maxValue    the maximum allowed value
     * @param unit        the unit of measurement (e.g. "rpm", "ms", "%")
     * @param description a description of the channel's function
     */
    public AdaptationChannel(int channelId, String name, int minValue, int maxValue,
                             String unit, String description) {
        this.channelId = channelId;
        this.name = Objects.requireNonNull(name, "name must not be null");
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue must not exceed maxValue");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.currentValue = minValue;
    }

    // ── Getters ─────────────────────────────────────────────────────────

    /** Returns the numeric channel identifier. */
    public int getChannelId() {
        return channelId;
    }

    /** Returns the human-readable name of this channel. */
    public String getName() {
        return name;
    }

    /** Returns the minimum allowed value. */
    public int getMinValue() {
        return minValue;
    }

    /** Returns the maximum allowed value. */
    public int getMaxValue() {
        return maxValue;
    }

    /** Returns the unit of measurement. */
    public String getUnit() {
        return unit;
    }

    /** Returns the description of this channel's function. */
    public String getDescription() {
        return description;
    }

    /** Returns the current value of this channel. */
    public int getCurrentValue() {
        return currentValue;
    }

    // ── Setters ─────────────────────────────────────────────────────────

    /**
     * Sets the current value of this channel.
     *
     * @param value the value to set
     * @throws IllegalArgumentException if the value is outside the allowed range
     */
    public void setCurrentValue(int value) {
        if (value < minValue || value > maxValue) {
            throw new IllegalArgumentException(String.format(
                    "Value %d out of range [%d..%d] for channel '%s'",
                    value, minValue, maxValue, name));
        }
        this.currentValue = value;
    }

    /**
     * Returns {@code true} if the given value is within the allowed range.
     *
     * @param value the value to check
     * @return whether the value is in range
     */
    public boolean isValueInRange(int value) {
        return value >= minValue && value <= maxValue;
    }

    @Override
    public String toString() {
        return String.format("AdaptationChannel{id=%d, name='%s', value=%d %s, range=[%d..%d], desc='%s'}",
                channelId, name, currentValue, unit, minValue, maxValue, description);
    }
}
