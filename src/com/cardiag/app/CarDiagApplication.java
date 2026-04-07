package com.cardiag.app;

import com.cardiag.app.config.AppConfig;
import com.cardiag.brand.Brand;
import com.cardiag.brand.BrandRegistry;
import com.cardiag.brand.citroen.CitroenBrand;
import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.protocol.ProtocolFactory;
import com.cardiag.protocol.ProtocolType;
import com.cardiag.protocol.can.CanBus;
import com.cardiag.protocol.can.ElmCanAdapter;

import java.io.IOException;

/**
 * Main entry point for the CarDiag Pro diagnostic and programming tool.
 * Orchestrates the application lifecycle: configuration loading, brand
 * registration, adapter connection, ECU scanning, and the main interaction loop.
 */
public class CarDiagApplication {

    private static final String APP_NAME = "CarDiag Pro - Universal Car Programming System";
    private static final String APP_VERSION = "1.0.0";
    private static final String CONFIG_FILE = "cardiag.properties";

    /**
     * Application entry point.
     *
     * @param args command-line arguments (optional config file path)
     */
    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        CommandHandler handler = new CommandHandler();

        // ── 1. Welcome banner ───────────────────────────────────────────
        printBanner(ui);

        // ── 2. Load configuration ───────────────────────────────────────
        AppConfig config = new AppConfig();
        String configPath = args.length > 0 ? args[0] : CONFIG_FILE;
        try {
            config.load(configPath);
            ui.printSuccess("Configuration loaded: " + config);
        } catch (IOException e) {
            ui.printWarning("Could not load config file '" + configPath + "': " + e.getMessage());
            ui.printWarning("Using default configuration.");
        }

        // ── 3. Initialize brand registry ────────────────────────────────
        BrandRegistry registry = BrandRegistry.getInstance();
        try {
            // Register BMW brand
            Class<?> bmwClass = Class.forName("com.cardiag.brand.bmw.BmwBrand");
            Brand bmwBrand = (Brand) bmwClass.getDeclaredConstructor().newInstance();
            registry.registerBrand(bmwBrand);
            ui.printSuccess("Registered brand: " + bmwBrand.getName());
        } catch (Exception e) {
            ui.printWarning("BMW brand not available: " + e.getMessage());
        }

        try {
            // Register Citroen brand
            CitroenBrand citroenBrand = new CitroenBrand();
            registry.registerBrand(citroenBrand);
            ui.printSuccess("Registered brand: " + citroenBrand.getName());
        } catch (Exception e) {
            ui.printWarning("Citro\u00ebn brand not available: " + e.getMessage());
        }

        System.out.println();

        // ── 4. Main application loop ────────────────────────────────────
        boolean running = true;
        while (running) {
            try {
                // ── 5a. Brand selection ─────────────────────────────────
                Brand selectedBrand = handler.handleBrandSelection(ui, registry);
                if (selectedBrand == null) {
                    running = false;
                    continue;
                }

                ui.printSuccess("Selected brand: " + selectedBrand.getName());

                // ── 5b. VIN entry ───────────────────────────────────────
                String vin = handler.handleVinEntry(ui);
                ui.printSuccess("VIN: " + vin);

                // ── 5c. Adapter connection ──────────────────────────────
                DiagnosticProtocol protocol = null;
                try {
                    ui.printHeader("Connecting to Vehicle");
                    System.out.println(ConsoleUI.CYAN + "  Adapter: " + config.getAdapterType()
                            + " on " + config.getSerialPort() + ConsoleUI.RESET);
                    System.out.println(ConsoleUI.CYAN + "  Baud Rate: " + config.getBaudRate()
                            + " bps" + ConsoleUI.RESET);
                    System.out.println();

                    ElmCanAdapter adapter = new ElmCanAdapter(config.getSerialPort());
                    adapter.setBitrate(config.getBaudRate());

                    try {
                        adapter.open();
                    } catch (IOException openEx) {
                        ui.printWarning("Hardware adapter not available: " + openEx.getMessage());
                        ui.printWarning("Running in simulation mode.");
                    }

                    CanBus canBus = adapter;
                    protocol = ProtocolFactory.createProtocol(ProtocolType.UDS, canBus);
                    protocol.setTimeout(config.getTimeout());

                    ui.printSuccess("Protocol initialized: " + protocol.getProtocolType().getDisplayName());
                } catch (Exception e) {
                    ui.printError("Failed to initialize adapter: " + e.getMessage());
                    ui.printWarning("Continuing with limited functionality.");
                }

                // ── 5d. Create session ──────────────────────────────────
                VehicleSession session = new VehicleSession();
                try {
                    session.initialize(selectedBrand, vin,
                            protocol != null ? protocol : createFallbackProtocol());
                    ui.printSuccess("Vehicle session established.");
                    System.out.println();
                } catch (Exception e) {
                    ui.printError("Failed to create vehicle session: " + e.getMessage());
                    continue;
                }

                // ── 5e. ECU scan ────────────────────────────────────────
                if (ui.confirm("Perform ECU scan now?")) {
                    handler.handleEcuScan(ui, session);
                }

                // ── 5f. Main menu loop ──────────────────────────────────
                handler.handleMainMenu(ui, session);

            } catch (Exception e) {
                ui.printError("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                if (!ui.confirm("Continue running?")) {
                    running = false;
                }
            }
        }

