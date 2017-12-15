package com.gclue.android.rtspplayer;


import com.gclue.android.rtspplayer.deviceconnect.DConnectInterface;
import com.gclue.android.rtspplayer.deviceconnect.DConnectService;

import java.util.ArrayList;
import java.util.List;

class RemotePreviewServerServiceFinder {

    private final DConnectInterface mDConnect;

    RemotePreviewServerServiceFinder(final DConnectInterface dConnect) {
        mDConnect = dConnect;
    }

    void find(final Callback listener) {
        mDConnect.getServiceList(new DConnectInterface.ServiceListListener() {
            @Override
            public void onSuccess(final List<DConnectService> serviceList) {
                List<RemotePreviewServerService> result = new ArrayList<>();
                for (DConnectService service : serviceList) {
                    if (service.getName().toLowerCase().contains("host")) {
                        result.add(new RemotePreviewServerService(mDConnect, service));
                    }
                }
                listener.onFinish(result);
            }

            @Override
            public void onError(final int errorCode, final String errorMessage) {
                listener.onError();
            }
        });
    }

    interface Callback {
        void onFinish(List<RemotePreviewServerService> services);
        void onError();
    }

}
