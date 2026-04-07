package com.cardiag.core.session;

import com.cardiag.core.ecu.EcuConnection;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages diagnostic session state and sends periodic TesterPresent (0x3E)
 * keep-alive messages to prevent the ECU from timing out the session.
 *
 * <p>When a non-default session is opened, the ECU expects to receive a
 * TesterPresent message at least every S3 timeout period (typically 5 seconds).
 * This manager uses a scheduled executor to send these messages automatically.</p>
 *
 * <p>Usage:
 * <pre>{@code
 *   SessionManager manager = new SessionManager();
 *   manager.startSession(connection, DiagnosticSession.EXTENDED);
 *   // ... perform diagnostic operations ...
 *   manager.stopSession();
 * }</pre>
 */
public class SessionManager {

    private static final Logger LOG = Logger.getLogger(SessionManager.class.getName());

    /** UDS TesterPresent service ID. */
    private static final int SID_TESTER_PRESENT = 0x3E;

    /** Sub-function for TesterPresent with suppress-positive-response bit cleared. */
    private static final int TESTER_PRESENT_SUB_FUNCTION = 0x00;

    /** Default keep-alive interval in milliseconds (2 seconds, well within the S3 timeout). */
    private static final long DEFAULT_KEEPALIVE_INTERVAL_MS = 2000;

    private final long keepAliveIntervalMs;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> keepAliveTask;
    private EcuConnection activeConnection;
    private DiagnosticSession activeSession;

    /**
     * Constructs a new {@code SessionManager} with the default keep-alive interval.
     */
    public SessionManager() {
        this(DEFAULT_KEEPALIVE_INTERVAL_MS);
    }

    /**
     * Constructs a new {@code SessionManager} with a custom keep-alive interval.
     *
     * @param keepAliveIntervalMs the interval between TesterPresent messages in milliseconds
     */
    public SessionManager(long keepAliveIntervalMs) {
        if (keepAliveIntervalMs <= 0) {
            throw new IllegalArgumentException("keepAliveIntervalMs must be positive");
        }
        this.keepAliveIntervalMs = keepAliveIntervalMs;
    }

    /**
     * Starts a diagnostic session and begins sending periodic TesterPresent messages.
     *
     * <p>If a session is already active, it is stopped before starting the new one.</p>
     *
     * @param connection the ECU connection to use
     * @param session    the diagnostic session to open
     * @throws IOException if the session change request fails
     */
    public synchronized void startSession(EcuConnection connection, DiagnosticSession session)
            throws IOException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(session, "session must not be null");

        // Stop any existing session
        if (activeConnection != null) {
            stopSession();
        }

        // Open the requested session on the ECU
        connection.openSession(session);

        this.activeConnection = connection;
        this.activeSession = session;

        // Start keep-alive for non-default sessions
        if (session != DiagnosticSession.DEFAULT) {
            startKeepAlive();
        }
    }

    /**
     * Stops the current session by cancelling the keep-alive timer and reverting
     * the ECU to the default diagnostic session.
     */
    public synchronized void stopSession() {
        stopKeepAlive();

        if (activeConnection != null && activeConnection.isConnected()) {
            try {
                activeConnection.openSession(DiagnosticSession.DEFAULT);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to revert to default session", e);
            }
        }

        activeConnection = null;
        activeSession = null;
    }

    /**
     * Sends a single TesterPresent (0x3E 0x00) message to the ECU.
     *
     * @throws IOException if the message cannot be sent
     */
    public synchronized void sendTesterPresent() throws IOException {
        if (activeConnection == null || !activeConnection.isConnected()) {
            throw new IllegalStateException("No active session");
        }

        byte[] request = new byte[]{
                (byte) SID_TESTER_PRESENT,
                (byte) TESTER_PRESENT_SUB_FUNCTION
        };

        activeConnection.sendAndValidate(request, SID_TESTER_PRESENT);
    }

    /**
     * Returns the currently active diagnostic session, or {@code null} if no
     * session is active.
     *
     * @return the active session or null
     */
    public synchronized DiagnosticSession getActiveSession() {
        return activeSession;
    }

    /**
     * Returns {@code true} if a session is currently active.
     *
     * @return session active status
     */
    public synchronized boolean isSessionActive() {
        return activeSession != null && activeConnection != null;
    }

    // ── Internal keep-alive management ──────────────────────────────────

    private void startKeepAlive() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cardiag-tester-present");
            t.setDaemon(true);
            return t;
        });

        keepAliveTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                sendTesterPresent();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "TesterPresent keep-alive failed", e);
            }
        }, keepAliveIntervalMs, keepAliveIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void stopKeepAlive() {
        if (keepAliveTask != null) {
            keepAliveTask.cancel(false);
            keepAliveTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
