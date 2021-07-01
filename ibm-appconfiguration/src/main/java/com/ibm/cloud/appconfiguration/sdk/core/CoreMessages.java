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
 * Class consisting of various member variables used as logger messages by the SDK.
 */
public class CoreMessages {

    private CoreMessages() {

    }

    public static final String REQUEST_TYPE_NOT_SUPPORTED = "The request is not allowed. Only GET and POST is allowed.";
    public static final String REQUEST_CLIENT_INVALID = "The request is invalid.";
    public static final String REQUEST_RESPONSE_ERROR = "Error while fetching the data from response.";
    public static final String GLOBAL_LOGGER_NAME = "AppConfigurationLogger";
}
