package com.gclue.android.rtspplayer.core.rtsp;


import com.gclue.android.rtspplayer.core.rtp.RTPPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public abstract class Session {

    private final String mSessionId;

    private final String mAbsoluteUri;

    private final DatagramSocket mStreamSocket;

    private Thread mUdpThread;

    public Session(final String sessionId,
                   final DatagramSocket streamSocket,
                   final String absoluteUri)  {
        if (sessionId == null) {
            throw new NullPointerException("sessionId is null.");
        }
        if (streamSocket == null) {
            throw new NullPointerException("streamSocket is null.");
        }
        if (absoluteUri == null) {
            throw new NullPointerException("absoluteUri is null.");
        }
        mSessionId = sessionId;
        mStreamSocket = streamSocket;
        mAbsoluteUri = absoluteUri;
    }

    public String getUri() {
        return mAbsoluteUri;
    }

    public String getId() {
        return mSessionId;
    }

    public void startStreaming() throws SocketException {
        synchronized (this) {
            if (!isStreaming()) {
                mUdpThread = new Thread(new Streaming());
                mUdpThread.start();
            }
        }
    }

    public void stopStreaming() {
        synchronized (this) {
            if (isStreaming()) {
                mUdpThread.interrupt();
                mUdpThread = null;
            }
        }
    }

    public void dispose() throws IOException {
        synchronized (this) {
            stopStreaming();
        }
    }

    public boolean isStreaming() {
        return mUdpThread != null;
    }

    public abstract void onReceiveRTPPacket(final String sessionId, final RTPPacket packet);

    class Streaming implements Runnable {

        static final int BUFFER_SIZE = 1024 * 1024;

        private final byte[] mBuffer = new byte[BUFFER_SIZE];

        private long mInterval = 0; // ms.

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    DatagramPacket packet = new DatagramPacket(mBuffer, mBuffer.length);
                    mStreamSocket.receive(packet);
                    onReceiveRTPPacket(mSessionId, new RTPPacket(packet.getData()));

                    try {
                        Thread.sleep(mInterval);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
