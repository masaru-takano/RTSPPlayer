package com.gclue.android.rtspplayer.core.rtsp;


import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

class RTSPSocket {

    private static final String LOG_TAG = "RTSPClient";

    private final Socket mSocket;
    private final OutputStreamWriter mRequestWriter;
    private final RTSPResponseReader mResponseReader;

    private Thread mCheckThread;

    public RTSPSocket(final String host, final int port) throws IOException {
        InetSocketAddress address = new InetSocketAddress(host, port);
        mSocket = new Socket();
        //mSocket.setKeepAlive(true);
        mSocket.setTcpNoDelay(true);
        mSocket.setSoTimeout(0);
        mSocket.connect(address, 1000);
        mRequestWriter = new OutputStreamWriter(mSocket.getOutputStream());
        mResponseReader = new RTSPResponseReader(mSocket.getInputStream());

        mCheckThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        Log.d(LOG_TAG, "*** Socket:"
                                + " connected=" + mSocket.isConnected()
                                + " input=" + mSocket.isInputShutdown()
                                + " output=" + mSocket.isOutputShutdown()
                                + " closed=" +  mSocket.isClosed()
                                + " bound=" + mSocket.isBound());

                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    // NOP.
                }
            }
        });
        //mCheckThread.start();
    }

    public void close() {
        try {
            mCheckThread.interrupt();
            mCheckThread = null;

            mSocket.close();
        } catch (IOException e) {
            // NOP.
            e.printStackTrace();
        }
    }

    RTSPResponseReader.Result read() throws IOException {
        return mResponseReader.read();
    }

    void write(final String line) throws IOException {
        Log.d(LOG_TAG, "write: " + line);

        mRequestWriter.write(line);
    }

    void flush() throws IOException {
        mRequestWriter.flush();
    }

}
