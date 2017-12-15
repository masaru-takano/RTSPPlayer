package com.gclue.android.rtspplayer.deviceconnect;


import android.content.Context;
import android.net.Uri;

import org.deviceconnect.message.DConnectMessage;
import org.deviceconnect.message.DConnectResponseMessage;
import org.deviceconnect.message.DConnectSDK;
import org.deviceconnect.message.DConnectSDKFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DConnectInterface {

    private DConnectSDK mDConnect;

    public DConnectInterface(final Context context) {
        mDConnect = DConnectSDKFactory.create(context, DConnectSDKFactory.Type.HTTP);
    }

    public void getServiceList(final ServiceListListener listener) {
        mDConnect.serviceDiscovery(new DConnectSDK.OnResponseListener() {
            @Override
            public void onResponse(final DConnectResponseMessage response) {
                if (response.getResult() != DConnectMessage.RESULT_OK) {
                    listener.onError(response.getErrorCode(), response.getErrorMessage());
                    return;
                }

                List<Object> list = response.getList("services");
                List<DConnectService> serviceList = new ArrayList<>();
                for (Object obj : list) {
                    if (obj instanceof DConnectMessage) {
                        DConnectMessage message = (DConnectMessage) obj;
                        String id = message.getString("id");
                        String name = message.getString("name");

                        DConnectService service = new DConnectService(id, name);
                        serviceList.add(service);
                    }
                }

                listener.onSuccess(serviceList);
            }
        });
    }

    public void getServiceInformation(final DConnectService service,
                                      final ServiceInformationListener listener) {
        throw new UnsupportedOperationException();
    }

    public void put(final DConnectService service,
                    final String profileName,
                    final String interfaceName,
                    final String attributeName,
                    final DConnectSDK.OnResponseListener listener) {
        put(service, profileName, interfaceName, attributeName, null, listener);
    }

    public void put(final DConnectService service,
                    final String profileName,
                    final String interfaceName,
                    final String attributeName,
                    final Map<String, String> params,
                    final DConnectSDK.OnResponseListener listener) {
        DConnectSDK.URIBuilder builder = mDConnect.createURIBuilder();
        builder.setProfile(profileName);
        builder.setInterface(interfaceName);
        builder.setAttribute(attributeName);
        builder.setServiceId(service.getId());
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addParameter(entry.getKey(), entry.getValue());
            }
        }

        mDConnect.put(builder.build(), null, listener);
    }

    public void delete(final DConnectService service,
                    final String profileName,
                    final String interfaceName,
                    final String attributeName,
                    final DConnectSDK.OnResponseListener listener) {
        DConnectSDK.URIBuilder builder = mDConnect.createURIBuilder();
        builder.setProfile(profileName);
        builder.setInterface(interfaceName);
        builder.setAttribute(attributeName);
        builder.setServiceId(service.getId());

        mDConnect.delete(builder.build(), listener);
    }

    public interface ServiceListListener extends DConnectInterfaceListener {
        void onSuccess(List<DConnectService> serviceList);
    }

    public interface ServiceInformationListener extends DConnectInterfaceListener {
        void onSuccess();
    }

    public interface DConnectInterfaceListener {
        void onError(int errorCode, String errorMessage);
    }
}
