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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import org.json.JSONObject;

import java.util.List;

/**
 *  Defines the model of a segment defined in App Configuration service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Segment {

    @JsonProperty("name")
    private String name;

    @JsonProperty("segment_id")
    private String segmentId;

    @JsonProperty("rules")
    private List<Rule> rules;

    private final String className = this.getClass().getName();

    // Constructor for Jackson with required field validation
    @JsonCreator
    public Segment(
        @JsonProperty(value = "name", required = true) String name,
        @JsonProperty(value = "segment_id", required = true) String segmentId,
        @JsonProperty(value = "rules", required = true) List<Rule> rules
    ) {
        this.name = name;
        this.segmentId = segmentId;
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Evaluate the Segment rules.
     *
     * @param entityAttributes entity attributes JSON object
     * @return {@code true} is evaluation is passed. {@code false} otherwise
     */
    public Boolean evaluateRule(JSONObject entityAttributes) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        for (Rule rule : rules) {
            try {
                if (!rule.evaluateRule(entityAttributes)) {
                    return false;
                }
            } catch (Exception e) {
                AppConfigException.logException(this.className, "evaluateRule", e,
                    new Object[]{"Invalid action in Segment class."});
                return false;
            }
        }
        return true;
    }
}
