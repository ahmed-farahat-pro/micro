package com.cardiag.core.security;

/**
 * Strategy interface for computing the security-access key from a seed received
 * from an ECU during the UDS SecurityAccess (0x27) handshake.
 *
 * <p>Implementations are ECU-family-specific and typically involve proprietary
 * cryptographic or obfuscation algorithms.
 */
public interface SeedKeyAlgorithm {

    /**
     * Computes the authentication key from the given seed bytes.
     *
     * @param seed          the seed bytes received from the ECU
     * @param securityLevel the access-level byte for which the key is being computed
     * @return the computed key bytes to send back to the ECU
     * @throws IllegalArgumentException if the seed is null, empty, or otherwise invalid
     */
    byte[] calculateKey(byte[] seed, int securityLevel);

    /**
     * Returns a human-readable name identifying this algorithm (e.g. "VAG-SA2" or "BMW-ISN").
     *
     * @return the algorithm name
     */
    String getName();
}
