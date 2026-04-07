package com.cardiag.core.security;

import com.cardiag.core.ecu.DiagnosticException;
import com.cardiag.core.ecu.EcuConnection;
import com.cardiag.core.io.DataReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Service for performing the UDS SecurityAccess (0x27) seed-key handshake.
 *
 * <p>The security access procedure follows these steps:
 * <ol>
 *   <li>Send a seed request with the target security level</li>
 *   <li>Receive the seed from the ECU</li>
 *   <li>Compute the key using the provided {@link SeedKeyAlgorithm}</li>
 *   <li>Send the computed key to the ECU</li>
 *   <li>Verify the ECU responds with a positive acknowledgement</li>
 * </ol>
 *
 * <p>If the ECU returns a zero seed, it indicates the requested level is already
 * unlocked and no key exchange is needed.</p>
 */
public class SecurityAccessService {

    /** UDS SecurityAccess service ID. */
    private static final int SID_SECURITY_ACCESS = 0x27;
    private static final int POSITIVE_RESPONSE_SID = SID_SECURITY_ACCESS + 0x40;

    /**
     * Performs a complete security access handshake on the given ECU connection.
     *
     * @param connection the active ECU connection
     * @param level      the target security level to unlock
     * @param algorithm  the seed-key algorithm for computing the response key
     * @throws IOException           if communication fails
     * @throws DiagnosticException   if the ECU rejects the seed request or key
     * @throws IllegalStateException if the connection is not established
     */
    public void performSecurityAccess(EcuConnection connection,
                                      SecurityLevel level,
                                      SeedKeyAlgorithm algorithm) throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");

        if (level == SecurityLevel.LOCKED) {
            throw new IllegalArgumentException("Cannot perform security access with LOCKED level");
        }

        // Step 1: Send seed request
        byte[] seedRequest = new byte[]{
                (byte) SID_SECURITY_ACCESS,
                (byte) level.getAccessLevel()
        };

        byte[] seedResponse = connection.sendAndValidate(seedRequest, SID_SECURITY_ACCESS);

        // Validate response structure
        if (seedResponse.length < 2) {
            throw new DiagnosticException("Seed response too short: " + seedResponse.length + " bytes");
        }

        // Verify the sub-function echo
        int echoedSubFunction = DataReader.readByte(seedResponse, 1);
        if (echoedSubFunction != level.getAccessLevel()) {
            throw new DiagnosticException(String.format(
                    "Seed response sub-function mismatch: expected 0x%02X, got 0x%02X",
                    level.getAccessLevel(), echoedSubFunction));
        }

        // Step 2: Extract seed bytes
        byte[] seed = Arrays.copyOfRange(seedResponse, 2, seedResponse.length);

        // Check for zero seed (already unlocked)
        boolean alreadyUnlocked = true;
        for (byte b : seed) {
            if (b != 0) {
                alreadyUnlocked = false;
                break;
            }
        }

        if (alreadyUnlocked) {
            return; // ECU is already unlocked at this level
        }

        // Step 3: Compute key
        byte[] key = algorithm.calculateKey(seed, level.getAccessLevel());

        if (key == null || key.length == 0) {
            throw new DiagnosticException("Algorithm '" + algorithm.getName()
                    + "' returned null or empty key");
        }

        // Step 4: Send key
        byte[] keyRequest = new byte[2 + key.length];
        keyRequest[0] = (byte) SID_SECURITY_ACCESS;
        keyRequest[1] = (byte) level.getKeySendByte();
        System.arraycopy(key, 0, keyRequest, 2, key.length);

        byte[] keyResponse = connection.sendAndValidate(keyRequest, SID_SECURITY_ACCESS);

        // Step 5: Verify positive response
        if (keyResponse.length < 2) {
            throw new DiagnosticException("Key response too short: " + keyResponse.length + " bytes");
        }

        int keyEchoedSub = DataReader.readByte(keyResponse, 1);
        if (keyEchoedSub != level.getKeySendByte()) {
            throw new DiagnosticException(String.format(
                    "Key response sub-function mismatch: expected 0x%02X, got 0x%02X",
                    level.getKeySendByte(), keyEchoedSub));
        }
    }
}
