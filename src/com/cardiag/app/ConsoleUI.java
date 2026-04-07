package com.cardiag.app;

import com.cardiag.core.coding.CodingMap;
import com.cardiag.core.coding.CodingParameter;
import com.cardiag.core.ecu.EcuIdentification;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Console output formatting utility for the CarDiag application.
 * Provides polished, colored terminal output using ANSI escape codes
 * including headers, menus, tables, progress bars, and user input helpers.
 */
public class ConsoleUI {

    // ── ANSI color codes ────────────────────────────────────────────────
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String DIM = "\u001B[2m";

    private static final int DEFAULT_WIDTH = 70;

    private final Scanner scanner;

    /**
     * Creates a new ConsoleUI with a Scanner reading from System.in.
     */
    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Creates a new ConsoleUI with the given Scanner (useful for testing).
     *
     * @param scanner the scanner to use for input
     */
    public ConsoleUI(Scanner scanner) {
        this.scanner = scanner;
    }

    // ── Output methods ──────────────────────────────────────────────────

    /**
     * Prints a decorative box header with the given title.
     *
     * @param title the title text to display
     */
    public void printHeader(String title) {
        String border = repeat("\u2550", DEFAULT_WIDTH - 2);
        System.out.println();
        System.out.println(BOLD + CYAN + "\u2554" + border + "\u2557" + RESET);
        String padded = centerText(title, DEFAULT_WIDTH - 2);
        System.out.println(BOLD + CYAN + "\u2551" + WHITE + padded + CYAN + "\u2551" + RESET);
        System.out.println(BOLD + CYAN + "\u255A" + border + "\u255D" + RESET);
        System.out.println();
    }

    /**
     * Prints a numbered menu with a title and list of options.
     *
     * @param title   the menu title
     * @param options the list of menu options
     */
    public void printMenu(String title, List<String> options) {
        String line = repeat("\u2500", DEFAULT_WIDTH - 2);
        System.out.println(BOLD + CYAN + "\u250C" + line + "\u2510" + RESET);
        String padded = centerText(title, DEFAULT_WIDTH - 2);
        System.out.println(BOLD + CYAN + "\u2502" + YELLOW + padded + CYAN + "\u2502" + RESET);
        System.out.println(BOLD + CYAN + "\u251C" + line + "\u2524" + RESET);

        for (int i = 0; i < options.size(); i++) {
            String entry = String.format("  %s%d%s. %s", BOLD + GREEN, i + 1, RESET + WHITE, options.get(i));
            int visibleLen = String.format("  %d. %s", i + 1, options.get(i)).length();
            int padding = DEFAULT_WIDTH - 2 - visibleLen;
            if (padding < 0) {
                padding = 0;
            }
            System.out.println(CYAN + "\u2502" + RESET + entry + repeat(" ", padding) + CYAN + "\u2502" + RESET);
        }

        System.out.println(BOLD + CYAN + "\u2514" + line + "\u2518" + RESET);
        System.out.println();
    }

