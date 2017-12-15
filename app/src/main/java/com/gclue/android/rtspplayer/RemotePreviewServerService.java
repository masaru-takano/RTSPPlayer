package com.gclue.android.rtspplayer;


import android.net.Uri;

import com.gclue.android.rtspplayer.deviceconnect.DConnectInterface;
import com.gclue.android.rtspplayer.deviceconnect.DConnectService;

import org.deviceconnect.message.DConnectMessage;
import org.deviceconnect.message.DConnectResponseMessage;
import org.deviceconnect.message.DConnectSDK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RemotePreviewServerService {

    private final DConnectInterface mDConnect;
    private final DConnectService mService;

    private List<StreamInfo> mStreamList;

    RemotePreviewServerService(final DConnectInterface dConnect, final DConnectService service) {
        mDConnect = dConnect;
        mService = service;
    }

    public String getName() {
        return mService.getName();
    }

    void setup(final PreviewParameters params, final ServerSetupListener listener) {
        if (params == null) {
            throw new NullPointerException("params is null.");
        }

        Map<String, String> map = new HashMap<>();
        map.put("mimeType", "image/png");
        map.put("previewWidth", Integer.toString(params.getWidth()));
        map.put("previewHeight", Integer.toString(params.getHeight()));
        map.put("previewMaxFrameRate", Float.toString(params.getMaxFrameRate()));
        map.put("previewBitRate", Integer.toString(params.getBitRate()));

        mDConnect.put(mService,
                "mediaStreamRecording",
                null,
                "options",
                map,
                new DConnectSDK.OnResponseListener() {
                    @Override
                    public void onResponse(final DConnectResponseMessage response) {
                        if (response.getResult() != DConnectMessage.RESULT_OK) {
                            listener.onError(response.getErrorCode(), response.getErrorMessage());
                            return;
                        }
                        listener.onSuccess();
                    }
                });
    }

    void launch(final ServerLaunchListener listener) {
        mDConnect.put(mService,
                "mediaStreamRecording",
                null,
                "preview",
                new DConnectSDK.OnResponseListener() {
                    @Override
                    public void onResponse(final DConnectResponseMessage response) {
                        if (response.getResult() != DConnectMessage.RESULT_OK) {
                            listener.onError(response.getErrorCode(), response.getErrorMessage());
                            return;
                        }
                        List<StreamInfo> streamList = parseStreamList(response);
                        mStreamList = streamList;
                        listener.onSuccess(mStreamList);
                    }
                });
    }

    private List<StreamInfo> parseStreamList(final DConnectResponseMessage response) {
        List<Object> list = response.getList("streams");
        List<StreamInfo> streamList = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof DConnectMessage) {
                DConnectMessage message = (DConnectMessage) obj;
                String mimeType = message.getString("mimeType");
                String uri = message.getString("uri");
                if (mimeType != null && uri != null) {
                    StreamInfo stream = new StreamInfo(mimeType, uri);
                    streamList.add(stream);
                }
            }
        }
        return streamList;
    }

    void shutdown(final ServerShutdownListener listener) {
        mDConnect.delete(mService,
                "mediaStreamRecording",
                null,
                "preview",
                new DConnectSDK.OnResponseListener() {
                    @Override
                    public void onResponse(final DConnectResponseMessage response) {
                        if (response.getResult() != DConnectMessage.RESULT_OK) {
                            listener.onError(response.getErrorCode(), response.getErrorMessage());
                            return;
                        }
                        mStreamList = null;
                        listener.onSuccess();
                    }
                });
    }

    interface ServerSetupListener extends ServerListener {
        void onSuccess();
    }

    interface ServerLaunchListener extends ServerListener {
        void onSuccess(List<StreamInfo> streamList);
    }

    interface ServerShutdownListener extends ServerListener {
        void onSuccess();
    }

    interface ServerListener {
        void onError(final int code, final String message);
    }

    static class StreamInfo {
        final String mMimeType;
        final Uri mUri;

        StreamInfo(final String mimeType, final String uri) {
            mMimeType = mimeType;
            mUri = Uri.parse(uri);
        }

        public String getMimeType() {
            return mMimeType;
        }

        public Uri getUri() {
            return mUri;
        }
    }
}
