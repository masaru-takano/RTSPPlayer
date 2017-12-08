package com.gclue.android.rtspplayer.core;


import android.util.Log;

import com.gclue.android.rtspplayer.core.rtp.RTPPacket;
import com.gclue.android.rtspplayer.core.rtsp.RTSPClient;
import com.gclue.android.rtspplayer.core.rtsp.RTSPResponse;
import com.gclue.android.rtspplayer.core.rtsp.Session;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;

public class OffscreenPlayer implements Player {

    private static final String LOG_TAG = "RTSP Player";

    private final int RTP_PORT = 25000;

    private RTSPClient mClient;

    private final DatagramSocket mStreamSocket;

    private Session mSession;

    public OffscreenPlayer() throws IOException {
        mStreamSocket = new DatagramSocket(RTP_PORT);
    }

    @Override
    public synchronized void connect(final String serverAddress, final int port) throws IOException {
        if (isConnected()) {
            return;
        }
        mClient = new RTSPClient(serverAddress, port);
        mClient.open();
    }

    @Override
    public synchronized void disconnect() throws IOException {
        if (!isConnected()) {
            return;
        }
        mClient.close();
        mClient = null;
    }

    @Override
    public synchronized void start(final String absoluteUri) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("player is not connected to server.");
        }
        RTSPResponse describeResponse = mClient.describe(absoluteUri);
        Log.d(LOG_TAG, "Response Status Code: " + describeResponse.getStatusCode());
        switch (describeResponse.getStatusCode()) {
            case "200":
                Log.d(LOG_TAG, "Response Body: \n" + describeResponse.getContent());
                onDescribe(absoluteUri);
                break;
            default:
                break;
        }
    }

    private void onDescribe(final String absoluteUri) throws IOException {
        RTSPResponse setupResponse = mClient.setup(absoluteUri + "/trackID=1", mStreamSocket.getLocalPort());
        Log.d(LOG_TAG, "Response Status Code: " + setupResponse.getStatusCode());
        switch (setupResponse.getStatusCode()) {
            case "200":
                Log.d(LOG_TAG, "Response Body: \n" + setupResponse.getContent());

                String sessionId = setupResponse.getHeader("Session");
                if (sessionId != null) {
                    Session session = createSession(sessionId, absoluteUri);
                    onSetup(session);
                } else {
                    Log.e(LOG_TAG, "Session ID is not specified: " + absoluteUri);
                }
                break;
            default:
                break;
        }
    }

    private void onSetup(final Session session) throws IOException {
        RTSPResponse playResponse = mClient.play(session.getUri(), session.getId());
        Log.d(LOG_TAG, "Response Status Code: " + playResponse.getStatusCode());
        switch (playResponse.getStatusCode()) {
            case "200":
                session.startStreaming();
                mSession = session;
                break;
            default:
                break;
        }
    }

    private Session createSession(final String sessionId, final String absoluteUri) {
        return new Session(sessionId, mStreamSocket, absoluteUri) {
            @Override
            public void onReceiveRTPPacket(final String sessionId, final RTPPacket packet) {
                Log.d(LOG_TAG, "onReceiveRTPPacket: sessionId = " + sessionId);
            }
        };
    }

    @Override
    public synchronized void stop() throws IOException {
        synchronized (this) {
            Session session = mSession;
            if (session != null) {
                RTSPResponse describeResponse = mClient.teardown(session.getUri(), session.getId());
                Log.d(LOG_TAG, "Response Status Code: " + describeResponse.getStatusCode());
                switch (describeResponse.getStatusCode()) {
                    case "200":
                        break;
                    default:
                        break;
                }
            }
            mSession = null;
        }
    }

    @Override
    public boolean isConnected() {
        return mClient != null;
    }

    @Override
    public void describe(final String absoluteUri) throws IOException {
        // TODO
    }
}
