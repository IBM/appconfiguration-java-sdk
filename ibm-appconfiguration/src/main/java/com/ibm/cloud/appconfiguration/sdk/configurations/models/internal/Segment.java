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

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Segment {

    String name;
    String segmentId;
    JSONArray rules;

    public Segment(JSONObject segmentJson) {
        try {
            this.name = segmentJson.getString("name");
            this.segmentId = segmentJson.getString("segment_id");
            this.rules = segmentJson.getJSONArray("rules");

        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "Segment.init", e, new Object[] {"Invalid action in Segment class. "});
        }
    }

    public JSONArray getRules() {
        return rules;
    }

    public String getName() {
        return name;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public Boolean evaluateRule(JSONObject identityAttributes) {

        for (int index = 0; index < this.rules.length(); index++) {
            try {
                Rule rule = new Rule(this.rules.getJSONObject(index));
                if (!rule.evaluateRule(identityAttributes)) {
                    return false;
                }
            } catch (Exception e) {
                AppConfigException.logException(this.getClass().getName(), "evaluateRule", e, new Object[] {"Invalid action in Segment class."});
            }
        }
        return true;
    }
}
