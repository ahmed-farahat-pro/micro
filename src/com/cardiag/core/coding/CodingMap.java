package com.cardiag.core.coding;

import com.cardiag.core.io.DataReader;
import com.cardiag.core.io.DataWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of {@link CodingParameter} entries that together describe the full
 * coding configuration of a single ECU coding block.
 *
 * <p>Provides methods to encode the current parameter values into a raw byte array
 * and to decode a raw byte array back into individual parameter values.</p>
 */
public class CodingMap {

    private final Map<String, CodingParameter> parameters;

    /**
     * Constructs an empty {@code CodingMap}.
     */
    public CodingMap() {
        this.parameters = new LinkedHashMap<>();
    }

    /**
     * Adds a coding parameter to this map.
     *
     * @param parameter the parameter to add
     * @throws IllegalArgumentException if a parameter with the same name already exists
     */
    public void addParameter(CodingParameter parameter) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        if (parameters.containsKey(parameter.getName())) {
            throw new IllegalArgumentException(
                    "Duplicate coding parameter name: " + parameter.getName());
        }
        parameters.put(parameter.getName(), parameter);
    }

    /**
     * Returns the coding parameter with the given name.
     *
     * @param name the parameter name
     * @return the {@link CodingParameter}, or {@code null} if not found
     */
    public CodingParameter getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Returns an unmodifiable collection of all coding parameters in this map.
     *
     * @return all parameters
     */
    public Collection<CodingParameter> getAllParameters() {
        return Collections.unmodifiableCollection(parameters.values());
    }

    /**
     * Returns the number of parameters in this map.
     *
     * @return the parameter count
     */
    public int size() {
        return parameters.size();
    }

    /**
     * Returns {@code true} if this map contains a parameter with the given name.
     *
     * @param name the parameter name
     * @return {@code true} if present
     */
    public boolean containsParameter(String name) {
        return parameters.containsKey(name);
    }

    /**
     * Encodes all parameter current values into a raw coding byte array.
     *
     * <p>The returned array is sized to accommodate all parameters based on their
     * byte positions and bit lengths.</p>
     *
     * @return the encoded coding bytes
     */
    public byte[] encode() {
        int byteCount = calculateByteCount();
        byte[] data = new byte[byteCount];

        for (CodingParameter param : parameters.values()) {
            int absoluteBitOffset = param.getAbsoluteBitOffset();
            DataWriter.setBits(data, absoluteBitOffset, param.getBitLength(), param.getCurrentValue());
        }

        return data;
    }

    /**
     * Decodes a raw coding byte array and populates each parameter's current value.
     *
     * @param data the raw coding bytes
     * @throws IllegalArgumentException if the data array is too short for the defined parameters
     */
    public void decode(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");

        int required = calculateByteCount();
        if (data.length < required) {
            throw new IllegalArgumentException(String.format(
                    "Coding data too short: need %d bytes, got %d", required, data.length));
        }

        for (CodingParameter param : parameters.values()) {
            int absoluteBitOffset = param.getAbsoluteBitOffset();
            int value = DataReader.extractBits(data, absoluteBitOffset, param.getBitLength());
            param.setCurrentValue(value);
        }
    }

    /**
     * Creates a deep copy of this {@code CodingMap} with all current parameter values.
     *
     * @return a new independent copy
     */
    public CodingMap copy() {
        CodingMap copy = new CodingMap();
        for (CodingParameter param : parameters.values()) {
            CodingParameter paramCopy = new CodingParameter(
                    param.getName(),
                    param.getBytePosition(),
                    param.getBitOffset(),
                    param.getBitLength(),
                    param.getAllowedValues(),
                    param.getDescription()
            );
            paramCopy.setCurrentValue(param.getCurrentValue());
            copy.addParameter(paramCopy);
        }
        return copy;
    }

    /**
     * Calculates the minimum number of bytes needed to contain all parameters.
     */
    private int calculateByteCount() {
        int maxBit = 0;
        for (CodingParameter param : parameters.values()) {
            int endBit = param.getAbsoluteBitOffset() + param.getBitLength();
            if (endBit > maxBit) {
                maxBit = endBit;
            }
        }
        return (maxBit + 7) / 8;
    }

    @Override
    public String toString() {
        return String.format("CodingMap{parameters=%d, byteSize=%d}", parameters.size(), calculateByteCount());
    }
}
