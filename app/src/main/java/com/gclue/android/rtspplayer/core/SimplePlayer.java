package com.gclue.android.rtspplayer.core;


import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceView;

import com.c77.androidstreamingclient.lib.rtp.RtpMediaDecoder;
import com.gclue.android.rtspplayer.core.rtsp.RTSPClient;
import com.gclue.android.rtspplayer.core.rtsp.RTSPResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public class SimplePlayer implements Player {

    private static final String LOG_TAG = "RTSP Player";

    private RTSPClient mClient;

    private RtpMediaDecoder mMediaDecoder;

    private SessionInfo mSession;

    public SimplePlayer(final Context context, final SurfaceView surfaceView) throws IOException {
        Properties configuration = new Properties();
        configuration.load(context.getAssets().open("configuration.ini"));

        mMediaDecoder = new RtpMediaDecoder(surfaceView, configuration);
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
        RTSPResponse setupResponse = mClient.setup(absoluteUri + "/trackID=1", mMediaDecoder.getDataStreamingPort());
        Log.d(LOG_TAG, "Response Status Code: " + setupResponse.getStatusCode());
        switch (setupResponse.getStatusCode()) {
            case "200":
                Log.d(LOG_TAG, "Response Body: \n" + setupResponse.getContent());

                String sessionId = setupResponse.getHeader("Session");
                if (sessionId != null) {
                    int index = sessionId.indexOf(";");
                    if (index != -1) {
                        sessionId = sessionId.substring(0, index);
                    }

                    SessionInfo session = new SessionInfo(absoluteUri, sessionId);
                    onSetup1(session);
                } else {
                    Log.e(LOG_TAG, "Session ID is not specified: " + absoluteUri);
                }
                break;
            default:
                break;
        }
    }

    private void onSetup1(final SessionInfo session) throws IOException {
        RTSPResponse setupResponse = mClient.setup(session.getUri() + "/trackID=2", mMediaDecoder.getDataStreamingPort());
        Log.d(LOG_TAG, "Response Status Code: " + setupResponse.getStatusCode());
        switch (setupResponse.getStatusCode()) {
            case "200":
                Log.d(LOG_TAG, "Response Body: \n" + setupResponse.getContent());

                onSetup2(session);
                break;
            default:
                break;
        }
    }

    private void onSetup2(final SessionInfo session) throws IOException {
        RTSPResponse playResponse = mClient.play(session.getUri(), session.getId());
        Log.d(LOG_TAG, "Response Status Code: " + playResponse.getStatusCode());
        switch (playResponse.getStatusCode()) {
            case "200":
                mSession = session;
                mMediaDecoder.start();
                break;
            default:
                break;
        }
    }

    @Override
    public synchronized void stop() throws IOException {
        SessionInfo session = mSession;
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

    @Override
    public boolean isConnected() {
        return mClient != null;
    }

    @Override
    public void describe(final String absoluteUri) throws IOException {
        if (!isConnected()) {
            Uri uri = Uri.parse(absoluteUri);
            connect(uri.getHost(), uri.getPort());
        }

        try {
            RTSPResponse describeResponse = mClient.describe(absoluteUri);
            Log.d(LOG_TAG, "Response Status Code: " + describeResponse.getStatusCode());
            switch (describeResponse.getStatusCode()) {
                case "200":
                    Log.d(LOG_TAG, "Response Body: \n" + describeResponse.getContent());
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SessionInfo {
        private String mUri;
        private String mId;

        SessionInfo(final String uri, final String id) {
            mUri = uri;
            mId = id;
        }

        String getUri() {
            return mUri;
        }

        String getId() {
            return mId;
        }
    }
}
