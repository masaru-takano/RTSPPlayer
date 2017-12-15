package com.gclue.android.rtspplayer;

class PreviewParameters {

    private final int mWidth;
    private final int mHeight;
    private final float mMaxFrameRate;
    private final int mBitRate;

    PreviewParameters(final int w, final int h, final float maxFps, final int bps) {
        mWidth = w;
        mHeight = h;
        mMaxFrameRate = maxFps;
        mBitRate = bps;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public float getMaxFrameRate() {
        return mMaxFrameRate;
    }

    public int getBitRate() {
        return mBitRate;
    }

    @Override
    public String toString() {
        return getWidth() + " x " + getHeight() + " / " + getMaxFrameRate() + " fps" + " / " + getBitRate() + " bps";
    }
}
