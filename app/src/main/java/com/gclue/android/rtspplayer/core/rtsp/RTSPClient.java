package com.gclue.android.rtspplayer.core.rtsp;


import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RTSPClient {

    private static final String LOG_TAG = "RTSPClient";

    private final String UA = "RTSPClient";
    private final SparseArray<RTSPRequest> mPendingRequest = new SparseArray<>();
    final String mHost;
    final int mPort;

    private int mCSeq = 1;
    private RTSPSocket mRTSPSocket;
    private ExecutorService mSenders;
    private Thread mReceiverThread;

    public RTSPClient(final String serverAddress, final int port) {
        mHost = serverAddress;
        mPort = port;
    }

    public synchronized void open() throws IOException {
        if (mRTSPSocket != null) {
            throw new IllegalStateException("Already open.");
        }
        mRTSPSocket = new RTSPSocket(mHost, mPort);

        mSenders = Executors.newFixedThreadPool(10);

        mReceiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        RTSPResponse response = receiveResponse();
                        int cSeq = response.getCSeq();
                        RTSPRequest request = mPendingRequest.get(cSeq);
                        if (request != null) {
                            mPendingRequest.remove(cSeq);
                            request.setResponse(response);
                        }

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // NOP.
                        return;
                    } catch (SocketTimeoutException e) {
                        Log.d(LOG_TAG, "Socket timeout.");
                        continue;
                    } catch (RTSPDisconnectedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mReceiverThread.start();
    }

    public RTSPResponse receiveResponse() throws IOException {
        Log.d(LOG_TAG, "receiveResponse:");
        RTSPSocket socket = mRTSPSocket;
        if (socket == null) {
            throw new RTSPDisconnectedException();
        }
        RTSPResponseReader.Result result = socket.read();
        if (!result.isSuccess()) {
            throw new IOException(result.getErrorMessage());
        }
        return result.getResponse();
    }

    public synchronized void close() {
        if (mRTSPSocket == null) {
            return;
        }
        mReceiverThread.interrupt();
        mReceiverThread = null;
        mSenders.shutdown();
        mSenders = null;
        mRTSPSocket.close();
        mRTSPSocket = null;
    }

    public boolean isOpen() {
        return mRTSPSocket != null;
    }

    public String getUserAgent() {
        return UA;
    }

    synchronized int nextCSeq() {
        return ++mCSeq;
    }

    private synchronized RTSPResponse sendRequest(final RTSPRequest request) throws IOException {
        if (!isOpen()) {
            open();
        }
        mSenders.execute(new Runnable() {
            @Override
            public void run() {
                mPendingRequest.put(request.getCSeq(), request);
                request.sendRequest();
            }
        });
        try {
            request.waitResponse();
            RTSPResponse response = request.getResponse();
            if (response == null) {
                throw new RTSPResponseTimeoutException();
            }
            return response;
        } catch (InterruptedException e) {
            return null;
        }
    }

    public RTSPResponse describe(final String uri) throws IOException {
        return sendRequest(new RTSPRequest(RTSPClient.this, mRTSPSocket) {
            @Override
            void run() throws IOException {
                writeRequestLine(Method.DESCRIBE, uri);
                writeCommonHeaders();
                writeHeaderEnd();
                flush();
            }

            @Override
            void onError(final Throwable error) {
                error.printStackTrace();
            }
        });
    }

    public RTSPResponse setup(final String uri, final int localPort) throws IOException {
        return sendRequest(new RTSPRequest(RTSPClient.this, mRTSPSocket) {
            @Override
            void run() throws IOException {
                writeRequestLine(Method.SETUP, uri);
                String transport = "RTP/AVP;unicast;client_port=" + localPort + "-" + (localPort + 1);
                writeHeaderLine(HEADER_TRANSPORT, transport);
                writeCommonHeaders();
                writeHeaderEnd();
                flush();
            }

            @Override
            void onError(final Throwable error) {
                error.printStackTrace();
            }
        });
    }

    public RTSPResponse play(final String uri, final String sessionId) throws IOException {
        return sendRequest(new RTSPRequest(RTSPClient.this, mRTSPSocket) {
            @Override
            void run() throws IOException {
                writeRequestLine(Method.PLAY, uri);
                writeCommonHeaders();
                writeHeaderLine(HEADER_SESSION, sessionId);
                writeHeaderEnd();
                flush();
            }

            @Override
            void onError(final Throwable error) {
                error.printStackTrace();
            }
        });
    }

    public RTSPResponse teardown(final String uri, final String sessionId) throws IOException {
        return sendRequest(new RTSPRequest(RTSPClient.this, mRTSPSocket) {
            @Override
            void run() throws IOException {
                writeRequestLine(Method.TEARDOWN, uri);
                writeCommonHeaders();
                writeHeaderLine(HEADER_SESSION, sessionId);
                writeHeaderEnd();
                flush();
            }

            @Override
            void onError(final Throwable error) {
                error.printStackTrace();
            }
        });
    }
}
