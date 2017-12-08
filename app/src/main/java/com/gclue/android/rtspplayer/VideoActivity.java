package com.gclue.android.rtspplayer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.VideoView;


public class VideoActivity extends Activity {

    private VideoView mVideoView;

    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp_video);

        mVideoView = (VideoView) findViewById(R.id.video_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mVideoView.setVideoURI(Uri.parse("rtsp://localhost:8086"));
        mVideoView.requestFocus();
        mVideoView.start();
    }
}
