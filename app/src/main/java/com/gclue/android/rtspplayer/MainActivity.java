package com.gclue.android.rtspplayer;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.gclue.android.rtspplayer.core.OffscreenPlayer;
import com.gclue.android.rtspplayer.core.Player;
import com.gclue.android.rtspplayer.deviceconnect.DConnectInterface;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;
    private static final int DEFAULT_FRAME_RATE = 30; // frame per second
    private static final int DEFAULT_BIT_RATE = 1024 * 1024; // bit per second

    private Player mPlayer;
    private ExecutorService mExecutor = Executors.newFixedThreadPool(10);
    private final Object mLock = new Object();
    private SurfaceView mVideoView;
    private EditText mWidthEdit;
    private EditText mHeightEdit;
    private EditText mFpsEdit;
    private EditText mBpsEdit;

    private long mTime = 0;
    private List<Entry> mChatEntryList = new ArrayList<>();
    private LineChart mLineChart;

    private RemotePreviewServerServiceFinder mServiceFinder;
    private RemotePreviewServerService mPreviewService;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWidthEdit = findViewById(R.id.edit_width);
        mWidthEdit.setText(Integer.toString(DEFAULT_WIDTH));

        mHeightEdit = findViewById(R.id.edit_height);
        mHeightEdit.setText(Integer.toString(DEFAULT_HEIGHT));

        mFpsEdit = findViewById(R.id.edit_fps);
        mFpsEdit.setText(Integer.toString(DEFAULT_FRAME_RATE));

        mBpsEdit = findViewById(R.id.edit_bps);
        mBpsEdit.setText(Integer.toString(DEFAULT_BIT_RATE));

        mLineChart = findViewById(R.id.chart_frame_rate);

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
                        setupPreview();
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
                            stopPlayer();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        mVideoView = findViewById(R.id.surface_view);

        try {
            mPlayer = new OffscreenPlayer() {

                @Override
                protected void onFrame(final long delta) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            synchronized (mChatEntryList) {
                                float frameRate = 1000f / delta;

                                if (++mTime < 0) {
                                    mTime = 0;
                                }

                                mChatEntryList.add(new Entry(mTime, frameRate));

                                if (mChatEntryList.size() > 100) {
                                    mChatEntryList.remove(0);
                                }

                                float total = 0;
                                for (int i = 0; i < mChatEntryList.size(); i++) {
                                    total += mChatEntryList.get(i).getY();
                                }
                                float average = total / mChatEntryList.size();

                                Log.d(TAG, "frame rate: " + average + " fps");
                            }

                            LineDataSet dataSet = new LineDataSet(mChatEntryList, "FrameRate");
                            LineData data = new LineData(dataSet);
                            mLineChart.setData(data);
                            mLineChart.invalidate();
                        }
                    });
                }
            };
            //mPlayer = new SimplePlayer(getApplicationContext(), mVideoView);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DConnectInterface dConnect = new DConnectInterface(getApplicationContext());
        mServiceFinder = new RemotePreviewServerServiceFinder(dConnect);
    }

    private PreviewParameters collectPreviewParameters() {
        try {
            int w = Integer.parseInt(mWidthEdit.getText().toString());
            int h = Integer.parseInt(mHeightEdit.getText().toString());
            float maxFps = (float) Integer.parseInt(mFpsEdit.getText().toString());
            int bps = Integer.parseInt(mBpsEdit.getText().toString());
            return new PreviewParameters(w, h, maxFps, bps);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setupPreview() {
        final PreviewParameters params = collectPreviewParameters();
        if (params == null) {
            Log.e(TAG, "Preview parameters is invalid.");
            return;
        }

        mPreviewService.setup(params, new RemotePreviewServerService.ServerSetupListener() {
            @Override
            public void onError(final int code, final String message) {
                Log.e(TAG, "Failed to setup preview service: name = " + mPreviewService.getName() + " error = (" + code + ", " + message + ")");
            }

            @Override
            public void onSuccess() {
                Log.i(TAG, "Current Params: " + params);


                mPreviewService.launch(new RemotePreviewServerService.ServerLaunchListener() {
                    @Override
                    public void onSuccess(final List<RemotePreviewServerService.StreamInfo> streamList) {
                        for (RemotePreviewServerService.StreamInfo stream : streamList) {
                            if (stream.getMimeType().equals("video/x-rtp")) {
                                Log.i(TAG, "RTP Stream: uri = " + stream.getUri());

                                try {
                                    startPlayer(stream.getUri());
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed to play preview stream: " + mPreviewService.getName());
                                }

                                return;
                            }
                        }
                        Log.e(TAG, "Remote preview stream over RTP is not found.");
                    }

                    @Override
                    public void onError(final int code, final String message) {
                        Log.e(TAG, "Failed to launch preview stream.");
                    }
                });
            }
        });
    }

    private void startPlayer(final Uri uri) throws IOException {
        synchronized (mLock) {
            if (!mPlayer.isConnected()) {
                mPlayer.connect(uri.getHost(), uri.getPort());
            }
            mPlayer.start(uri.toString());
        }
    }

    private void stopPlayer() throws IOException {
        synchronized (mLock) {
            mPlayer.stop();
            mPlayer.disconnect();
        }

        if (mPreviewService != null) {
            mPreviewService.shutdown(new RemotePreviewServerService.ServerShutdownListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Shutdown remote preview service: name = " + mPreviewService.getName());
                }

                @Override
                public void onError(int code, String message) {
                    Log.e(TAG, "Failed to shutdown remote preview service: name = " + mPreviewService.getName());
                }
            });
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTime = 0;
                mChatEntryList.clear();
                mLineChart.clear();
            }
        });
    }

    private void findStream() {
        mServiceFinder.find(new RemotePreviewServerServiceFinder.Callback() {
            @Override
            public void onFinish(final List<RemotePreviewServerService> services) {
                if (services.size() <= 0) {
                    Log.e(TAG, "Remote preview service is not found.");
                    return;
                }

                RemotePreviewServerService server = services.get(0);
                mPreviewService = server;
            }

            @Override
            public void onError() {
                Log.e(TAG, "Failed to find remote preview server.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        findStream();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    //private Uri mVideoUri = Uri.parse("rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_175k.mov");
    private Uri mVideoUri = Uri.parse("rtsp://localhost:8086");
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
