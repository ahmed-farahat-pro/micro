package com.cardiag.protocol.kwp;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.protocol.ProtocolType;
import com.cardiag.protocol.can.CanBus;
import com.cardiag.protocol.can.CanFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * KWP2000 (Keyword Protocol 2000) implementation over CAN bus.
 *
 * <p>This implementation follows ISO 14230-3 and uses ISO-TP (ISO 15765-2)
 * for transport-layer segmentation, similar to UDS but with KWP2000-specific
 * service identifiers and timing requirements.</p>
 *
 * <h3>KWP2000 Timing Parameters</h3>
 * <ul>
 *     <li><b>P2:</b> Time between request and response (default 50 ms, max 5000 ms).</li>
 *     <li><b>P3:</b> Time between responses (idle timeout, default 5000 ms).</li>
 * </ul>
 */
public class Kwp2000Protocol extends DiagnosticProtocol {

    /** Maximum data bytes in a single CAN frame for KWP over CAN. */
    private static final int SINGLE_FRAME_MAX_DATA = 7;

    /** ISO-TP frame type identifiers. */
    private static final int FRAME_TYPE_SINGLE = 0x00;
    private static final int FRAME_TYPE_FIRST = 0x10;
    private static final int FRAME_TYPE_CONSECUTIVE = 0x20;
    private static final int FRAME_TYPE_FLOW_CONTROL = 0x30;

    /** Flow control continue-to-send flag. */
    private static final int FC_CONTINUE = 0x00;

    /** Default block size (0 = unlimited). */
    private static final int DEFAULT_BLOCK_SIZE = 0;

    /** Default minimum separation time in milliseconds. */
    private static final int DEFAULT_ST_MIN = 10;

    /** Default transmit CAN ID for KWP2000 over CAN. */
    private static final int DEFAULT_TX_ID = 0x600;

    /** Default receive CAN ID for KWP2000 over CAN. */
    private static final int DEFAULT_RX_ID = 0x608;

    /** KWP2000 negative response service identifier. */
    private static final int NEGATIVE_RESPONSE_SID = 0x7F;

    /** P2 timing: maximum response time in milliseconds. */
    private int p2Timeout = 50;

    /** P3 timing: session idle timeout in milliseconds. */
    private int p3Timeout = 5000;

    private final CanBus canBus;
    private final int txId;
    private final int rxId;

    /**
     * Creates a KWP2000 protocol instance using default CAN IDs (0x600 / 0x608).
     *
     * @param canBus the CAN bus transport
     */
    public Kwp2000Protocol(CanBus canBus) {
        this(canBus, DEFAULT_TX_ID, DEFAULT_RX_ID);
    }

    /**
     * Creates a KWP2000 protocol instance with explicit CAN IDs.
     *
     * @param canBus the CAN bus transport
     * @param txId   the transmit CAN identifier
     * @param rxId   the receive CAN identifier
     */
    public Kwp2000Protocol(CanBus canBus, int txId, int rxId) {
        if (canBus == null) {
            throw new IllegalArgumentException("CAN bus must not be null");
        }
        this.canBus = canBus;
        this.txId = txId;
        this.rxId = rxId;
    }

    @Override
    public void connect() throws IOException {
        if (!canBus.isOpen()) {
            canBus.open();
        }
        canBus.addFilter(rxId, 0x7FF);
        connected = true;

        // Send StartCommunication service to initiate KWP session
        sendRequest(new byte[]{(byte) KwpServiceId.START_COMMUNICATION.getId()});
        byte[] response = readResponse();
        int responseSid = response[0] & 0xFF;
        if (responseSid == NEGATIVE_RESPONSE_SID) {
            connected = false;
            throw new IOException("ECU rejected StartCommunication request");
        }
    }

    @Override
    public void disconnect() throws IOException {
        if (connected) {
            try {
                sendRequest(new byte[]{(byte) KwpServiceId.STOP_COMMUNICATION.getId()});
                readResponse();
            } catch (IOException ignored) {
                // Best-effort disconnect
            } finally {
                connected = false;
                canBus.clearFilters();
            }
        }
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.KWP2000;
    }

    @Override
    public void sendRequest(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Request data must not be null or empty");
        }

