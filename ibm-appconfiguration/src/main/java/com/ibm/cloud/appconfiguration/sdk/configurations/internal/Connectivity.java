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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.net.Socket;

public class Connectivity {

    private static Connectivity instance;
    private List<ConnectivityListener> listeners = new ArrayList<>();

    public synchronized static Connectivity getInstance() {
        if (instance == null) {
            instance = new Connectivity();
            instance.start();
        }
        return instance;
    }

    private Connectivity(){}

    public void addConnectivityListener(ConnectivityListener listener) {
        listeners.add(listener);
    }

    private void start() {
        new RetryHandler(new RetryInterface() {
            @Override
            public void retryMethod() {
                checkConnection();
            }
        }, 30000);
    }

    public void checkConnection() {
        Boolean connected = false;
        Socket socket = new Socket();
        try {
            InetSocketAddress address = new InetSocketAddress("cloud.ibm.com", 80);
            socket.connect(address, 500);
            connected = socket.isConnected();
        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "checkConnection", e, new Object[] {"Exception in checking network connection."});
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                AppConfigException.logException(this.getClass().getName(), "checkConnection", e, new Object[] {"Exception in closing network connection."});
            }
        }
        if (connected) {
            listeners.forEach((listener -> listener.onConnectionChange(true)));
        } else {
            listeners.forEach((listener -> listener.onConnectionChange(false)));
        }
    }
}