    /**
     * Prints a formatted ASCII table with headers and rows.
     *
     * @param headers the column headers
     * @param rows    the row data
     */
    public void printTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return;
        }

        // Calculate column widths
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                if (row[i] != null && row[i].length() > widths[i]) {
                    widths[i] = row[i].length();
                }
            }
        }

        // Build format string
        StringBuilder fmt = new StringBuilder();
        StringBuilder sep = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            widths[i] += 2; // padding
            fmt.append("%-").append(widths[i]).append("s");
            sep.append(repeat("\u2500", widths[i]));
            if (i < widths.length - 1) {
                fmt.append("\u2502");
                sep.append("\u253C");
            }
        }

        // Print header
        System.out.println(BOLD + CYAN + repeat("\u2500", sep.length()) + RESET);
        String headerLine = String.format(fmt.toString(), (Object[]) padArray(headers, widths));
        System.out.println(BOLD + WHITE + headerLine + RESET);
        System.out.println(CYAN + sep.toString() + RESET);

        // Print rows
        for (String[] row : rows) {
            String[] paddedRow = padArray(row, widths);
            String rowLine = String.format(fmt.toString(), (Object[]) paddedRow);
            System.out.println(WHITE + rowLine + RESET);
        }
        System.out.println(CYAN + repeat("\u2500", sep.length()) + RESET);
        System.out.println();
    }

    /**
     * Prints a progress bar for the given task.
     *
     * @param task    the task description
     * @param percent the completion percentage (0-100)
     */
    public void printProgress(String task, int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        int barWidth = 40;
        int filled = (clamped * barWidth) / 100;
        int empty = barWidth - filled;

        String bar = repeat("\u2588", filled) + repeat("\u2591", empty);
        String color = clamped < 50 ? YELLOW : GREEN;

        System.out.printf("\r  %s%-20s%s [%s%s%s] %s%3d%%%s",
                CYAN, task, RESET,
                color, bar, RESET,
                BOLD, clamped, RESET);

        if (clamped >= 100) {
            System.out.println();
        }
    }

    /**
     * Prints a success message in green.
     *
     * @param msg the message
     */
    public void printSuccess(String msg) {
        System.out.println(GREEN + BOLD + "  [OK] " + RESET + GREEN + msg + RESET);
    }

    /**
     * Prints an error message in red.
     *
     * @param msg the message
     */
    public void printError(String msg) {
        System.out.println(RED + BOLD + "  [ERROR] " + RESET + RED + msg + RESET);
    }

    /**
     * Prints a warning message in yellow.
     *
     * @param msg the message
     */
    public void printWarning(String msg) {
        System.out.println(YELLOW + BOLD + "  [WARN] " + RESET + YELLOW + msg + RESET);
    }

    /**
     * Prints formatted ECU identification details.
     *
     * @param info the ECU identification data
     */
    public void printEcuInfo(EcuIdentification info) {
        if (info == null) {
            printWarning("No ECU identification data available.");
            return;
        }

        String line = repeat("\u2500", DEFAULT_WIDTH - 2);
        System.out.println();
        System.out.println(BOLD + CYAN + "\u250C" + line + "\u2510" + RESET);
        String padded = centerText("ECU Identification", DEFAULT_WIDTH - 2);
        System.out.println(BOLD + CYAN + "\u2502" + WHITE + padded + CYAN + "\u2502" + RESET);
        System.out.println(CYAN + "\u251C" + line + "\u2524" + RESET);

        printInfoRow("Hardware Version", info.getHardwareVersion());
        printInfoRow("Software Version", info.getSoftwareVersion());
        printInfoRow("Part Number", info.getPartNumber());
        printInfoRow("Serial Number", info.getSerialNumber());
        printInfoRow("VIN", info.getVin());
        printInfoRow("Manufacturing Date", info.getManufacturerDate());

        System.out.println(BOLD + CYAN + "\u2514" + line + "\u2518" + RESET);
        System.out.println();
    }

    /**
     * Prints a formatted table of Diagnostic Trouble Codes.
     *
     * @param dtcs the list of DTC entries, each as a String array: [code, status, description]
     */
    public void printDtcList(List<String[]> dtcs) {
        if (dtcs == null || dtcs.isEmpty()) {
            printSuccess("No Diagnostic Trouble Codes found.");
            return;
        }

        System.out.println();
        System.out.println(BOLD + YELLOW + "  Diagnostic Trouble Codes (" + dtcs.size() + " found)" + RESET);
        System.out.println();

        String[] headers = {"#", "DTC Code", "Status", "Description"};
        java.util.List<String[]> numberedRows = new java.util.ArrayList<>();
        for (int i = 0; i < dtcs.size(); i++) {
            String[] dtc = dtcs.get(i);
            String code = dtc.length > 0 ? dtc[0] : "";
            String status = dtc.length > 1 ? dtc[1] : "";
            String desc = dtc.length > 2 ? dtc[2] : "";
            numberedRows.add(new String[]{String.valueOf(i + 1), code, status, desc});
        }
        printTable(headers, numberedRows);
    }

    /**
     * Prints the contents of a coding map showing all parameters and their values.
     *
     * @param map the coding map to display
     */
    public void printCodingMap(CodingMap map) {
        if (map == null || map.size() == 0) {
            printWarning("No coding parameters available.");
            return;
        }

        System.out.println();
        System.out.println(BOLD + MAGENTA + "  Coding Parameters (" + map.size() + " entries)" + RESET);
        System.out.println();

        String[] headers = {"#", "Parameter", "Value", "Description"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        int idx = 1;
        for (CodingParameter param : map.getAllParameters()) {
            String valueLabel;
            Map<Integer, String> allowed = param.getAllowedValues();
            if (allowed != null && allowed.containsKey(param.getCurrentValue())) {
                valueLabel = param.getCurrentValue() + " (" + allowed.get(param.getCurrentValue()) + ")";
            } else {
                valueLabel = String.valueOf(param.getCurrentValue());
            }
            rows.add(new String[]{
                    String.valueOf(idx++),
                    param.getName(),
                    valueLabel,
                    param.getDescription()
            });
        }
        printTable(headers, rows);
    }

    // ── Input methods ───────────────────────────────────────────────────

    /**
     * Prompts the user and reads a line of input.
     *
     * @param prompt the prompt text
     * @return the user's input string
     */
    public String readInput(String prompt) {
        System.out.print(CYAN + "  " + prompt + RESET + " ");
        String line = scanner.nextLine();
        return line != null ? line.trim() : "";
    }

    /**
     * Prompts the user for an integer within a given range, re-prompting on invalid input.
     *
     * @param prompt the prompt text
     * @param min    the minimum allowed value (inclusive)
     * @param max    the maximum allowed value (inclusive)
     * @return the validated integer
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String input = readInput(prompt + " [" + min + "-" + max + "]:");
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                printError("Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Invalid number. Please enter a value between " + min + " and " + max + ".");
            }
        }
    }

    /**
     * Prompts the user for a yes/no confirmation.
     *
     * @param prompt the question to ask
     * @return {@code true} if the user confirms (y/yes), {@code false} otherwise
     */
    public boolean confirm(String prompt) {
        String input = readInput(prompt + " (y/n):");
        return input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
    }

    // ── Helper methods ──────────────────────────────────────────────────

    private void printInfoRow(String label, String value) {
        String display = value != null && !value.isEmpty() ? value : "(not available)";
        String content = String.format("  %-22s: %s", label, display);
        int padding = DEFAULT_WIDTH - 2 - content.length();
        if (padding < 0) {
            padding = 0;
        }
        System.out.println(CYAN + "\u2502" + RESET + BOLD + WHITE + "  " + label
                + RESET + DIM + repeat(" ", Math.max(0, 22 - label.length())) + ": "
                + RESET + GREEN + display
                + repeat(" ", Math.max(0, DEFAULT_WIDTH - 2 - 2 - 22 - 2 - display.length()))
                + CYAN + "\u2502" + RESET);
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int totalPad = width - text.length();
        int leftPad = totalPad / 2;
        int rightPad = totalPad - leftPad;
        return repeat(" ", leftPad) + text + repeat(" ", rightPad);
    }

    private String[] padArray(String[] arr, int[] widths) {
        String[] result = new String[widths.length];
        for (int i = 0; i < widths.length; i++) {
            String val = (i < arr.length && arr[i] != null) ? " " + arr[i] : " ";
            result[i] = val;
        }
        return result;
    }

    private static String repeat(String s, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
