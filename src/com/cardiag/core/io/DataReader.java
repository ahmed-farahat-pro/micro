package com.cardiag.core.io;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Utility class providing static methods for reading primitive values and
 * bit-fields from raw byte arrays.
 *
 * <p>All multi-byte reads use <b>big-endian</b> (network) byte order, which is
 * the standard for UDS / ISO 14229 diagnostic data.
 */
public final class DataReader {

    private DataReader() {
        // utility class
    }

    /**
     * Reads an unsigned byte from the array at the given offset.
     *
     * @param data   the source byte array
     * @param offset the zero-based offset
     * @return the unsigned byte value (0-255)
     * @throws IndexOutOfBoundsException if offset is out of range
     */
    public static int readByte(byte[] data, int offset) {
        Objects.requireNonNull(data, "data must not be null");
        return data[offset] & 0xFF;
    }

    /**
     * Reads an unsigned 16-bit big-endian value from the array at the given offset.
     *
     * @param data   the source byte array
     * @param offset the zero-based offset of the high byte
     * @return the unsigned short value (0-65535)
     * @throws IndexOutOfBoundsException if the read extends beyond the array
     */
    public static int readShort(byte[] data, int offset) {
        Objects.requireNonNull(data, "data must not be null");
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    /**
     * Reads a 32-bit big-endian value from the array at the given offset.
     *
     * @param data   the source byte array
     * @param offset the zero-based offset of the most-significant byte
     * @return the int value
     * @throws IndexOutOfBoundsException if the read extends beyond the array
     */
    public static int readInt(byte[] data, int offset) {
        Objects.requireNonNull(data, "data must not be null");
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    /**
     * Extracts an arbitrary bit-field from a byte array.
     *
     * @param data      the source byte array
     * @param bitOffset the zero-based offset in <b>bits</b> from the start of the array
     * @param bitLength the number of bits to extract (1-32)
     * @return the extracted value as an int with the field right-justified
     * @throws IllegalArgumentException if bitLength is outside 1-32
     */
    public static int extractBits(byte[] data, int bitOffset, int bitLength) {
        Objects.requireNonNull(data, "data must not be null");
        if (bitLength < 1 || bitLength > 32) {
            throw new IllegalArgumentException("bitLength must be between 1 and 32, got " + bitLength);
        }

        int result = 0;
        for (int i = 0; i < bitLength; i++) {
            int totalBit = bitOffset + i;
            int byteIndex = totalBit / 8;
            int bitIndex = 7 - (totalBit % 8); // MSB-first within each byte
            int bit = (data[byteIndex] >> bitIndex) & 1;
            result = (result << 1) | bit;
        }
        return result;
    }

    /**
     * Converts a packed BCD (Binary-Coded Decimal) byte sequence to an integer.
     *
     * <p>For example, bytes {@code [0x12, 0x34]} yield {@code 1234}.
     *
     * @param data   the source byte array
     * @param offset the zero-based offset
     * @param length the number of BCD bytes to convert
     * @return the decoded integer value
     * @throws IllegalArgumentException if any nibble is not a valid BCD digit
     */
    public static int bcdToInt(byte[] data, int offset, int length) {
        Objects.requireNonNull(data, "data must not be null");
        int result = 0;
        for (int i = 0; i < length; i++) {
            int b = data[offset + i] & 0xFF;
            int high = b >> 4;
            int low = b & 0x0F;
            if (high > 9 || low > 9) {
                throw new IllegalArgumentException(
                        String.format("Invalid BCD byte 0x%02X at offset %d", b, offset + i));
            }
            result = result * 100 + high * 10 + low;
        }
        return result;
    }

    /**
     * Converts a byte array segment to a hexadecimal string.
     *
     * @param data   the source byte array
     * @param offset the zero-based offset
     * @param length the number of bytes to convert
     * @return the uppercase hex string (e.g. "0A1BFF")
     */
    public static String bytesToHex(byte[] data, int offset, int length) {
        Objects.requireNonNull(data, "data must not be null");
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", data[offset + i] & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Convenience overload that converts the entire byte array to hex.
     *
     * @param data the source byte array
     * @return the uppercase hex string
     */
    public static String bytesToHex(byte[] data) {
        if (data == null) return "";
        return bytesToHex(data, 0, data.length);
    }

    /**
     * Parses an ASCII string from the byte array at the given offset/length,
     * trimming trailing null characters and whitespace.
     *
     * @param data   the source byte array
     * @param offset the zero-based offset
     * @param length the number of bytes to read
     * @return the trimmed ASCII string
     */
    public static String parseAscii(byte[] data, int offset, int length) {
        Objects.requireNonNull(data, "data must not be null");
        // Find the effective end (ignore trailing nulls)
        int end = offset + length;
        while (end > offset && data[end - 1] == 0x00) {
            end--;
        }
        return new String(data, offset, end - offset, StandardCharsets.US_ASCII).trim();
    }
}
