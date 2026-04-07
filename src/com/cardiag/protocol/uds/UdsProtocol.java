package com.cardiag.protocol.uds;

import com.cardiag.protocol.DiagnosticProtocol;
import com.cardiag.protocol.ProtocolType;
import com.cardiag.protocol.can.CanBus;
import com.cardiag.protocol.can.CanFrame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * UDS (Unified Diagnostic Services) protocol implementation over CAN (ISO-TP).
 *
 * <p>This class handles ISO 15765-2 (ISO-TP) transport-layer segmentation
 * and reassembly, supporting single-frame, first-frame / consecutive-frame,
 * and flow-control message types.</p>
 *
 * <h3>ISO-TP Frame Types</h3>
 * <ul>
 *     <li><b>Single Frame (SF):</b> PCI byte = 0x0N, where N is the data length (1-7).</li>
 *     <li><b>First Frame (FF):</b> PCI bytes = 0x1H LL, total length up to 4095 bytes.</li>
 *     <li><b>Consecutive Frame (CF):</b> PCI byte = 0x2N, sequence index wraps 0-F.</li>
 *     <li><b>Flow Control (FC):</b> PCI byte = 0x30 + flow status, then BS, STmin.</li>
 * </ul>
 */
public class UdsProtocol extends DiagnosticProtocol {

    /** Maximum payload in a single CAN frame (single-frame ISO-TP). */
    private static final int SINGLE_FRAME_MAX_DATA = 7;

    /** ISO-TP frame type nibble values. */
    private static final int FRAME_TYPE_SINGLE = 0x00;
    private static final int FRAME_TYPE_FIRST = 0x10;
    private static final int FRAME_TYPE_CONSECUTIVE = 0x20;
    private static final int FRAME_TYPE_FLOW_CONTROL = 0x30;

    /** Flow control: continue to send. */
    private static final int FC_CONTINUE = 0x00;

    /** Default block size for flow control (0 = no limit). */
    private static final int DEFAULT_BLOCK_SIZE = 0;

    /** Default separation time minimum in milliseconds. */
    private static final int DEFAULT_ST_MIN = 10;

    private final CanBus canBus;
    private final int txId;
    private final int rxId;

    /**
     * Creates a UDS protocol instance.
     *
     * @param canBus the CAN bus transport
     * @param txId   the CAN identifier used for transmitting requests (e.g. 0x7E0)
     * @param rxId   the CAN identifier expected for responses (e.g. 0x7E8)
     */
    public UdsProtocol(CanBus canBus, int txId, int rxId) {
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
        // Set up a filter so we only receive frames addressed to us
        canBus.addFilter(rxId, 0x7FF);
        connected = true;
    }

    @Override
    public void disconnect() throws IOException {
        connected = false;
        canBus.clearFilters();
    }

    @Override
    public ProtocolType getProtocolType() {
        return ProtocolType.UDS;
    }

    /**
     * Sends a UDS request over ISO-TP.
     *
     * @param data the UDS request payload (service ID + parameters)
     * @throws IOException if transmission fails
     */
    @Override
    public void sendRequest(byte[] data) throws IOException {
        ensureConnected();

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Request data must not be null or empty");
        }

