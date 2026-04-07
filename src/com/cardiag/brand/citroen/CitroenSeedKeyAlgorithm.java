package com.cardiag.brand.citroen;

import com.cardiag.core.security.SeedKeyAlgorithm;

/**
 * PSA-style seed/key security algorithm for Citroen vehicles.
 *
 * <p>Implements the proprietary seed-to-key transformation used by PSA/Stellantis
 * ECUs during UDS SecurityAccess (0x27) handshakes. The algorithm uses a combination
 * of XOR operations, bit shifts, and magic constants that vary by security level.</p>
 */
public class CitroenSeedKeyAlgorithm implements SeedKeyAlgorithm {

    private static final String ALGORITHM_NAME = "PSA-SeedKey";

    // PSA magic constants per security level
    private static final long MAGIC_LEVEL1 = 0xC541A9E3L;
    private static final long MAGIC_LEVEL2 = 0xB7A3C5D1L;
    private static final long MAGIC_LEVEL3 = 0xD8E4F2A6L;
    private static final long MAGIC_ENGINEERING = 0xF1E2D3C4L;

    @Override
    public byte[] calculateKey(byte[] seed, int securityLevel) {
        if (seed == null || seed.length == 0) {
            throw new IllegalArgumentException("Seed must not be null or empty");
        }
        if (seed.length != 4 && seed.length != 2) {
            throw new IllegalArgumentException(
                    "PSA seed must be 2 or 4 bytes, got " + seed.length);
        }

        long magic = selectMagic(securityLevel);

        if (seed.length == 4) {
            return computeKey4(seed, magic);
        } else {
            return computeKey2(seed, magic);
        }
    }

    @Override
    public String getName() {
        return ALGORITHM_NAME;
    }

    /**
     * Computes the key for a 4-byte seed using the PSA algorithm.
     * Steps: convert seed to 32-bit value, then apply iterative XOR/shift
     * transformations with the magic constant.
     */
    private byte[] computeKey4(byte[] seed, long magic) {
        long seedValue = ((seed[0] & 0xFFL) << 24)
                       | ((seed[1] & 0xFFL) << 16)
                       | ((seed[2] & 0xFFL) << 8)
                       | (seed[3] & 0xFFL);

        long key = seedValue;

        // PSA transformation: 5 rounds of XOR, rotate, and mix
        for (int round = 0; round < 5; round++) {
            // XOR with magic
            key ^= magic;

            // Rotate left by 3
            key = ((key << 3) | (key >>> 29)) & 0xFFFFFFFFL;

            // Mix: add the inverted seed nibbles
            long mix = (~seedValue) & 0xFFFFFFFFL;
            key = (key + mix) & 0xFFFFFFFFL;

            // Shift and XOR feedback
            key ^= (key >>> 16);
            key = ((key << 7) | (key >>> 25)) & 0xFFFFFFFFL;
        }

        // Final XOR with complement of magic
        key ^= (~magic) & 0xFFFFFFFFL;

        return new byte[]{
                (byte) ((key >> 24) & 0xFF),
                (byte) ((key >> 16) & 0xFF),
                (byte) ((key >> 8) & 0xFF),
                (byte) (key & 0xFF)
        };
    }

    /**
     * Computes the key for a 2-byte seed (legacy PSA ECUs).
     */
    private byte[] computeKey2(byte[] seed, long magic) {
        int seedValue = ((seed[0] & 0xFF) << 8) | (seed[1] & 0xFF);

        int key = seedValue;
        int magicLow = (int) (magic & 0xFFFF);

        for (int round = 0; round < 4; round++) {
            key ^= magicLow;
            key = ((key << 5) | (key >>> 11)) & 0xFFFF;
            key = (key + (~seedValue & 0xFFFF)) & 0xFFFF;
        }

        key ^= (~magicLow) & 0xFFFF;

        return new byte[]{
                (byte) ((key >> 8) & 0xFF),
                (byte) (key & 0xFF)
        };
    }

    /**
     * Selects the magic constant based on the UDS security level.
     */
    private long selectMagic(int securityLevel) {
        switch (securityLevel) {
            case 0x01: return MAGIC_LEVEL1;
            case 0x03: return MAGIC_LEVEL2;
            case 0x05: return MAGIC_LEVEL3;
            case 0x61: return MAGIC_ENGINEERING;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported security level: 0x%02X", securityLevel));
        }
    }
}
