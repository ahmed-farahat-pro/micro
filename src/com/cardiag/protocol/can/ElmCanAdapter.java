package com.cardiag.protocol.can;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CAN bus adapter implementation for the ELM327 OBD-II interface.
 * Communicates with the ELM327 chip over a serial port (or Bluetooth/Wi-Fi
 * serial bridge) using AT commands and hex-encoded CAN frame payloads.
 *
 * <p>This implementation simulates serial communication and can be extended
 * with a real serial port library (e.g. jSerialComm, RXTX).</p>
 */
public class ElmCanAdapter extends CanAdapter {

    /** Pattern matching a hex-encoded CAN response line from the ELM327. */
    private static final Pattern RESPONSE_PATTERN =
            Pattern.compile("^([0-9A-Fa-f]{3,8})\\s+(.+)$");

    /** End-of-response prompt character used by the ELM327. */
    private static final char ELM_PROMPT = '>';

    /** Carriage return character used to terminate ELM327 commands. */
    private static final char ELM_CR = '\r';

    private final String serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean echo = false;

    /**
     * Creates an ELM327 adapter for the specified serial port.
     *
     * @param serialPort the serial port path (e.g. "/dev/ttyUSB0" or "COM3")
     */
    public ElmCanAdapter(String serialPort) {
        if (serialPort == null || serialPort.isEmpty()) {
            throw new IllegalArgumentException("Serial port must not be null or empty");
        }
        this.serialPort = serialPort;
    }

