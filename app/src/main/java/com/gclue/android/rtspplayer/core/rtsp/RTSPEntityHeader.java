package com.gclue.android.rtspplayer.core.rtsp;


public abstract class RTSPEntityHeader {

    private final String mName;
    private final String mValue;

    RTSPEntityHeader(final String name,
                     final String value) {
        mName = name;
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public String getValue() {
        return mValue;
    }

    enum Name {
        CONTENT_LENGTH("Content-Length"),
        C_SEQ("CSeq"),
        SESSION("Session");

        private final String mValue;

        Name(final String value) {
            mValue = value;
        }

        String getValue() {
            return mValue;
        }
    }
}
