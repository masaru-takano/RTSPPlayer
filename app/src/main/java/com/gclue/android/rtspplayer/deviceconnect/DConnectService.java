package com.gclue.android.rtspplayer.deviceconnect;


public class DConnectService {

    private final String mId;
    private final String mName;

    DConnectService(final String id, final String name) {
        mId = id;
        mName = name;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

}
