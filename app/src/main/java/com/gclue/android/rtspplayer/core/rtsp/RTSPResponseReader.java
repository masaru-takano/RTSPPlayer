package com.gclue.android.rtspplayer.core.rtsp;


import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;


class RTSPResponseReader {

    private static final String LOG_TAG = "RTSPClient";

    private static final String SUPPORTED_VERSION = "RTSP/1.0";
    private static final String SP = " ";
    private static final String EMPTY = "";
    private static final String HEADER_SEPARATOR = ":";

    private final Reader mReader;

    RTSPResponseReader(final InputStream in) {
        mReader = new InputStreamReader(in);
    }

    private boolean supportsVersion(final String rtspVersion) {
        return SUPPORTED_VERSION.equals(rtspVersion);
    }

    private String readLine() throws IOException {
        int result;
        StringBuilder builder = new StringBuilder();
        while ((result = mReader.read()) != -1) {
            char ch = (char) result;
            if (ch == '\r') {
                continue;
            } else if (ch == '\n') {
                break;
            } else {
                builder.append(ch);
            }
        }
        if (result == -1) {
            throw new RTSPDisconnectedException();
        }
        String line = builder.toString();
        Log.d(LOG_TAG, "read: " + line);
        return line;
    }

    Result read() throws IOException {
        // Response Status-Line
        String statusLine = readLine();
        if (statusLine == null) {
            return new Result(Error.ILLEGAL_FORMAT, "Failed to read Response Status-Line.");
        }

        String rtspVersion;
        String status;
        String statusCode;
        String reasonPhrase;
        int index;
        if ((index = statusLine.indexOf(SP)) == -1) {
            return new Result(Error.ILLEGAL_FORMAT, "Illegal Format for Status-Line: " + statusLine);
        }
        rtspVersion = statusLine.substring(0, index);
        status = statusLine.substring(index + 1);
        if ((index = status.indexOf(SP)) == -1) {
            return new Result(Error.ILLEGAL_FORMAT, "Illegal Format for Status-Line: " + statusLine);
        }
        statusCode = status.substring(0, index);
        reasonPhrase = status.substring(index + 1);
        if (!supportsVersion(rtspVersion)) {
            return new Result(Error.UNSUPPORTED_VERSION, SUPPORTED_VERSION + " is only supported.");
        }

        // Response Headers
        Map<String, RTSPResponseHeader> headers = new HashMap<>();
        while (true) {
            String line = readLine();
            if (line == null || EMPTY.equals(line)) {
                break;
            }
            int headerIndex = line.indexOf(HEADER_SEPARATOR);
            if (headerIndex != -1) {
                String name = line.substring(0, headerIndex).trim();
                String value = line.substring(headerIndex + 1).trim();
                RTSPResponseHeader header = new RTSPResponseHeader(name, value);
                headers.put(name.toLowerCase(), header);
            } else {
                // TODO 不正なヘッダーをデバッグログに出す.
            }
        }

        RTSPResponseHeader cseqHeader = findHeader(headers, RTSPEntityHeader.Name.C_SEQ);
        if (cseqHeader == null) {
            return new Result(Error.ILLEGAL_FORMAT, "CSeq header is not found.");
        }
        int cSeq;
        try {
            cSeq = Integer.parseInt(cseqHeader.getValue());
        } catch (NumberFormatException e) {
            return new Result(Error.ILLEGAL_FORMAT, "CSeq header is not an integer.");
        }

        // Response Body
        char[] body;
        RTSPResponseHeader contentLengthHeader = findHeader(headers, RTSPEntityHeader.Name.CONTENT_LENGTH);
        if (contentLengthHeader != null) {
            int conetntLength = Integer.parseInt(contentLengthHeader.getValue());
            body = new char[conetntLength];
            if (mReader.read(body, 0, conetntLength) != conetntLength) {
                return new Result(Error.ILLEGAL_FORMAT, "Failed to read response body.");
            }
        } else {
            body = null;
        }

        RTSPResponse response = new RTSPResponse(
                new RTSPResponse.StatusLine(rtspVersion, statusCode, reasonPhrase),
                headers,
                body,
                cSeq
        );
        return new Result(response);
    }

    private static RTSPResponseHeader findHeader(final Map<String, RTSPResponseHeader> headers,
                                                 final RTSPEntityHeader.Name headerName) {
        return headers.get(headerName.getValue().toLowerCase());
    }

    static class Result {
        final RTSPResponse mResponse;
        final Error mError;
        final String mErrorMessage;

        Result(final RTSPResponse response) {
            mResponse = response;
            mError = null;
            mErrorMessage = null;
        }

        Result(final Error error, final String errorMessage) {
            mResponse = null;
            mError = error;
            mErrorMessage = errorMessage;
        }

        boolean isSuccess() {
            return mError == null;
        }

        Error getError() {
            return mError;
        }

        String getErrorMessage() {
            return mErrorMessage;
        }

        RTSPResponse getResponse() {
            return mResponse;
        }
    }

    enum Error {
        UNSUPPORTED_VERSION,
        ILLEGAL_FORMAT
    }

}
