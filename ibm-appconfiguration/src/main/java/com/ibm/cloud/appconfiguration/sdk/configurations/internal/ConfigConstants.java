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

    public static final String DEFAULT_SEGMENT_ID = "$$null$$";
    public static final String DEFAULT_ENTITY_ID = "$$null$$";
    public static final String DEFAULT_HTTP_TYPE = "https://";
    public static final String DEFAULT_WSS_TYPE = "wss://";
    public static final String DEFAULT_BASE_URL = ".apprapp.cloud.ibm.com";
    public static final int DEFAULT_USAGE_LIMIT = 10;
    public static final String DEFAULT_SERVICE_NAME = "app_configuration";
    public static final String PROPERTIES = "properties";
    public static final String SEGMENTS = "segments";
    public static final String FEATURES = "features";
    public static final String EVALUATED_SEGMENT_ID = "evaluated_segment_id";
    public static final String EVALUATION_TIME = "evaluation_time";
    public static final String COLLECTION_ID = "collection_id";
    public static final String ENVIRONMENT_ID = "environment_id";
    public static final String USAGES = "usages";
    public static final String COUNT = "count";
    public static final String ENTITY_ID = "entity_id";
    public static final String SEGMENT_ID = "segment_id";
    public static final String ENABLED = "enabled";
    public static final String NAME = "name";
    public static final String RULES = "rules";
    public static final String VALUE = "value";
    public static final String PROPERTY_ID = "property_id";
    public static final String FEATURE_ID = "feature_id";
    public static final String SEGMENT_RULES = "segment_rules";
    public static final String TYPE = "type";
    public static final String ENABLED_VALUE = "enabled_value";
    public static final String DISABLED_VALUE = "disabled_value";
    public static final String FORMAT = "format";
    public static final String PERSISTENTCACHE_FILE = "appconfiguration.json";
    public static final String ROLLOUT_PERCENTAGE = "rollout_percentage";
    public static final String FEATURE_ENABLED = "feature_enabled";
    public static final int DEFAULT_ROLLOUT_PERCENTAGE = 100;
    public static final int SEED = 0;
    public static final int OFFSET = 0;
    public static final double MAX_VAL = Math.pow(2, 32);
    public static final String IS_ENABLED = "is_enabled";
    public static final String CURRENT_VALUE = "current_value";
    public static final int CUSTOM_SOCKET_CLOSE_REASON_CODE = 4001;

}
