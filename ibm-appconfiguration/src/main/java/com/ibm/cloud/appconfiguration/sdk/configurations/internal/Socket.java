/**
 * Copyright 2021 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.cloud.appconfiguration.sdk.configurations.internal;

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

/**
 * Manage websocket events that are sent by App Configuration socket server.
 */
public class Socket extends WebSocketClient {

    SocketHandler listener;

    public Socket(Builder builder) {
        super(builder.serverUri, new Draft_6455(), builder.headers, 0);
        this.listener = builder.listener;
    }

    public void cancel() {
        super.close(ConfigConstants.CUSTOM_SOCKET_CLOSE_REASON_CODE);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.listener.onOpen("Opened the webSocket");
    }

    @Override
    public void onMessage(String message) {
        if (!message.equals("test message")) {
            this.listener.onMessage(message);
        } else {
            BaseLogger.debug("Received Heart beat from web socket");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (code != ConfigConstants.CUSTOM_SOCKET_CLOSE_REASON_CODE) {
            this.listener.onClose("Connection closed." + " Code: " + code + " Reason: " + reason);
        } else {
            // Don't to anything as it is the intentional close initiated by the client
            BaseLogger.debug("Connection closed by client." + " Code: " + code);
        }
    }

    @Override
    public void onError(Exception e) {
        this.listener.onError(e);
    }

    public static class Builder {
        private Map<String, String> headers;
        private URI serverUri;
        private SocketHandler listener;


        public final Socket.Builder url(String url) {

            try {
                this.serverUri = new URI(url);
            } catch (Exception e) {
                AppConfigException.logException(this.getClass().getName(), "Builder.url", e);
            }
            return this;
        }

        public final Socket.Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public final Socket.Builder listener(SocketHandler listener) {
            this.listener = listener;
            return this;
        }

        public Socket build() {
            return new Socket(this);
        }
    }
}
