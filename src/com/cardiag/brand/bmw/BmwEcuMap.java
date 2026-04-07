package com.cardiag.brand.bmw;

import com.cardiag.core.ecu.EcuAddress;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Static mapping of all BMW ECU names to their CAN bus addresses.
 * Each entry contains the physical request ID and response ID used
 * for diagnostic communication.
 */
public final class BmwEcuMap {

    // ── ECU address constants ───────────────────────────────────────────

    public static final EcuAddress DME_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x7E0, 0x7E8, "DME");

    public static final EcuAddress EGS_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6F1, 0x6F9, "EGS");

    public static final EcuAddress CAS_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x640, 0x648, "CAS");

    public static final EcuAddress FRM_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6C0, 0x6C8, "FRM");

    public static final EcuAddress KOMBI_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x720, 0x728, "KOMBI");

    public static final EcuAddress CIC_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6F1, 0x6F9, "CIC");

    public static final EcuAddress IHKA_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6C1, 0x6C9, "IHKA");

    public static final EcuAddress DSC_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6C2, 0x6CA, "DSC");

    public static final EcuAddress ELV_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6C3, 0x6CB, "ELV");

    public static final EcuAddress EHC_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6C4, 0x6CC, "EHC");

    public static final EcuAddress SZL_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6C5, 0x6CD, "SZL");

    public static final EcuAddress KAFAS_ADDRESS =
            EcuAddress.ofPhysicalOnly(0x6D0, 0x6D8, "KAFAS");

    // ── Internal map ────────────────────────────────────────────────────

    private static final Map<String, EcuAddress> ECU_MAP;

    static {
        Map<String, EcuAddress> map = new LinkedHashMap<>();
        map.put("DME", DME_ADDRESS);
        map.put("EGS", EGS_ADDRESS);
        map.put("CAS", CAS_ADDRESS);
        map.put("FRM", FRM_ADDRESS);
        map.put("KOMBI", KOMBI_ADDRESS);
        map.put("CIC", CIC_ADDRESS);
        map.put("IHKA", IHKA_ADDRESS);
        map.put("DSC", DSC_ADDRESS);
        map.put("ELV", ELV_ADDRESS);
        map.put("EHC", EHC_ADDRESS);
        map.put("SZL", SZL_ADDRESS);
        map.put("KAFAS", KAFAS_ADDRESS);
        ECU_MAP = Collections.unmodifiableMap(map);
    }

    private BmwEcuMap() {
        // utility class
    }

    /**
     * Returns the CAN address for the named BMW ECU.
     *
     * @param name the ECU name (e.g. "DME", "CAS")
     * @return the {@link EcuAddress}, or {@code null} if not found
     */
    public static EcuAddress getAddress(String name) {
        return ECU_MAP.get(name != null ? name.toUpperCase() : null);
    }

    /**
     * Returns all registered BMW ECU names and their addresses.
     *
     * @return an unmodifiable map of ECU name to address
     */
    public static Map<String, EcuAddress> getAllEcus() {
        return ECU_MAP;
    }

    /**
     * Returns the set of all registered ECU names.
     *
     * @return an unmodifiable set of ECU names
     */
    public static Set<String> getEcuNames() {
        return ECU_MAP.keySet();
    }

    /**
     * Checks whether an ECU with the given name is registered.
     *
     * @param name the ECU name
     * @return {@code true} if the ECU exists in the map
     */
    public static boolean hasEcu(String name) {
        return ECU_MAP.containsKey(name != null ? name.toUpperCase() : null);
    }
}
