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

package com.ibm.cloud.appconfiguration.sdk.configurations.models.internal;

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Defines the model of a segment rule defined in App Configuration service.
 */
public class SegmentRules {

    Integer order;
    Object value;
    JSONArray rules;

    /**
     * @param segmentRulesJson segment_rules JSON object that contains all the segment rules
     */
    public SegmentRules(JSONObject segmentRulesJson) {
        try {
            this.order = segmentRulesJson.getInt("order");
            this.value = segmentRulesJson.get(ConfigConstants.VALUE);
            this.rules = segmentRulesJson.getJSONArray(ConfigConstants.RULES);

        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "SegmentRules.init", e, new Object[] {"Invalid action in SegmentRules class."});
        }
    }

    /**
     * @return the order of the rule in the segment_rules object
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * @return the value of the rule in segment_rules object
     */
    public Object getValue() {
        return value;
    }

    /**
     * @return the rules array of the segment_rules object
     */
    public JSONArray getRules() {
        return rules;
    }
}
