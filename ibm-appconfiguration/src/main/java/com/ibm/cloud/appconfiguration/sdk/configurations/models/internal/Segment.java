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
 *  Defines the model of a segment defined in App Configuration service.
 */
public class Segment {

    String name;
    String segmentId;
    JSONArray rules;
    private final String className = this.getClass().getName();

    /**
     * @param segmentJson segments JSON object that contains all the segments
     */
    public Segment(JSONObject segmentJson) {
        try {
            this.name = segmentJson.getString(ConfigConstants.NAME);
            this.segmentId = segmentJson.getString(ConfigConstants.SEGMENT_ID);
            this.rules = segmentJson.getJSONArray(ConfigConstants.RULES);

        } catch (Exception e) {
            AppConfigException.logException(this.className, "Segment.init", e,
                    new Object[]{"Invalid action in Segment class. "});
        }
    }

    /**
     * @return rules of the segment
     */
    public JSONArray getRules() {
        return rules;
    }

    /**
     * @return segment name
     */
    public String getName() {
        return name;
    }

    /**
     * @return segment id
     */
    public String getSegmentId() {
        return segmentId;
    }

    /**
     * Evaluate the Segment rules.
     *
     * @param entityAttributes entity attributes JSON object
     * @return {@code true} is evaluation is passed. {@code false} otherwise
     */
    public Boolean evaluateRule(JSONObject entityAttributes) {

        for (int index = 0; index < this.rules.length(); index++) {
            try {
                Rule rule = new Rule(this.rules.getJSONObject(index));
                if (!rule.evaluateRule(entityAttributes)) {
                    return false;
                }
            } catch (Exception e) {
                AppConfigException.logException(this.className, "evaluateRule", e,
                        new Object[]{"Invalid action in Segment class."});
            }
        }
        return true;
    }
}
