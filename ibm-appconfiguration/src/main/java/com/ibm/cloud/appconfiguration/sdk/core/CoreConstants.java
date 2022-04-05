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

package com.ibm.cloud.appconfiguration.sdk.core;

/**
 * Class consisting of various member variables used as constants by the SDK.
 */
public class CoreConstants {

    private CoreConstants() { }

    public static final Integer REQUEST_SUCCESS_200 = 200;
    public static final Integer REQUEST_SUCCESS_202 = 202;
    public static final Integer REQUEST_SUCCESS_299 = 299;
    public static final Integer REQUEST_ERROR_AUTH = 401;
    public static final Integer REQUEST_ERROR = 400;
    public static final Integer TOO_MANY_REQUESTS = 429;
    public static final Integer REQUEST_ERROR_NOT_SUPPORTED = 405;
    public static final Integer SERVER_ERROR_BEGIN = 500;
    public static final Integer SERVER_ERROR_END = 599;
    public static final Integer MAX_NO_OF_RETRIES = 3;
    public static final Integer MAX_RETRY_INTERVAL = 30; // (in seconds) The maximum interval between two successive retry attempts
}
