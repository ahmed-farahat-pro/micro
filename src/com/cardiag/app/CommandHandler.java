package com.cardiag.app;

import com.cardiag.brand.Brand;
import com.cardiag.brand.BrandRegistry;
import com.cardiag.core.coding.CodingMap;
import com.cardiag.core.coding.CodingParameter;
import com.cardiag.core.ecu.EcuConnection;
import com.cardiag.core.ecu.EcuIdentification;
import com.cardiag.system.SystemType;
import com.cardiag.system.VehicleSystem;

import java.io.IOException;
import java.util.*;

/**
 * Command dispatcher that handles user interactions and routes them to
 * the appropriate session, ECU, or system operations. Each method represents
 * a logical operation or submenu within the application.
 */
public class CommandHandler {

    /**
     * Displays the brand selection menu and returns the selected brand.
     *
     * @param ui       the console UI for input/output
     * @param registry the brand registry containing available brands
     * @return the selected {@link Brand}, or {@code null} if the user chose to exit
     */
    public Brand handleBrandSelection(ConsoleUI ui, BrandRegistry registry) {
        Collection<Brand> brands = registry.listBrands();
        if (brands.isEmpty()) {
            ui.printError("No brands registered. Cannot continue.");
            return null;
        }

        List<String> options = new ArrayList<>();
        List<Brand> brandList = new ArrayList<>(brands);
        for (Brand brand : brandList) {
            options.add(brand.getName());
        }
        options.add("Exit");

        ui.printMenu("Select Vehicle Brand", options);
        int choice = ui.readInt("Enter choice", 1, options.size());

        if (choice == options.size()) {
            return null; // Exit selected
        }
        return brandList.get(choice - 1);
    }

    /**
     * Prompts the user to enter a VIN and validates its length.
     *
     * @param ui the console UI for input/output
     * @return the validated 17-character VIN string
     */
    public String handleVinEntry(ConsoleUI ui) {
        while (true) {
            String vin = ui.readInput("Enter Vehicle Identification Number (VIN):");
            if (vin.length() == 17) {
                return vin.toUpperCase();
            }
            ui.printError("VIN must be exactly 17 characters. You entered " + vin.length() + ".");
        }
    }