        if (data.length <= SINGLE_FRAME_MAX_DATA) {
            sendSingleFrame(data);
        } else {
            sendMultiFrame(data);
        }
    }

    @Override
    public byte[] readResponse() throws IOException {
        CanFrame frame = canBus.receiveFrame(timeoutMs);
        if (frame == null) {
            throw new IOException("Timeout waiting for KWP2000 response");
        }

        byte[] frameData = frame.getData();
        if (frameData.length == 0) {
            throw new IOException("Received empty CAN frame");
        }

        int pciType = frameData[0] & 0xF0;

        switch (pciType) {
            case FRAME_TYPE_SINGLE:
                return parseSingleFrame(frameData);

            case FRAME_TYPE_FIRST:
                return receiveMultiFrame(frameData);

            default:
                throw new IOException(String.format(
                        "Unexpected ISO-TP frame type: 0x%02X", pciType));
        }
    }

    /**
     * Sets the P2 timing parameter (maximum time between request and response).
     *
     * @param ms timeout in milliseconds
     */
    public void setP2Timeout(int ms) {
        if (ms <= 0) {
            throw new IllegalArgumentException("P2 timeout must be positive");
        }
        this.p2Timeout = ms;
    }

    /**
     * Sets the P3 timing parameter (session idle timeout).
     *
     * @param ms timeout in milliseconds
     */
    public void setP3Timeout(int ms) {
        if (ms <= 0) {
            throw new IllegalArgumentException("P3 timeout must be positive");
        }
        this.p3Timeout = ms;
    }

    /**
     * Returns the current P2 timeout value.
     *
     * @return P2 timeout in milliseconds
     */
    public int getP2Timeout() {
        return p2Timeout;
    }

    /**
     * Returns the current P3 timeout value.
     *
     * @return P3 timeout in milliseconds
     */
    public int getP3Timeout() {
        return p3Timeout;
    }

    // -------------------------------------------------------------------------
    // ISO-TP framing – transmit
    // -------------------------------------------------------------------------

    private void sendSingleFrame(byte[] data) throws IOException {
        byte[] frame = new byte[8];
        frame[0] = (byte) (FRAME_TYPE_SINGLE | (data.length & 0x0F));
        System.arraycopy(data, 0, frame, 1, data.length);
        for (int i = 1 + data.length; i < 8; i++) {
            frame[i] = (byte) 0xCC;
        }
        canBus.sendFrame(new CanFrame(txId, frame));
    }

    private void sendMultiFrame(byte[] data) throws IOException {
        int totalLength = data.length;

        // First Frame
        byte[] ff = new byte[8];
        ff[0] = (byte) (FRAME_TYPE_FIRST | ((totalLength >> 8) & 0x0F));
        ff[1] = (byte) (totalLength & 0xFF);
        int firstChunk = Math.min(6, data.length);
        System.arraycopy(data, 0, ff, 2, firstChunk);
        canBus.sendFrame(new CanFrame(txId, ff));

        // Wait for Flow Control
        CanFrame fcFrame = canBus.receiveFrame(timeoutMs);
        if (fcFrame == null) {
            throw new IOException("Timeout waiting for flow control frame");
        }

        byte[] fcData = fcFrame.getData();
        if ((fcData[0] & 0xF0) != FRAME_TYPE_FLOW_CONTROL) {
            throw new IOException(String.format(
                    "Expected flow control frame, got PCI 0x%02X", fcData[0] & 0xFF));
        }

        int blockSize = fcData.length > 1 ? (fcData[1] & 0xFF) : 0;
        int stMin = fcData.length > 2 ? (fcData[2] & 0xFF) : DEFAULT_ST_MIN;

        // Consecutive Frames
        int offset = firstChunk;
        int sequenceNumber = 1;
        int blockCount = 0;

        while (offset < data.length) {
            byte[] cf = new byte[8];
            cf[0] = (byte) (FRAME_TYPE_CONSECUTIVE | (sequenceNumber & 0x0F));
            int chunkSize = Math.min(7, data.length - offset);
            System.arraycopy(data, offset, cf, 1, chunkSize);
            for (int i = 1 + chunkSize; i < 8; i++) {
                cf[i] = (byte) 0xCC;
            }

            canBus.sendFrame(new CanFrame(txId, cf));
            offset += chunkSize;
            sequenceNumber = (sequenceNumber + 1) & 0x0F;
            blockCount++;

            if (blockSize > 0 && blockCount >= blockSize && offset < data.length) {
                CanFrame nextFc = canBus.receiveFrame(timeoutMs);
                if (nextFc == null) {
                    throw new IOException("Timeout waiting for flow control after block");
                }
                blockCount = 0;
            }

            if (stMin > 0 && offset < data.length) {
                try {
                    Thread.sleep(stMin);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during ISO-TP transmission", e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ISO-TP framing – receive
    // -------------------------------------------------------------------------

    private byte[] parseSingleFrame(byte[] frameData) {
        int length = frameData[0] & 0x0F;
        if (length == 0 || length > SINGLE_FRAME_MAX_DATA) {
            return Arrays.copyOfRange(frameData, 1, frameData.length);
        }
        return Arrays.copyOfRange(frameData, 1, 1 + length);
    }

    private byte[] receiveMultiFrame(byte[] firstFrameData) throws IOException {
        int totalLength = ((firstFrameData[0] & 0x0F) << 8) | (firstFrameData[1] & 0xFF);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(totalLength);
        int firstChunk = Math.min(6, totalLength);
        buffer.write(firstFrameData, 2, firstChunk);

        // Send Flow Control
        byte[] fc = new byte[8];
        fc[0] = (byte) (FRAME_TYPE_FLOW_CONTROL | FC_CONTINUE);
        fc[1] = (byte) DEFAULT_BLOCK_SIZE;
        fc[2] = (byte) DEFAULT_ST_MIN;
        canBus.sendFrame(new CanFrame(txId, fc));

        // Receive Consecutive Frames
        int expectedSeq = 1;

        while (buffer.size() < totalLength) {
            CanFrame cfFrame = canBus.receiveFrame(timeoutMs);
            if (cfFrame == null) {
                throw new IOException(String.format(
                        "Timeout waiting for consecutive frame (received %d of %d bytes)",
                        buffer.size(), totalLength));
            }

            byte[] cfData = cfFrame.getData();
            if ((cfData[0] & 0xF0) != FRAME_TYPE_CONSECUTIVE) {
                throw new IOException(String.format(
                        "Expected consecutive frame, got PCI 0x%02X", cfData[0] & 0xFF));
            }

            int seq = cfData[0] & 0x0F;
            if (seq != expectedSeq) {
                throw new IOException(String.format(
                        "Consecutive frame sequence error: expected %d, got %d",
                        expectedSeq, seq));
            }

            int remaining = totalLength - buffer.size();
            int chunkSize = Math.min(7, remaining);
            buffer.write(cfData, 1, chunkSize);

            expectedSeq = (expectedSeq + 1) & 0x0F;
        }

        return buffer.toByteArray();
    }
}