        if (data.length <= SINGLE_FRAME_MAX_DATA) {
            sendSingleFrame(data);
        } else {
            sendMultiFrame(data);
        }
    }

    /**
     * Reads a UDS response, reassembling multi-frame ISO-TP messages.
     *
     * @return the reassembled UDS response payload
     * @throws IOException if a communication error occurs or the operation times out
     */
    @Override
    public byte[] readResponse() throws IOException {
        ensureConnected();

        CanFrame frame = canBus.receiveFrame(timeoutMs);
        if (frame == null) {
            throw new IOException("Timeout waiting for UDS response");
        }

        byte[] frameData = frame.getData();
        if (frameData.length == 0) {
            throw new IOException("Received empty CAN frame");
        }

        int pciType = (frameData[0] & 0xF0);

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
     * Sends a UDS request and reads the response in one call.
     *
     * <p>If the ECU responds with NRC 0x78 (Response Pending), this method
     * will continue waiting for the final response.</p>
     *
     * @param data the UDS request payload
     * @return the parsed UDS response
     * @throws IOException if communication fails
     */
    public UdsResponse sendUdsRequest(byte[] data) throws IOException {
        sendRequest(data);

        while (true) {
            byte[] responseBytes = readResponse();
            UdsResponse response = UdsResponse.parse(responseBytes);

            // If NRC is ResponsePending, wait and read again
            if (!response.isPositiveResponse()
                    && response.getNrc() == NegativeResponseCode.RESPONSE_PENDING.getCode()) {
                continue;
            }

            return response;
        }
    }

    // -------------------------------------------------------------------------
    // ISO-TP framing – transmit
    // -------------------------------------------------------------------------

    /**
     * Sends a single-frame ISO-TP message.
     */
    private void sendSingleFrame(byte[] data) throws IOException {
        byte[] frame = new byte[8];
        frame[0] = (byte) (FRAME_TYPE_SINGLE | (data.length & 0x0F));
        System.arraycopy(data, 0, frame, 1, data.length);
        // Pad remaining bytes with 0xCC (ISO-TP padding)
        for (int i = 1 + data.length; i < 8; i++) {
            frame[i] = (byte) 0xCC;
        }
        canBus.sendFrame(new CanFrame(txId, frame));
    }

    /**
     * Sends a multi-frame ISO-TP message (first frame + consecutive frames),
     * handling flow control from the receiver.
     */
    private void sendMultiFrame(byte[] data) throws IOException {
        int totalLength = data.length;

        // First Frame: PCI = 0x1H LL, followed by first 6 data bytes
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

        int flowStatus = fcData[0] & 0x0F;
        if (flowStatus != FC_CONTINUE) {
            throw new IOException("Flow control: not cleared to send (status=" + flowStatus + ")");
        }

        int blockSize = fcData.length > 1 ? (fcData[1] & 0xFF) : 0;
        int stMin = fcData.length > 2 ? (fcData[2] & 0xFF) : DEFAULT_ST_MIN;

        // Send Consecutive Frames
        int offset = firstChunk;
        int sequenceNumber = 1;
        int blockCount = 0;

        while (offset < data.length) {
            byte[] cf = new byte[8];
            cf[0] = (byte) (FRAME_TYPE_CONSECUTIVE | (sequenceNumber & 0x0F));

            int chunkSize = Math.min(7, data.length - offset);
            System.arraycopy(data, offset, cf, 1, chunkSize);
            // Pad
            for (int i = 1 + chunkSize; i < 8; i++) {
                cf[i] = (byte) 0xCC;
            }

            canBus.sendFrame(new CanFrame(txId, cf));
            offset += chunkSize;
            sequenceNumber = (sequenceNumber + 1) & 0x0F;
            blockCount++;

            // If block size is non-zero, wait for another FC after each block
            if (blockSize > 0 && blockCount >= blockSize && offset < data.length) {
                CanFrame nextFc = canBus.receiveFrame(timeoutMs);
                if (nextFc == null) {
                    throw new IOException("Timeout waiting for flow control after block");
                }
                blockCount = 0;
            }

            // Respect separation time
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

    /**
     * Extracts the data payload from a single-frame ISO-TP message.
     */
    private byte[] parseSingleFrame(byte[] frameData) {
        int length = frameData[0] & 0x0F;
        if (length == 0 || length > SINGLE_FRAME_MAX_DATA) {
            return Arrays.copyOfRange(frameData, 1, frameData.length);
        }
        return Arrays.copyOfRange(frameData, 1, 1 + length);
    }

    /**
     * Receives and reassembles a multi-frame ISO-TP message. The first frame
     * has already been received and its data is passed in.
     */
    private byte[] receiveMultiFrame(byte[] firstFrameData) throws IOException {
        int totalLength = ((firstFrameData[0] & 0x0F) << 8) | (firstFrameData[1] & 0xFF);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(totalLength);

        // Copy data portion of the first frame (bytes 2-7)
        int firstChunk = Math.min(6, totalLength);
        buffer.write(firstFrameData, 2, firstChunk);

        // Send Flow Control: CTS, block size = 0 (unlimited), STmin = 10ms
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
