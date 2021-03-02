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

package com.ibm.cloud.appconfiguration.sdk.feature.models.internal;

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONArray;
import org.json.JSONObject;

public class SegmentRules {

    Integer order;
    Object value;
    JSONArray rules;

    public SegmentRules(JSONObject segmentRulesJson) {
        try {
            this.order = segmentRulesJson.getInt("order");
            this.value = segmentRulesJson.get("value");
            this.rules = segmentRulesJson.getJSONArray("rules");

        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "SegmentRules.init", e, new Object[] {"Invalid action in SegmentRules class."});
        }
    }

    public Integer getOrder() {
        return order;
    }

    public Object getValue() {
        return value;
    }

    public JSONArray getRules() {
        return rules;
    }
}
