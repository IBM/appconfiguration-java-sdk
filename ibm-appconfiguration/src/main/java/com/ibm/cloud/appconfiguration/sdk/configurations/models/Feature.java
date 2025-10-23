/**
 * Copyright 2021 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.cloud.appconfiguration.sdk.configurations.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Feature class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feature {

    @JsonProperty("name")
    private String name;

    @JsonProperty("feature_id")
    private String featureId;

    @JsonProperty("type")
    private ConfigurationType type;

    @JsonProperty("format")
    private String format;

    @JsonProperty("enabled_value")
    private Object enabledValue;

    @JsonProperty("disabled_value")
    private Object disabledValue;

    @JsonProperty("segment_rules")
    private List<SegmentRules> segmentRules;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("rollout_percentage")
    private Integer rolloutPercentage;

    @JsonProperty("collections")
    private List<Collection> collections;

    // Constructor for Jackson with required field validation
    @JsonCreator
    public Feature(
        @JsonProperty(value = "name", required = true) String name,
        @JsonProperty(value = "feature_id", required = true) String featureId,
        @JsonProperty(value = "type", required = true) ConfigurationType type,
        @JsonProperty(value = "enabled_value", required = true) Object enabledValue,
        @JsonProperty(value = "disabled_value", required = true) Object disabledValue,
        @JsonProperty(value = "segment_rules", required = true) List<SegmentRules> segmentRules,
        @JsonProperty(value = "enabled", required = true) Boolean enabled
    ) {
        this.name = name;
        this.featureId = featureId;
        this.type = type;
        this.enabledValue = enabledValue;
        this.disabledValue = disabledValue;
        this.segmentRules = segmentRules;
        this.enabled = enabled;
    }

    // optional field, set it post-creation
    @JsonProperty("format")
    public void setFormat(String format) {
        this.format = format;
    }

    // optional field, set it post-creation
    @JsonProperty("rollout_percentage")
    public void setRolloutPercentage(Integer rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    // optional field, set it post-creation
    @JsonProperty("collections")
    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

    public String getName() {
        return name;
    }

    /**
     * @return String feature flag name
     * Kept for backward compatibility. Same as "getName()".
     */
    @JsonIgnore
    public String getFeatureName() {
        return this.getName();
    }

    public String getFeatureId() {
        return featureId;
    }

    public ConfigurationType getType() {
        return type;
    }

    /**
     * Kept for backward compatibility. Same as "getType()".
     *
     * @return string named BOOLEAN/STRING/NUMERIC
     */
    @JsonIgnore
    public ConfigurationType getFeatureDataType() {
        return this.getType();
    }

    public String getFormat() {
        if (this.type.equals(ConfigurationType.STRING) && this.format == null) {
            return "TEXT";
        }
        return format;
    }

    /**
     * Kept for backward compatibility. Same as "getFormat()".
     *
     * @return string named TEXT/JSON/YAML/null
     */
    @JsonIgnore
    public String getFeatureDataFormat() {
        return this.getFormat();
    }

    public Object getEnabledValue() {
        return enabledValue;
    }

    public Object getDisabledValue() {
        return disabledValue;
    }

    public List<SegmentRules> getSegmentRules() {
        return segmentRules;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * @return Boolean enabled status of flag
     * Kept for backward compatibility. Same as "getEnabled()".
     */
    @JsonIgnore
    public Boolean isEnabled() {
        return this.getEnabled();
    }

    public Integer getRolloutPercentage() {
        return rolloutPercentage != null ? rolloutPercentage : ConfigConstants.DEFAULT_ROLLOUT_PERCENTAGE;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    /**
     * Get the evaluated value of the feature.
     *
     * @param entityId         Id of the Entity.
     *                         This will be a string identifier related to the Entity against which the feature is evaluated.
     *                         For example, an entity might be an instance of an app that runs on a mobile device, a microservice that runs on the cloud,
     *                         or a component of infrastructure that runs that microservice. For any entity to interact with App Configuration, it must provide a unique entity ID.
     * @param entityAttributes A JSON object consisting of the attribute name and their values that defines the specified entity.
     *                         This is an optional parameter if the feature flag is not configured with any targeting definition. If the targeting is configured,
     *                         then entityAttributes should be provided for the rule evaluation.
     *                         An attribute is a parameter that is used to define a segment. The SDK uses the attribute values to determine if the
     *                         specified entity satisfies the targeting rules, and returns the appropriate feature flag value.
     * @return {boolean|string|number|null} Returns one of the Enabled/Disabled/Overridden value based on the evaluation.
     */
    public Object getCurrentValue(String entityId, JSONObject entityAttributes) {
        if (!Validators.validateString(entityId)) {
            BaseLogger.error("A valid entity id should be passed for this method.");
            return null;
        }
        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        HashMap<String, Object> map = configurationHandler.featureEvaluation(this, entityId, entityAttributes);
        Object res = map.get(ConfigConstants.CURRENT_VALUE);
        return res;
    }

    /**
     * Get the evaluated value of the feature.
     *
     * @param entityId Id of the Entity.
     *                 This will be a string identifier related to the Entity against which the feature flag is evaluated.
     *                 For example, an entity might be an instance of an app that runs on a mobile device, a microservice that runs on the cloud,
     *                 or a component of infrastructure that runs that microservice. For any entity to interact with App Configuration, it must provide a unique entity ID.
     *                 The SDK uses the entityId to determine if the specified entity is eligible for default percentage rollout, and returns the appropriate feature flag value.
     * @return {boolean|string|number} Returns one of the Enabled/Disabled value based on the evaluation.
     */
    public Object getCurrentValue(String entityId) {
        return getCurrentValue(entityId, null);
    }
}