        // ── Farewell ────────────────────────────────────────────────────
        System.out.println();
        ui.printHeader("Goodbye");
        System.out.println(ConsoleUI.CYAN + "  Thank you for using " + APP_NAME + ConsoleUI.RESET);
        System.out.println(ConsoleUI.DIM + "  Drive safe!" + ConsoleUI.RESET);
        System.out.println();
    }

    /**
     * Prints the ASCII art welcome banner.
     *
     * @param ui the console UI
     */
    private static void printBanner(ConsoleUI ui) {
        System.out.println();
        System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                + "   ____            ____  _                ____           " + ConsoleUI.RESET);
        System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                + "  / ___|__ _ _ __ |  _ \\(_) __ _  __ _   |  _ \\ _ __ ___ " + ConsoleUI.RESET);
        System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                + " | |   / _` | '__|  | | | |/ _` |/ _` |  | |_) | '__/ _ \\" + ConsoleUI.RESET);
        System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                + " | |__| (_| | |  | |_| | | (_| | (_| |  |  __/| | | (_) |" + ConsoleUI.RESET);
        System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                + "  \\____\\__,_|_|  |____/|_|\\__,_|\\__, |  |_|   |_|  \\___/ " + ConsoleUI.RESET);
        System.out.println(ConsoleUI.BOLD + ConsoleUI.CYAN
                + "                                 |___/                     " + ConsoleUI.RESET);
        System.out.println();
        System.out.println(ConsoleUI.BOLD + ConsoleUI.WHITE
                + "         " + APP_NAME + ConsoleUI.RESET);
        System.out.println(ConsoleUI.DIM
                + "                         Version " + APP_VERSION + ConsoleUI.RESET);
        System.out.println();

        String line = "";
        for (int i = 0; i < 68; i++) {
            line += "\u2550";
        }
        System.out.println(ConsoleUI.CYAN + "  " + line + ConsoleUI.RESET);
        System.out.println();
    }

    /**
     * Creates a fallback protocol that operates in offline/simulation mode
     * when no real hardware adapter is available.
     *
     * @return a no-op diagnostic protocol
     */
    private static DiagnosticProtocol createFallbackProtocol() {
        return new DiagnosticProtocol() {
            private boolean connected = false;

            @Override
            public void connect() throws IOException {
                connected = true;
            }

            @Override
            public void disconnect() throws IOException {
                connected = false;
            }

            @Override
            public void sendRequest(byte[] data) throws IOException {
                // No-op in simulation mode
            }

            @Override
            public byte[] readResponse() throws IOException {
                // Return empty positive response in simulation mode
                return new byte[]{0x7F, 0x00, 0x11};
            }

            @Override
            public boolean isConnected() {
                return connected;
            }

            @Override
            public ProtocolType getProtocolType() {
                return ProtocolType.UDS;
            }
        };
    }
}
