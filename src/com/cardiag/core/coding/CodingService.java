package com.cardiag.core.coding;

import com.cardiag.core.ecu.EcuConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Service class for reading and writing ECU coding data.
 *
 * <p>Coding data is read/written via UDS ReadDataByIdentifier (0x22) and
 * WriteDataByIdentifier (0x2E) services using a coding-specific DID.</p>
 */
public class CodingService {

    /**
     * Reads the raw coding bytes from the ECU and decodes them into the given
     * {@link CodingMap}.
     *
     * @param connection the active ECU connection
     * @param codingDid  the Data Identifier for the coding block
     * @param codingMap  the coding map whose parameters will be populated
     * @return the populated coding map (same instance)
     * @throws IOException if communication fails or the ECU returns a negative response
     */
    public CodingMap readCoding(EcuConnection connection, int codingDid, CodingMap codingMap) throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(codingMap, "codingMap must not be null");

        byte[] rawCoding = connection.readData(codingDid);
        codingMap.decode(rawCoding);
        return codingMap;
    }

    /**
     * Encodes the coding map and writes the resulting bytes to the ECU.
     *
     * @param connection the active ECU connection (must have appropriate security access)
     * @param codingDid  the Data Identifier for the coding block
     * @param codingMap  the coding map containing the values to write
     * @throws IOException if communication fails or the ECU returns a negative response
     */
    public void writeCoding(EcuConnection connection, int codingDid, CodingMap codingMap) throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(codingMap, "codingMap must not be null");

        byte[] rawCoding = codingMap.encode();
        connection.writeData(codingDid, rawCoding);
    }

    /**
     * Compares two coding maps and returns a list of {@link CodingValue} objects
     * representing the differences.
     *
     * <p>Only parameters whose values differ between the original and modified maps
     * are included in the result.</p>
     *
     * @param original the original (baseline) coding map
     * @param modified the modified coding map
     * @return a list of {@code CodingValue} entries for each changed parameter
     * @throws IllegalArgumentException if the maps contain different parameter sets
     */
    public List<CodingValue> compareCoding(CodingMap original, CodingMap modified) {
        Objects.requireNonNull(original, "original must not be null");
        Objects.requireNonNull(modified, "modified must not be null");

        List<CodingValue> changes = new ArrayList<>();
        Collection<CodingParameter> originalParams = original.getAllParameters();

        for (CodingParameter origParam : originalParams) {
            CodingParameter modParam = modified.getParameter(origParam.getName());
            if (modParam == null) {
                throw new IllegalArgumentException(
                        "Modified map is missing parameter: " + origParam.getName());
            }

            if (origParam.getCurrentValue() != modParam.getCurrentValue()) {
                CodingValue change = new CodingValue(
                        modParam,
                        origParam.getCurrentValue(),
                        modParam.getCurrentValue()
                );
                changes.add(change);
            }
        }

        return changes;
    }
}
