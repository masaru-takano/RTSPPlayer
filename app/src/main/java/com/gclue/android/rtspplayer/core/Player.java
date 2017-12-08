package com.gclue.android.rtspplayer.core;


import java.io.IOException;

public interface Player {

    void connect(String serverAddress, int port) throws IOException;

    void disconnect() throws IOException;

    void describe(String absoluteUri) throws IOException;

    void start(String absoluteUri) throws IOException;

    void stop() throws IOException;

    boolean isConnected();
}