    /**
     * Runs the main menu interaction loop, dispatching to submenus
     * based on user selection.
     *
     * @param ui      the console UI for input/output
     * @param session the active vehicle session
     */
    public void handleMainMenu(ConsoleUI ui, VehicleSession session) {
        boolean running = true;
        while (running) {
            List<String> options = Arrays.asList(
                    "System Control",
                    "Read All DTCs",
                    "Clear All DTCs",
                    "ECU Identification",
                    "Coding & Programming",
                    "Adaptation Channels",
                    "Vehicle Info",
                    "Disconnect & Exit"
            );

            ui.printMenu("Main Menu - " + session.getBrand().getName(), options);
            int choice = ui.readInt("Select option", 1, options.size());

            try {
                switch (choice) {
                    case 1:
                        handleSystemSelection(ui, session);
                        break;
                    case 2:
                        handleReadAllDtcs(ui, session);
                        break;
                    case 3:
                        handleClearAllDtcs(ui, session);
                        break;
                    case 4:
                        handleEcuIdentificationMenu(ui, session);
                        break;
                    case 5:
                        handleCodingSelection(ui, session);
                        break;
                    case 6:
                        handleAdaptationSelection(ui, session);
                        break;
                    case 7:
                        handleVehicleInfo(ui, session);
                        break;
                    case 8:
                        if (ui.confirm("Disconnect from vehicle?")) {
                            session.disconnectAll();
                            ui.printSuccess("Disconnected from all ECUs.");
                            running = false;
                        }
                        break;
                    default:
                        ui.printError("Invalid option.");
                        break;
                }
            } catch (Exception e) {
                ui.printError("Operation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Displays system-specific operations for a selected vehicle system.
     *
     * @param ui     the console UI for input/output
     * @param system the vehicle system to interact with
     */
    public void handleSystemMenu(ConsoleUI ui, VehicleSystem system) {
        boolean running = true;
        while (running) {
            List<String> capabilities = system.getCapabilities();
            List<String> options = new ArrayList<>(capabilities);
            options.add("System Info");
            options.add("Back");

            ui.printMenu(system.getSystemType().getDisplayName() + " - Operations", options);
            int choice = ui.readInt("Select operation", 1, options.size());

            if (choice == options.size()) {
                running = false;
            } else if (choice == options.size() - 1) {
                System.out.println();
                System.out.println(ConsoleUI.CYAN + "  " + system.getSystemInfo() + ConsoleUI.RESET);
                System.out.println();
            } else {
                ui.printWarning("Executing: " + capabilities.get(choice - 1));
                ui.printSuccess("Operation completed.");
            }
        }
    }

    /**
     * Handles the DTC read/clear submenu.
     *
     * @param ui      the console UI for input/output
     * @param session the active vehicle session
     */
    public void handleDtcMenu(ConsoleUI ui, VehicleSession session) {
        List<String> options = Arrays.asList(
                "Read All DTCs",
                "Read DTCs by ECU",
                "Clear All DTCs",
                "Clear DTCs by ECU",
                "Back"
        );

        ui.printMenu("DTC Management", options);
        int choice = ui.readInt("Select option", 1, options.size());

        switch (choice) {
            case 1:
                handleReadAllDtcs(ui, session);
                break;
            case 2:
                handleReadDtcByEcu(ui, session);
                break;
            case 3:
                handleClearAllDtcs(ui, session);
                break;
            case 4:
                handleClearDtcByEcu(ui, session);
                break;
            case 5:
                // Back
                break;
            default:
                break;
        }
    }

    /**
     * Handles the coding read/modify/write operations for an ECU.
     *
     * @param ui   the console UI for input/output
     * @param conn the ECU connection
     * @param map  the coding map to work with
     */
    public void handleCodingMenu(ConsoleUI ui, EcuConnection conn, CodingMap map) {
        boolean running = true;
        while (running) {
            List<String> options = Arrays.asList(
                    "Read Current Coding",
                    "Modify Parameter",
                    "Write Coding to ECU",
                    "Reset to Defaults",
                    "Back"
            );

            ui.printMenu("Coding - " + conn.getEcuDefinition().getName(), options);
            int choice = ui.readInt("Select option", 1, options.size());

            try {
                switch (choice) {
                    case 1:
                        readCodingFromEcu(ui, conn, map);
                        break;
                    case 2:
                        modifyCodingParameter(ui, map);
                        break;
                    case 3:
                        writeCodingToEcu(ui, conn, map);
                        break;
                    case 4:
                        if (ui.confirm("Reset all parameters to factory defaults?")) {
                            Map<String, Integer> defaults = conn.getEcuDefinition().getDefaultCodingMap();
                            for (CodingParameter param : map.getAllParameters()) {
                                Integer defaultVal = defaults.get(param.getName());
                                if (defaultVal != null) {
                                    param.setCurrentValue(defaultVal);
                                }
                            }
                            ui.printSuccess("Parameters reset to factory defaults.");
                            ui.printCodingMap(map);
                        }
                        break;
                    case 5:
                        running = false;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                ui.printError("Coding operation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Handles adaptation channel read/modify operations.
     *
     * @param ui       the console UI for input/output
     * @param conn     the ECU connection
     * @param channels the adaptation channel map (channel ID to description)
     */
    public void handleAdaptationMenu(ConsoleUI ui, EcuConnection conn, Map<Integer, String> channels) {
        if (channels == null || channels.isEmpty()) {
            ui.printWarning("No adaptation channels available for this ECU.");
            return;
        }

        boolean running = true;
        while (running) {
            List<String> options = Arrays.asList(
                    "List All Channels",
                    "Read Channel Value",
                    "Write Channel Value",
                    "Back"
            );

            ui.printMenu("Adaptation - " + conn.getEcuDefinition().getName(), options);
            int choice = ui.readInt("Select option", 1, options.size());

            try {
                switch (choice) {
                    case 1:
                        listAdaptationChannels(ui, channels);
                        break;
                    case 2:
                        readAdaptationChannel(ui, conn, channels);
                        break;
                    case 3:
                        writeAdaptationChannel(ui, conn, channels);
                        break;
                    case 4:
                        running = false;
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                ui.printError("Adaptation operation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Scans for ECUs and displays the results.
     *
     * @param ui      the console UI for input/output
     * @param session the active vehicle session
     */
    public void handleEcuScan(ConsoleUI ui, VehicleSession session) {
        ui.printHeader("ECU Scan");
        System.out.println(ConsoleUI.CYAN + "  Scanning for ECUs on the diagnostic bus..." + ConsoleUI.RESET);
        System.out.println();

        Map<String, com.cardiag.core.ecu.EcuDefinition> ecuMap = session.getBrand().getEcuMap();
        int total = ecuMap.size();
        int scanned = 0;

        List<String> foundEcus = new ArrayList<>();
        for (Map.Entry<String, com.cardiag.core.ecu.EcuDefinition> entry : ecuMap.entrySet()) {
            scanned++;
            int percent = (scanned * 100) / total;
            ui.printProgress("Scanning " + entry.getKey(), percent);

            try {
                EcuConnection conn = session.connectEcu(entry.getKey());
                foundEcus.add(entry.getKey());
                // Small delay simulation for visual effect
                Thread.sleep(100);
            } catch (Exception e) {
                // ECU not responding
            }
        }
        System.out.println();
        System.out.println();

        if (foundEcus.isEmpty()) {
            ui.printWarning("No ECUs responded. Check connection and try again.");
        } else {
            ui.printSuccess(foundEcus.size() + " of " + total + " ECUs found:");
            System.out.println();
            String[] headers = {"#", "ECU Name", "Description", "Status"};
            List<String[]> rows = new ArrayList<>();
            int idx = 1;
            for (Map.Entry<String, com.cardiag.core.ecu.EcuDefinition> entry : ecuMap.entrySet()) {
                String status = foundEcus.contains(entry.getKey())
                        ? ConsoleUI.GREEN + "ONLINE" + ConsoleUI.RESET
                        : ConsoleUI.RED + "OFFLINE" + ConsoleUI.RESET;
                rows.add(new String[]{
                        String.valueOf(idx++),
                        entry.getKey(),
                        entry.getValue().getName(),
                        foundEcus.contains(entry.getKey()) ? "ONLINE" : "OFFLINE"
                });
            }
            ui.printTable(headers, rows);
        }
    }

    /**
     * Reads and displays ECU identification data for a connected ECU.
     *
     * @param ui   the console UI for input/output
     * @param conn the ECU connection
     */
    public void handleEcuIdentification(ConsoleUI ui, EcuConnection conn) {
        ui.printHeader("ECU Identification - " + conn.getEcuDefinition().getName());
        try {
            EcuIdentification info = EcuIdentification.fromEcuConnection(conn);
            ui.printEcuInfo(info);
        } catch (IOException e) {
            ui.printError("Failed to read ECU identification: " + e.getMessage());
        }
    }

    // ── Private helper methods ──────────────────────────────────────────

    private void handleSystemSelection(ConsoleUI ui, VehicleSession session) {
        List<SystemType> supported = session.getBrand().getSupportedSystems();
        List<String> options = new ArrayList<>();
        for (SystemType type : supported) {
            options.add(type.getDisplayName() + " - " + type.getDescription());
        }
        options.add("Back");

        ui.printMenu("System Control", options);
        int choice = ui.readInt("Select system", 1, options.size());

        if (choice == options.size()) {
            return;
        }

        SystemType selectedType = supported.get(choice - 1);
        VehicleSystem system = session.getSystem(selectedType);

        if (system != null) {
            handleSystemMenu(ui, system);
        } else {
            ui.printWarning("System '" + selectedType.getDisplayName()
                    + "' is not active. No ECU found for this subsystem.");
        }
    }

    private void handleReadAllDtcs(ConsoleUI ui, VehicleSession session) {
        ui.printHeader("Read All DTCs");
        Map<String, EcuConnection> ecus = session.getConnectedEcus();

        if (ecus.isEmpty()) {
            ui.printWarning("No ECUs connected. Run ECU scan first.");
            return;
        }

        int totalDtcs = 0;
        for (Map.Entry<String, EcuConnection> entry : ecus.entrySet()) {
            System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                    + "  --- " + entry.getKey() + " (" + entry.getValue().getEcuDefinition().getName() + ") ---"
                    + ConsoleUI.RESET);

            try {
                // UDS ReadDTCInformation (service 0x19, sub-function 0x02)
                byte[] request = new byte[]{0x19, 0x02, (byte) 0xFF};
                byte[] response = entry.getValue().sendAndValidate(request, 0x19);

                List<String[]> dtcs = parseDtcResponse(response);
                if (dtcs.isEmpty()) {
                    ui.printSuccess("No DTCs stored.");
                } else {
                    totalDtcs += dtcs.size();
                    ui.printDtcList(dtcs);
                }
            } catch (Exception e) {
                ui.printWarning("Could not read DTCs from " + entry.getKey() + ": " + e.getMessage());
            }
            System.out.println();
        }

        System.out.println(ConsoleUI.BOLD + ConsoleUI.WHITE
                + "  Total DTCs found: " + totalDtcs + ConsoleUI.RESET);
        System.out.println();
    }

    private void handleClearAllDtcs(ConsoleUI ui, VehicleSession session) {
        if (!ui.confirm("Clear all DTCs from all ECUs? This action cannot be undone.")) {
            return;
        }

        ui.printHeader("Clear All DTCs");
        Map<String, EcuConnection> ecus = session.getConnectedEcus();

        for (Map.Entry<String, EcuConnection> entry : ecus.entrySet()) {
            try {
                // UDS ClearDiagnosticInformation (service 0x14)
                byte[] request = new byte[]{0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
                entry.getValue().sendAndValidate(request, 0x14);
                ui.printSuccess("Cleared DTCs from " + entry.getKey());
            } catch (Exception e) {
                ui.printWarning("Could not clear DTCs from " + entry.getKey() + ": " + e.getMessage());
            }
        }
        System.out.println();
    }

    private void handleReadDtcByEcu(ConsoleUI ui, VehicleSession session) {
        EcuConnection conn = selectEcu(ui, session);
        if (conn == null) {
            return;
        }

        try {
            byte[] request = new byte[]{0x19, 0x02, (byte) 0xFF};
            byte[] response = conn.sendAndValidate(request, 0x19);
            List<String[]> dtcs = parseDtcResponse(response);
            ui.printDtcList(dtcs);
        } catch (Exception e) {
            ui.printError("Failed to read DTCs: " + e.getMessage());
        }
    }

    private void handleClearDtcByEcu(ConsoleUI ui, VehicleSession session) {
        EcuConnection conn = selectEcu(ui, session);
        if (conn == null) {
            return;
        }

        if (!ui.confirm("Clear DTCs from " + conn.getEcuDefinition().getName() + "?")) {
            return;
        }

        try {
            byte[] request = new byte[]{0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            conn.sendAndValidate(request, 0x14);
            ui.printSuccess("DTCs cleared successfully.");
        } catch (Exception e) {
            ui.printError("Failed to clear DTCs: " + e.getMessage());
        }
    }

    private void handleEcuIdentificationMenu(ConsoleUI ui, VehicleSession session) {
        EcuConnection conn = selectEcu(ui, session);
        if (conn != null) {
            handleEcuIdentification(ui, conn);
        }
    }

    private void handleCodingSelection(ConsoleUI ui, VehicleSession session) {
        EcuConnection conn = selectEcu(ui, session);
        if (conn == null) {
            return;
        }

        com.cardiag.core.ecu.EcuDefinition ecuDef = conn.getEcuDefinition();
        Map<String, Integer> defaultCoding = ecuDef.getDefaultCodingMap();

        CodingMap codingMap = new CodingMap();
        for (Map.Entry<String, Integer> entry : defaultCoding.entrySet()) {
            CodingParameter param = new CodingParameter(
                    entry.getKey(),
                    0, 0, 8,
                    Collections.emptyMap(),
                    "Coding parameter: " + entry.getKey()
            );
            param.setCurrentValue(entry.getValue());
            codingMap.addParameter(param);
        }

        handleCodingMenu(ui, conn, codingMap);
    }

    private void handleAdaptationSelection(ConsoleUI ui, VehicleSession session) {
        EcuConnection conn = selectEcu(ui, session);
        if (conn == null) {
            return;
        }

        Map<Integer, String> channels = conn.getEcuDefinition().getAdaptationChannels();
        handleAdaptationMenu(ui, conn, channels);
    }

    private void handleVehicleInfo(ConsoleUI ui, VehicleSession session) {
        ui.printHeader("Vehicle Information");
        System.out.println(ConsoleUI.WHITE + session.getSessionSummary() + ConsoleUI.RESET);
    }

    private EcuConnection selectEcu(ConsoleUI ui, VehicleSession session) {
        Map<String, EcuConnection> ecus = session.getConnectedEcus();
        if (ecus.isEmpty()) {
            ui.printWarning("No ECUs connected. Run ECU scan first.");
            return null;
        }

        List<String> ecuNames = new ArrayList<>(ecus.keySet());
        List<String> options = new ArrayList<>();
        for (String name : ecuNames) {
            EcuConnection conn = ecus.get(name);
            options.add(name + " - " + conn.getEcuDefinition().getName());
        }
        options.add("Cancel");

        ui.printMenu("Select ECU", options);
        int choice = ui.readInt("Select ECU", 1, options.size());

        if (choice == options.size()) {
            return null;
        }
        return ecus.get(ecuNames.get(choice - 1));
    }

    private void readCodingFromEcu(ConsoleUI ui, EcuConnection conn, CodingMap map) throws IOException {
        ui.printSuccess("Reading coding data from ECU...");
        Map<String, byte[]> codingBlocks = conn.getEcuDefinition().getCodingBlocks();

        if (!codingBlocks.isEmpty()) {
            Map.Entry<String, byte[]> firstBlock = codingBlocks.entrySet().iterator().next();
            try {
                byte[] data = conn.readData(0xF199); // Common coding DID
                map.decode(data);
                ui.printSuccess("Coding data read successfully.");
            } catch (Exception e) {
                ui.printWarning("Could not read live coding data, showing stored values.");
            }
        }
        ui.printCodingMap(map);
    }

    private void modifyCodingParameter(ConsoleUI ui, CodingMap map) {
        List<CodingParameter> params = new ArrayList<>(map.getAllParameters());
        List<String> options = new ArrayList<>();
        for (CodingParameter param : params) {
            Map<Integer, String> allowed = param.getAllowedValues();
            String currentLabel;
            if (allowed != null && allowed.containsKey(param.getCurrentValue())) {
                currentLabel = allowed.get(param.getCurrentValue());
            } else {
                currentLabel = String.valueOf(param.getCurrentValue());
            }
            options.add(param.getName() + " = " + currentLabel);
        }
        options.add("Cancel");

        ui.printMenu("Select Parameter to Modify", options);
        int choice = ui.readInt("Select parameter", 1, options.size());

        if (choice == options.size()) {
            return;
        }

        CodingParameter param = params.get(choice - 1);
        Map<Integer, String> allowed = param.getAllowedValues();

        if (allowed != null && !allowed.isEmpty()) {
            List<String> valueOptions = new ArrayList<>();
            List<Integer> valueKeys = new ArrayList<>(allowed.keySet());
            for (int key : valueKeys) {
                String marker = (key == param.getCurrentValue()) ? " <-- current" : "";
                valueOptions.add(key + " = " + allowed.get(key) + marker);
            }
            ui.printMenu("Select value for '" + param.getName() + "'", valueOptions);
            int valChoice = ui.readInt("Select value", 1, valueOptions.size());
            param.setCurrentValue(valueKeys.get(valChoice - 1));
        } else {
            int newValue = ui.readInt("Enter new value for '" + param.getName() + "'",
                    0, param.getMaxValue());
            param.setCurrentValue(newValue);
        }

        ui.printSuccess("Parameter '" + param.getName() + "' set to " + param.getCurrentValue());
    }

    private void writeCodingToEcu(ConsoleUI ui, EcuConnection conn, CodingMap map) throws IOException {
        ui.printCodingMap(map);
        if (!ui.confirm("Write this coding configuration to ECU?")) {
            return;
        }

        ui.printWarning("Writing coding data to ECU...");
        byte[] encoded = map.encode();

        try {
            conn.writeData(0xF199, encoded);
            ui.printSuccess("Coding data written successfully.");
        } catch (Exception e) {
            ui.printError("Failed to write coding data: " + e.getMessage());
        }
    }

    private void listAdaptationChannels(ConsoleUI ui, Map<Integer, String> channels) {
        String[] headers = {"Channel", "Description"};
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : channels.entrySet()) {
            rows.add(new String[]{String.valueOf(entry.getKey()), entry.getValue()});
        }
        ui.printTable(headers, rows);
    }

    private void readAdaptationChannel(ConsoleUI ui, EcuConnection conn,
                                       Map<Integer, String> channels) throws IOException {
        int channelId = ui.readInt("Enter channel number", 0, 255);
        String desc = channels.getOrDefault(channelId, "Unknown channel");

        try {
            byte[] data = conn.readData(0x0200 + channelId);
            int value = 0;
            for (byte b : data) {
                value = (value << 8) | (b & 0xFF);
            }
            System.out.println();
            System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN + "  Channel " + channelId
                    + ": " + ConsoleUI.RESET + desc);
            System.out.println(ConsoleUI.GREEN + "  Current value: " + value + ConsoleUI.RESET);
            System.out.println();
        } catch (Exception e) {
            ui.printError("Failed to read adaptation channel " + channelId + ": " + e.getMessage());
        }
    }

    private void writeAdaptationChannel(ConsoleUI ui, EcuConnection conn,
                                        Map<Integer, String> channels) throws IOException {
        int channelId = ui.readInt("Enter channel number", 0, 255);
        String desc = channels.getOrDefault(channelId, "Unknown channel");
        System.out.println(ConsoleUI.CYAN + "  Channel: " + desc + ConsoleUI.RESET);

        int newValue = ui.readInt("Enter new value", 0, 65535);

        if (!ui.confirm("Write value " + newValue + " to channel " + channelId + "?")) {
            return;
        }

        try {
            byte[] data = new byte[]{
                    (byte) ((newValue >> 8) & 0xFF),
                    (byte) (newValue & 0xFF)
            };
            conn.writeData(0x0200 + channelId, data);
            ui.printSuccess("Adaptation channel " + channelId + " set to " + newValue);
        } catch (Exception e) {
            ui.printError("Failed to write adaptation channel: " + e.getMessage());
        }
    }

    private List<String[]> parseDtcResponse(byte[] response) {
        List<String[]> dtcs = new ArrayList<>();
        if (response == null || response.length < 3) {
            return dtcs;
        }

        // Skip service ID echo and sub-function echo (bytes 0-2),
        // then read DTCs in groups of 4 bytes (3 bytes DTC + 1 byte status)
        int offset = 3;
        while (offset + 3 < response.length) {
            int dtcHigh = response[offset] & 0xFF;
            int dtcMid = response[offset + 1] & 0xFF;
            int dtcLow = response[offset + 2] & 0xFF;
            int status = response[offset + 3] & 0xFF;

            String dtcCode = String.format("P%02X%02X", (dtcHigh << 4) | (dtcMid >> 4), dtcLow);
            String statusStr = decodeDtcStatus(status);
            String description = "Diagnostic Trouble Code";

            dtcs.add(new String[]{dtcCode, statusStr, description});
            offset += 4;
        }

        return dtcs;
    }

    private String decodeDtcStatus(int status) {
        List<String> flags = new ArrayList<>();
        if ((status & 0x01) != 0) flags.add("Active");
        if ((status & 0x02) != 0) flags.add("Confirmed");
        if ((status & 0x04) != 0) flags.add("Pending");
        if ((status & 0x08) != 0) flags.add("History");
        return flags.isEmpty() ? "Unknown" : String.join(", ", flags);
    }
}
