package com.gclue.android.rtspplayer;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.gclue.android.rtspplayer.core.OffscreenPlayer;
import com.gclue.android.rtspplayer.core.Player;
import com.gclue.android.rtspplayer.core.SimplePlayer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Player mPlayer;

    private ExecutorService mExecutor = Executors.newFixedThreadPool(10);

    private final Object mLock = new Object();

    private SurfaceView mVideoView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button describeButton = findViewById(R.id.player_button_describe);
        describeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mPlayer.describe(getAbsoluteUri());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        Button startButton = findViewById(R.id.player_button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            synchronized (mLock) {
                                if (!mPlayer.isConnected()) {
                                    mPlayer.connect(getIpV4Address(), getPort());
                                }
                                mPlayer.start(getAbsoluteUri());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        Button stopButton = findViewById(R.id.player_button_stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            synchronized (mLock) {
                                mPlayer.stop();
                                mPlayer.disconnect();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        mVideoView = findViewById(R.id.surface_view);

        try {
            mPlayer = new SimplePlayer(getApplicationContext(), mVideoView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private Uri mVideoUri = Uri.parse("rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_175k.mov");
    //private Uri mVideoUri = Uri.parse("rtsp://localhost:8086");
    //private Uri mVideoUri = Uri.parse("rtsp://b1.dnsdojo.com:1935/live/sys3.stream");
    //private Uri mVideoUri = Uri.parse("rtsp://stream2.taksimbilisim.com:1935/gtv/bant1");

    private String getIpV4Address() {
        return mVideoUri.getHost();
    }

    private int getPort() {
        return mVideoUri.getPort();
    }

    private String getAbsoluteUri() {
        return mVideoUri.toString();
    }
}
