package com.cardiag.brand.bmw;

import com.cardiag.core.security.SeedKeyAlgorithm;

/**
 * BMW-specific seed/key algorithm for UDS SecurityAccess (0x27).
 * Uses XOR and bit-rotation operations typical of BMW ECU authentication.
 */
public class BmwSeedKeyAlgorithm implements SeedKeyAlgorithm {

    private static final long BMW_SECRET_KEY_L1 = 0x3F4A5B6CL;
    private static final long BMW_SECRET_KEY_L3 = 0x7E2D1A09L;
    private static final long BMW_SECRET_KEY_ENG = 0xA5B4C3D2L;

    @Override
    public byte[] calculateKey(byte[] seed, int securityLevel) {
        if (seed == null || seed.length == 0) {
            throw new IllegalArgumentException("Seed must not be null or empty");
        }

        long secretKey = selectSecret(securityLevel);
        long seedValue = bytesToLong(seed);

        // Step 1: XOR seed with level-specific secret
        long result = seedValue ^ secretKey;

        // Step 2: Rotate left by (securityLevel & 0x0F) bits
        int rotation = securityLevel & 0x0F;
        result = rotateLeft(result, rotation, seed.length * 8);

        // Step 3: Byte-wise complement and XOR with shifted seed
        result = (result ^ (seedValue >>> 4)) & mask(seed.length);

        // Step 4: Final XOR pass with alternating nibble pattern
        result ^= 0xA5A5A5A5L & mask(seed.length);

        return longToBytes(result, seed.length);
    }

    @Override
    public String getName() {
        return "BMW-SA2";
    }

    private long selectSecret(int securityLevel) {
        if (securityLevel == 0x61 || securityLevel == 0x62) {
            return BMW_SECRET_KEY_ENG;
        } else if (securityLevel == 0x05 || securityLevel == 0x06) {
            return BMW_SECRET_KEY_L3;
        }
        return BMW_SECRET_KEY_L1;
    }

    private static long rotateLeft(long value, int count, int bitWidth) {
        long mask = (1L << bitWidth) - 1;
        value &= mask;
        return ((value << count) | (value >>> (bitWidth - count))) & mask;
    }

    private static long mask(int byteCount) {
        if (byteCount >= 8) {
            return -1L;
        }
        return (1L << (byteCount * 8)) - 1;
    }

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        return value;
    }

    private static byte[] longToBytes(long value, int length) {
        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return result;
    }
}
