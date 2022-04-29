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

/**
 * Class consisting of various methods that handles the internet connectivity status of the SDK.
 */
public class Connectivity {

    private static Connectivity instance;
    private List<ConnectivityListener> listeners = new ArrayList<>();
    private final String className = this.getClass().getName();

    public static synchronized Connectivity getInstance() {
        if (instance == null) {
            instance = new Connectivity();
            instance.checkConnection();
            instance.start();
        }
        return instance;
    }

    private Connectivity() {
    }

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

    /**
     * Check the internet connection by making ping to <a href="https://cloud.ibm.com">https://cloud.ibm.com</a>.
     */
    public void checkConnection() {
        boolean connected = false;
        Socket socket = new Socket();
        String methodName = "checkConnection";
        try {
            InetSocketAddress address = new InetSocketAddress("cloud.ibm.com", 80);
            socket.connect(address, 5000);
            connected = socket.isConnected();
        } catch (Exception e) {
            AppConfigException.logException(this.className, methodName, e,
                    new Object[]{"Exception in checking network connection."});
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                AppConfigException.logException(this.className, methodName, e,
                        new Object[]{"Exception in closing network connection."});
            }
        }
        if (connected) {
            listeners.forEach((listener -> listener.onConnectionChange(true)));
        } else {
            listeners.forEach((listener -> listener.onConnectionChange(false)));
        }
    }
}
