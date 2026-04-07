package com.cardiag.brand.citroen;

import com.cardiag.core.ecu.EcuAddress;

import java.util.*;

/**
 * Maps Citroen ECU names to their CAN bus addresses.
 *
 * <p>Each entry defines the physical (request) and response CAN IDs used
 * by PSA/Stellantis diagnostic protocols to communicate with a specific ECU.</p>
 */
public class CitroenEcuMap {

    /**
     * Internal record holding the request and response CAN IDs for an ECU.
     */
    public static final class EcuEntry {
        private final String name;
        private final int requestId;
        private final int responseId;

        public EcuEntry(String name, int requestId, int responseId) {
            this.name = name;
            this.requestId = requestId;
            this.responseId = responseId;
        }

        public String getName() {
            return name;
        }

        public int getRequestId() {
            return requestId;
        }

        public int getResponseId() {
            return responseId;
        }

        /**
         * Converts this entry to an {@link EcuAddress}.
         *
         * @return a new EcuAddress with the PSA functional broadcast ID
         */
        public EcuAddress toEcuAddress() {
            return new EcuAddress(requestId, 0x7DF, responseId, name);
        }

        @Override
        public String toString() {
            return String.format("%s (req=0x%03X, resp=0x%03X)", name, requestId, responseId);
        }
    }

    private static final Map<String, EcuEntry> ECU_MAP;

    static {
        Map<String, EcuEntry> map = new LinkedHashMap<>();
        map.put("ECM",   new EcuEntry("Engine Control Module",                 0x7E0, 0x7E8));
        map.put("BSI",   new EcuEntry("Boitier de Servitude Intelligent",      0x764, 0x664));
        map.put("BEM",   new EcuEntry("Built-in Equipment Module",             0x765, 0x665));
        map.put("CMB",   new EcuEntry("Combined Instrument Cluster",           0x766, 0x666));
        map.put("SMEG",  new EcuEntry("SMEG/NAC Infotainment",                 0x767, 0x667));
        map.put("CLIM",  new EcuEntry("Climate Control",                       0x768, 0x668));
        map.put("ABS",   new EcuEntry("ABS/ESP Control Unit",                  0x760, 0x660));
        map.put("EPS",   new EcuEntry("Electric Power Steering",               0x762, 0x662));
        map.put("AMVAR", new EcuEntry("AMVAR Suspension",                      0x769, 0x669));
        map.put("DAEP",  new EcuEntry("Directional Adaptive Headlights",       0x770, 0x670));
        ECU_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the CAN address entry for the given ECU short name.
     *
     * @param ecuName the ECU identifier (e.g. "ECM", "BSI", "ABS")
     * @return the corresponding {@link EcuEntry}, or {@code null} if not found
     */
    public static EcuEntry getAddress(String ecuName) {
        return ECU_MAP.get(ecuName);
    }

    /**
     * Returns an unmodifiable map of all Citroen ECU entries.
     *
     * @return a map of ECU short name to {@link EcuEntry}
     */
    public static Map<String, EcuEntry> getAllEcus() {
        return ECU_MAP;
    }
}
