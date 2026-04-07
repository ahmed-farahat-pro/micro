package com.cardiag.core.io;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Utility class providing static methods for writing primitive values and
 * bit-fields into raw byte arrays.
 *
 * <p>All multi-byte writes use <b>big-endian</b> (network) byte order, which is
 * the standard for UDS / ISO 14229 diagnostic data.
 */
public final class DataWriter {

    private DataWriter() {
        // utility class
    }

    /**
     * Writes a single byte value into the array at the given offset.
     *
     * @param data   the target byte array
     * @param offset the zero-based offset
     * @param value  the value to write (only the low 8 bits are used)
     */
    public static void writeByte(byte[] data, int offset, int value) {
        Objects.requireNonNull(data, "data must not be null");
        data[offset] = (byte) (value & 0xFF);
    }

    /**
     * Writes a 16-bit big-endian value into the array at the given offset.
     *
     * @param data   the target byte array
     * @param offset the zero-based offset of the high byte
     * @param value  the value to write (only the low 16 bits are used)
     */
    public static void writeShort(byte[] data, int offset, int value) {
        Objects.requireNonNull(data, "data must not be null");
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    /**
     * Writes a 32-bit big-endian value into the array at the given offset.
     *
     * @param data   the target byte array
     * @param offset the zero-based offset of the most-significant byte
     * @param value  the value to write
     */
    public static void writeInt(byte[] data, int offset, int value) {
        Objects.requireNonNull(data, "data must not be null");
        data[offset] = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    /**
     * Sets an arbitrary bit-field within a byte array.
     *
     * @param data      the target byte array
     * @param bitOffset the zero-based offset in <b>bits</b> from the start of the array
     * @param bitLength the number of bits to write (1-32)
     * @param value     the value to write, right-justified
     */
    public static void setBits(byte[] data, int bitOffset, int bitLength, int value) {
        Objects.requireNonNull(data, "data must not be null");
        if (bitLength < 1 || bitLength > 32) {
            throw new IllegalArgumentException("bitLength must be between 1 and 32, got " + bitLength);
        }

        for (int i = bitLength - 1; i >= 0; i--) {
            int totalBit = bitOffset + (bitLength - 1 - i);
            int byteIndex = totalBit / 8;
            int bitIndex = 7 - (totalBit % 8); // MSB-first within each byte
            int bit = (value >> i) & 1;
            if (bit == 1) {
                data[byteIndex] |= (byte) (1 << bitIndex);
            } else {
                data[byteIndex] &= (byte) ~(1 << bitIndex);
            }
        }
    }

    /**
     * Converts an integer to a packed BCD (Binary-Coded Decimal) byte sequence.
     *
     * <p>For example, {@code intToBcd(1234, 2)} produces bytes {@code [0x12, 0x34]}.
     *
     * @param value  the non-negative integer value
     * @param length the number of BCD bytes to produce
     * @return the packed BCD byte array
     * @throws IllegalArgumentException if the value is negative or cannot fit in the given length
     */
    public static byte[] intToBcd(int value, int length) {
        if (value < 0) {
            throw new IllegalArgumentException("BCD value must be non-negative, got " + value);
        }

        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            int low = value % 10;
            value /= 10;
            int high = value % 10;
            value /= 10;
            result[i] = (byte) ((high << 4) | low);
        }

        if (value != 0) {
            throw new IllegalArgumentException(
                    "Value too large for " + length + " BCD bytes");
        }
        return result;
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param hex the hex string (even length, e.g. "0A1BFF")
     * @return the decoded byte array
     * @throws IllegalArgumentException if the string has an odd length or contains invalid characters
     */
    public static byte[] hexToBytes(String hex) {
        Objects.requireNonNull(hex, "hex must not be null");
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length, got " + hex.length());
        }

        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException(
                        "Invalid hex character at position " + (i * 2) + " in: " + hex);
            }
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    /**
     * Converts an ASCII string to a byte array using US-ASCII encoding.
     *
     * @param text the ASCII string
     * @return the encoded byte array
     */
    public static byte[] asciiToBytes(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}
