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

import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;


/**
 * Feature class.
 */
public class Feature {

    private Boolean enabled;
    private String name;
    private String featureId;
    private JSONArray segmentRules;
    private ConfigurationType type;
    private String format;
    private Object disabledValue;
    private Object enabledValue;
    private Integer rolloutPercentage;

    /**
     * @param featureData features JSON object that contains all the features
     */
    public Feature(JSONObject featureData) {

        try {
            this.enabled = featureData.getBoolean(ConfigConstants.ENABLED);
            this.name = featureData.getString(ConfigConstants.NAME);
            this.featureId = featureData.getString(ConfigConstants.FEATURE_ID);
            this.segmentRules = featureData.getJSONArray(ConfigConstants.SEGMENT_RULES);
            this.type = ConfigurationType.valueOf(featureData.getString(ConfigConstants.TYPE));
            this.format = featureData.optString(ConfigConstants.FORMAT, null);
            this.disabledValue = featureData.get(ConfigConstants.DISABLED_VALUE);
            this.enabledValue = featureData.get(ConfigConstants.ENABLED_VALUE);
            if (featureData.has(ConfigConstants.ROLLOUT_PERCENTAGE)) {
                this.rolloutPercentage = featureData.getInt(ConfigConstants.ROLLOUT_PERCENTAGE);
            } else {
                this.rolloutPercentage = ConfigConstants.DEFAULT_ROLLOUT_PERCENTAGE;
            }
        } catch (Exception e) {
            AppConfigException.logException("Feature", "Constructor", e, new Object[]{"Invalid action in Feature class."});
        }
    }

    /**
     * Return the state of the feature flag. Returns true, if the feature flag is enabled, otherwise returns false.
     *
     * @return {@code true} or {@code false}
     */
    public Boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Get the Feature name.
     *
     * @return the feature name
     */
    public String getFeatureName() {
        return this.name;
    }

    /**
     * Get the Feature Id.
     *
     * @return the feature id
     */
    public String getFeatureId() {
        return this.featureId;
    }

    /**
     * Get the enabled value of the feature.
     *
     * @return enabled value
     */
    public Object getEnabledValue() {
        return enabledValue;
    }

    /**
     * Get the disabled value of the feature.
     *
     * @return disabled value
     */
    public Object getDisabledValue() {
        return disabledValue;
    }

    /**
     * Get the feature data type.
     *
     * @return string named BOOLEAN/STRING/NUMERIC
     */
    public ConfigurationType getFeatureDataType() {
        return type;
    }

    /**
     * Get the feature format type.
     *
     * @return string named TEXT/JSON/YAML/null
     */
    public String getFeatureDataFormat() {
        if (this.type.equals(ConfigurationType.STRING) && this.format == null) {
            return "TEXT";
        }
        return format;
    }

    /**
     * Get the rules of the Segment targeted.
     *
     * @return segment rules
     */
    public JSONArray getSegmentRules() {
        return segmentRules;
    }

    /**
     * Get the Feature rolloutPercentage.
     *
     * @return the feature rolloutPercentage
     */
    public Integer getRolloutPercentage() {
        return rolloutPercentage;
    }

    /**
     * Get the evaluated value of the feature.
     *
     * @param entityId         Id of the Entity.
     *                         This will be a string identifier related to the Entity against which the feature is evaluated.
     *                         For example, an entity might be an instance of an app that runs on a mobile device, a microservice that runs on the cloud, or a component of infrastructure that runs that microservice.
     *                         For any entity to interact with App Configuration, it must provide a unique entity ID.
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

    public Object getCurrentValue(String entityId) {
        return getCurrentValue(entityId, null);
    }
}
