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

package com.ibm.cloud.appconfiguration.sdk.test.configurations.internal;

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Socket;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.SocketHandler;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocketTest {

    private SocketHandler socketHandler;
    private Boolean calledOpen = false;
    private Boolean calledClose = false;
    private Boolean calledMessage = false;
    private Boolean calledError = false;

    private SocketHandler getSocketHandler() {

        if (socketHandler == null) {
            socketHandler = new SocketHandler() {
                @Override
                public void onOpen(String openMessage) {
                    calledOpen = true;
                }

                @Override
                public void onMessage(String message) {
                    calledMessage = true;
                }

                @Override
                public void onClose(String closeMessage) {
                    calledClose = true;
                }

                @Override
                public void onError(Exception e) {
                    calledError = true;
                }
            };
        }
        return socketHandler;
    }

    @Test
    public void testSocket() {
       Socket socket = new Socket.Builder().url("url").headers(new HashMap<>()).listener(this.getSocketHandler()).build();
       socket.onClose(200,"",true);
       socket.onMessage("");
       socket.onOpen(null);
       socket.onError(null);

       assertTrue(calledOpen);
        assertTrue(calledMessage);
        assertTrue(calledClose);
        assertTrue(calledError);

    }



}
