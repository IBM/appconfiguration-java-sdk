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
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;

import java.util.List;

/**
 * Defines the model of a segment rule defined in App Configuration service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SegmentRules {

    @JsonProperty("rules")
    private List<RuleElem> rules;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("rollout_percentage")
    private Object rolloutPercentage;

    // Constructor for Jackson with required field validation
    @JsonCreator
    public SegmentRules(
        @JsonProperty(value = "rules", required = true) List<RuleElem> rules,
        @JsonProperty(value = "value", required = true) Object value,
        @JsonProperty(value = "order", required = true) Integer order
    ) {
        this.rules = rules;
        this.value = value;
        this.order = order;
    }

    // optional field, set it post-creation
    @JsonProperty("rollout_percentage")
    public void setRolloutPercentage(Object rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
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
    public List<RuleElem> getRules() {
        return rules;
    }

    /**
     * @return the rolloutPercentage in the segment_rules object
     */
    public Object getRolloutPercentage() {
        return rolloutPercentage != null ? rolloutPercentage : ConfigConstants.DEFAULT_ROLLOUT_PERCENTAGE;
    }
}
