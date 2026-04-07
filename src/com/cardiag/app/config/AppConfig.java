package com.cardiag.app.config;

import java.io.*;
import java.util.Properties;

/**
 * Application configuration for the CarDiag diagnostic tool.
 * Manages connection parameters, adapter settings, and runtime options.
 * Supports loading from and saving to a standard Java properties file.
 */
public class AppConfig {

    private static final String KEY_SERIAL_PORT = "serialPort";
    private static final String KEY_BAUD_RATE = "baudRate";
    private static final String KEY_ADAPTER_TYPE = "adapterType";
    private static final String KEY_LOG_LEVEL = "logLevel";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_KEEP_ALIVE_INTERVAL = "keepAliveInterval";

    private static final String DEFAULT_SERIAL_PORT = "/dev/ttyUSB0";
    private static final int DEFAULT_BAUD_RATE = 500000;
    private static final String DEFAULT_ADAPTER_TYPE = "ELM327";
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_KEEP_ALIVE_INTERVAL = 2000;

    private String serialPort;
    private int baudRate;
    private String adapterType;
    private String logLevel;
    private int timeout;
    private int keepAliveInterval;

    /**
     * Creates a new configuration with default values.
     */
    public AppConfig() {
        this.serialPort = DEFAULT_SERIAL_PORT;
        this.baudRate = DEFAULT_BAUD_RATE;
        this.adapterType = DEFAULT_ADAPTER_TYPE;
        this.logLevel = DEFAULT_LOG_LEVEL;
        this.timeout = DEFAULT_TIMEOUT;
        this.keepAliveInterval = DEFAULT_KEEP_ALIVE_INTERVAL;
    }

    /**
     * Loads configuration from a properties file. Any missing properties retain
     * their current (default) values.
     *
     * @param filePath the path to the properties file
     * @throws IOException if the file cannot be read
     */
    public void load(String filePath) throws IOException {
        Properties props = new Properties();
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }

        this.serialPort = props.getProperty(KEY_SERIAL_PORT, this.serialPort);
        this.adapterType = props.getProperty(KEY_ADAPTER_TYPE, this.adapterType);
        this.logLevel = props.getProperty(KEY_LOG_LEVEL, this.logLevel);

        String baudRateStr = props.getProperty(KEY_BAUD_RATE);
        if (baudRateStr != null) {
            try {
                this.baudRate = Integer.parseInt(baudRateStr.trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        String timeoutStr = props.getProperty(KEY_TIMEOUT);
        if (timeoutStr != null) {
            try {
                this.timeout = Integer.parseInt(timeoutStr.trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        String keepAliveStr = props.getProperty(KEY_KEEP_ALIVE_INTERVAL);
        if (keepAliveStr != null) {
            try {
                this.keepAliveInterval = Integer.parseInt(keepAliveStr.trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
    }

    /**
     * Saves the current configuration to a properties file.
     *
     * @param filePath the path to the properties file
     * @throws IOException if the file cannot be written
     */
    public void save(String filePath) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_SERIAL_PORT, serialPort);
        props.setProperty(KEY_BAUD_RATE, String.valueOf(baudRate));
        props.setProperty(KEY_ADAPTER_TYPE, adapterType);
        props.setProperty(KEY_LOG_LEVEL, logLevel);
        props.setProperty(KEY_TIMEOUT, String.valueOf(timeout));
        props.setProperty(KEY_KEEP_ALIVE_INTERVAL, String.valueOf(keepAliveInterval));

        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "CarDiag Pro Configuration");
        }
    }

    // ── Getters ─────────────────────────────────────────────────────────

    public String getSerialPort() {
        return serialPort;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    // ── Setters ─────────────────────────────────────────────────────────

    public void setSerialPort(String serialPort) {
        this.serialPort = serialPort != null ? serialPort : DEFAULT_SERIAL_PORT;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate > 0 ? baudRate : DEFAULT_BAUD_RATE;
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType != null ? adapterType : DEFAULT_ADAPTER_TYPE;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel != null ? logLevel : DEFAULT_LOG_LEVEL;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval > 0 ? keepAliveInterval : DEFAULT_KEEP_ALIVE_INTERVAL;
    }

    @Override
    public String toString() {
        return String.format(
                "AppConfig{serialPort='%s', baudRate=%d, adapterType='%s', logLevel='%s', timeout=%d, keepAliveInterval=%d}",
                serialPort, baudRate, adapterType, logLevel, timeout, keepAliveInterval);
    }
}