    /**
     * Creates an ELM327 adapter using pre-existing I/O streams.
     * Useful for testing or when the serial port is managed externally.
     *
     * @param serialPort   the serial port name (for identification)
     * @param inputStream  the input stream to read responses from
     * @param outputStream the output stream to send commands to
     */
    public ElmCanAdapter(String serialPort, InputStream inputStream, OutputStream outputStream) {
        this(serialPort);
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void open() throws IOException {
        if (open) {
            throw new IllegalStateException("Adapter is already open on " + serialPort);
        }

        if (inputStream == null || outputStream == null) {
            // In a real implementation, open the serial port here:
            // SerialPort port = SerialPort.getCommPort(serialPort);
            // port.openPort();
            // inputStream = port.getInputStream();
            // outputStream = port.getOutputStream();
            throw new IOException(
                    "No I/O streams provided and native serial port access is not implemented. "
                            + "Use the constructor that accepts InputStream and OutputStream.");
        }

        open = true;

        // Initialize the ELM327
        sendAtCommand("ATZ");    // Reset
        sendAtCommand("ATE0");   // Echo off
        echo = false;
        sendAtCommand("ATL0");   // Linefeeds off
        sendAtCommand("ATS0");   // Spaces off in responses
        sendAtCommand("ATH1");   // Headers on (include CAN ID)
        sendAtCommand("ATSP6");  // Set protocol to ISO 15765-4 (CAN 11/500)

        configureBitrate();
    }

    @Override
    public void close() throws IOException {
        if (!open) {
            return;
        }
        try {
            sendAtCommand("ATZ"); // Reset the ELM327
        } catch (IOException ignored) {
            // Best-effort reset on close
        } finally {
            open = false;
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
            inputStream = null;
            outputStream = null;
        }
    }

    @Override
    public void sendFrame(CanFrame frame) throws IOException {
        ensureOpen();

        // Set the transmit header (CAN ID)
        String headerId = String.format("%03X", frame.getId());
        sendAtCommand("ATSH" + headerId);

        // Build the hex-encoded data payload
        StringBuilder hexData = new StringBuilder();
        byte[] data = frame.getData();
        for (byte b : data) {
            hexData.append(String.format("%02X", b & 0xFF));
        }

        // Send the data as a raw CAN frame
        sendCommand(hexData.toString());
    }

    @Override
    public CanFrame receiveFrame(int timeoutMs) throws IOException {
        ensureOpen();

        long deadline = System.currentTimeMillis() + timeoutMs;
        String response = readElmResponse(timeoutMs);

        if (response == null || response.isEmpty()) {
            return null;
        }

        // Parse each line of the response for CAN frames
        String[] lines = response.split("[\r\n]+");
        for (String line : lines) {
            line = line.trim();

            // Skip known non-data responses
            if (line.isEmpty() || line.equals("OK") || line.equals("?")
                    || line.equals("NO DATA") || line.equals("SEARCHING...")
                    || line.startsWith("AT") || line.equals("STOPPED")) {
                continue;
            }

            CanFrame frame = parseElmResponse(line);
            if (frame != null && passesFilter(frame)) {
                return frame;
            }
        }

        return null;
    }

    @Override
    public void addFilter(int id, int mask) throws IOException {
        super.addFilter(id, mask);
        if (open) {
            // Configure hardware filter on the ELM327
            sendAtCommand(String.format("ATCF%03X", id));
            sendAtCommand(String.format("ATCM%03X", mask));
        }
    }

    @Override
    public void clearFilters() throws IOException {
        super.clearFilters();
        if (open) {
            sendAtCommand("ATCF000");
            sendAtCommand("ATCM000");
        }
    }

    @Override
    public void setBitrate(int bitrate) throws IOException {
        super.setBitrate(bitrate);
        if (open) {
            configureBitrate();
        }
    }

    /**
     * Sends an AT command to the ELM327 and returns the response.
     *
     * @param command the AT command (e.g. "ATZ", "ATSP6")
     * @return the response string
     * @throws IOException if communication fails
     */
    public String sendAtCommand(String command) throws IOException {
        ensureOpen();
        sendCommand(command);
        return readElmResponse(2000);
    }

    /**
     * Returns the serial port path.
     *
     * @return the serial port
     */
    public String getSerialPort() {
        return serialPort;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Sends a raw command string to the ELM327 (terminated with CR).
     */
    private void sendCommand(String command) throws IOException {
        if (outputStream == null) {
            throw new IOException("Output stream is not available");
        }
        String fullCommand = command + ELM_CR;
        outputStream.write(fullCommand.getBytes("US-ASCII"));
        outputStream.flush();
    }

    /**
     * Reads the ELM327 response up to the prompt character or timeout.
     */
    private String readElmResponse(int timeoutMs) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream is not available");
        }

        StringBuilder sb = new StringBuilder();
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            int available = inputStream.available();
            if (available > 0) {
                int b = inputStream.read();
                if (b == -1) {
                    break;
                }
                char c = (char) b;
                if (c == ELM_PROMPT) {
                    break; // End of response
                }
                sb.append(c);
            } else {
                // Brief sleep to avoid busy-waiting
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while reading ELM response", e);
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Parses a single ELM327 response line into a CAN frame.
     *
     * @param line the response line (e.g. "7E8 0641055A")
     * @return the parsed frame, or {@code null} if the line is not a valid frame
     */
    private CanFrame parseElmResponse(String line) {
        Matcher matcher = RESPONSE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            // Try without spaces - the whole line might be hex with leading ID
            return parseCompactResponse(line);
        }

        try {
            int id = Integer.parseInt(matcher.group(1), 16);
            String hexData = matcher.group(2).replaceAll("\\s+", "");
            byte[] data = hexStringToBytes(hexData);
            return new CanFrame(id, data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Attempts to parse a compact (no-space) hex response where the first 3
     * characters are the CAN ID and the remainder is the data.
     */
    private CanFrame parseCompactResponse(String line) {
        String hex = line.replaceAll("\\s+", "");
        if (hex.length() < 3) {
            return null;
        }
        try {
            int id = Integer.parseInt(hex.substring(0, 3), 16);
            byte[] data = hexStringToBytes(hex.substring(3));
            return new CanFrame(id, data);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts a hex string to a byte array.
     */
    private static byte[] hexStringToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    /**
     * Configures the ELM327 bitrate based on the current {@link #bitrate} setting.
     */
    private void configureBitrate() throws IOException {
        switch (bitrate) {
            case 250000:
                sendAtCommand("ATSP5"); // ISO 15765-4 CAN (11 bit, 250 kbaud)
                break;
            case 500000:
                sendAtCommand("ATSP6"); // ISO 15765-4 CAN (11 bit, 500 kbaud)
                break;
            default:
                // Attempt user-defined bitrate via PP command
                sendAtCommand(String.format("ATPP2CSV%02X", bitrate / 1000));
                sendAtCommand("ATPP2CON");
                break;
        }
    }
}
