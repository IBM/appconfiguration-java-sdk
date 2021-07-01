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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Retry handling in case of failures.
 */
public class RetryHandler {

    private Timer retryTimer;
    private RetryInterface retryInterface;
    private int retryInterval = 600000;

    public RetryHandler(RetryInterface retryInterface, int retryInterval) {
        if (retryInterval >= 0) {
            this.retryInterval = retryInterval;
        }
        this.retryInterface = retryInterface;
        this.startRetryTimer();
    }

    private synchronized void startRetryTimer() {

        if (this.retryTimer != null) {
            this.retryTimer.cancel();
            this.retryTimer = null;
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                retryInterface.retryMethod();
            }
        };
        this.retryTimer = new Timer();
        this.retryTimer.scheduleAtFixedRate(task, this.retryInterval, this.retryInterval);
    }

    public void cancel() {
        if (this.retryTimer != null) {
            this.retryTimer.cancel();
            this.retryTimer = null;
        }
    }
}
