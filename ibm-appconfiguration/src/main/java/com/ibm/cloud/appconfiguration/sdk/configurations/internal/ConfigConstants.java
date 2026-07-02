/**
 * Copyright 2021 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.cloud.appconfiguration.sdk.configurations.internal;

/**
 * Class consisting of various member variables used as constants by the SDK.
 */
public class ConfigConstants {

    private ConfigConstants() {
    }


    public static final int USAGE_SEND_INTERVAL = 600000;
    public static final String DELIMITER = "\u001F";
    public static final String JSON_CONTENT = "application/json";
    public static final String EMPTY_STRING = "";
    public static final String DEFAULT_SEGMENT_ID = "$$null$$";
    public static final String DEFAULT_ENTITY_ID = "$$null$$";
    public static final int DEFAULT_USAGE_LIMIT = 30;
    public static final String DEFAULT_SERVICE_NAME = "app_configuration";
    public static final String EVALUATED_SEGMENT_ID = "evaluated_segment_id";
    public static final String COLLECTION_ID = "collection_id";
    public static final String ENVIRONMENT_ID = "environment_id";
    public static final String VALUE = "value";
    public static final String PROPERTY_ID = "property_id";
    public static final String FEATURE_ID = "feature_id";
    public static final String PERSISTENCE_CACHE_FILE = "appconfiguration.json";
    public static final String FEATURE_ENABLED = "feature_enabled";
    public static final int DEFAULT_ROLLOUT_PERCENTAGE = 100;
    public static final int LOWEST_POSSIBLE_ROLLOUT_PERCENTAGE = 0;
    public static final int SEED = 0;
    public static final int OFFSET = 0;
    public static final double MAX_VAL = Math.pow(2, 32);
    public static final String IS_ENABLED = "is_enabled";
    public static final String CURRENT_VALUE = "current_value";
    public static final int CUSTOM_SOCKET_CLOSE_REASON_CODE = 4001;
    public static final String MANUAL = "MANUAL";
    public static final String PROGRESSIVE = "PROGRESSIVE";
    public static final String DEFAULT = "$default";
}
