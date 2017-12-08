package com.gclue.android.rtspplayer.core.rtsp;


import android.support.annotation.NonNull;

import java.util.Map;


public class RTSPResponse {

    private final StatusLine mStatusLine;
    private final Map<String, RTSPResponseHeader> mHeaders;
    private final char[] mBody;
    private final int mCSeq;

    RTSPResponse(final StatusLine statusLine,
                 final Map<String, RTSPResponseHeader> headers,
                 final char[] body,
                 final int cSeq) {
        mStatusLine = statusLine;
        mHeaders = headers;
        mBody = body;
        mCSeq = cSeq;
    }

    public String getStatusCode() {
        return mStatusLine.getStatusCode();
    }

    public boolean isOK() {
        return "200".equals(getStatusCode());
    }

    public int getCSeq() {
        return mCSeq;
    }

    public String getContent() {
        if (mBody == null) {
            return null;
        }
        return new String(mBody);
    }

    public int getContentLength() {
        if (mBody == null) {
            return 0;
        }
        return mBody.length;
    }

    public String getHeader(final @NonNull String headerName) {
        RTSPResponseHeader header = mHeaders.get(headerName.toLowerCase());
        if (header == null) {
            return null;
        }
        return header.getValue();
    }

    public static class StatusLine {
        final String mRtspVersion;
        final String mStatusCode;
        final String mReasonPhrase;

        StatusLine(final String rtspVersion,
                   final String statusCode,
                   final String reasonPhrase) {
            mRtspVersion = rtspVersion;
            mStatusCode = statusCode;
            mReasonPhrase = reasonPhrase;
        }

        public String getRtspVersion() {
            return mRtspVersion;
        }

        public String getStatusCode() {
            return mStatusCode;
        }

        public String getReasonPhrase() {
            return mReasonPhrase;
        }
    }
}
