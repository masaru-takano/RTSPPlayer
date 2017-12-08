package com.gclue.android.rtspplayer.core.rtsp;


import java.io.IOException;

public abstract class RTSPRequest {

    enum Method {
        DESCRIBE,
        OPTIONS,
        SETUP,
        PLAY,
        PAUSE,
        TEARDOWN
    }

    static final String VERSION = "1.0";
    static final String HEADER_CONTENT_LENGTH = "Content-Length";
    static final String HEADER_C_SEQ = "CSeq";
    static final String HEADER_USER_AGENT = "User-Agent";
    static final String HEADER_TRANSPORT = "Transport";
    static final String HEADER_SESSION = "Session";

    private static final String CRLF = "\r\n";

    private final RTSPClient mRTSPClient;
    private final RTSPSocket mRTSPSocket;
    private final int mCSeq;
    private final String mUA;
    private final long mResponseTimeout = 60 * 1000; // ミリ秒
    private final Object mLock = new Object();

    private RTSPResponse mResponse;

    RTSPRequest(final RTSPClient client, final RTSPSocket socket) throws IOException {
        mRTSPClient = client;
        mRTSPSocket = socket;

        mCSeq = mRTSPClient.nextCSeq();
        mUA = mRTSPClient.getUserAgent();
    }

    public long getResponseTimeout() {
        return mResponseTimeout;
    }

    abstract void run() throws IOException;

    abstract void onError(final Throwable error);

    public void waitResponse() throws InterruptedException {
        synchronized (mLock) {
            mLock.wait(getResponseTimeout());
        }
    }

    public void setResponse(final RTSPResponse response) {
        synchronized (mLock) {
            mResponse = response;
            mLock.notifyAll();
        }
    }

    public RTSPResponse getResponse() {
        return mResponse;
    }

    public int getCSeq() {
        return mCSeq;
    }

    void writeCommonHeaders() throws IOException {
        writeCommonHeaders(0);
    }

    void writeCommonHeaders(int contentLength) throws IOException {
        writeHeaderLine(HEADER_C_SEQ, mCSeq);
        writeHeaderLine(HEADER_USER_AGENT, mUA);
        if (contentLength > 0) {
            writeHeaderLine(HEADER_CONTENT_LENGTH, contentLength);
        }
    }

    void writeRequestLine(final Method method, final String uri) throws IOException {
        write(method.name() + " " + uri + " RTSP/" + VERSION + CRLF);
    }

    void writeHeaderLine(final String name, final int value) throws IOException {
        writeHeaderLine(name, Integer.toString(value));
    }

    void writeHeaderLine(final String name, final String value) throws IOException {
        write(name + ": " + value + CRLF);
    }

    void writeHeaderEnd() throws IOException {
        write(CRLF);
    }

    void write(final String line) throws IOException {
        mRTSPSocket.write(line);
    }

    void flush() throws IOException {
        mRTSPSocket.flush();
    }

    void sendRequest() {
        try {
            run();
        } catch (Throwable error) {
            onError(error);
        }
    }
}
