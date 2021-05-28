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

package com.ibm.cloud.appconfiguration.sdk.configurations.models;

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Feature class
 */
public class Feature {

    private Boolean enabled;
    private String name;
    private String featureId;
    private JSONArray segmentRules;
    private ConfigurationType type;
    private Object disabledValue;
    private Object enabledValue;

    public Feature(JSONObject featureData) {

        try {
            this.enabled = featureData.getBoolean("enabled");
            this.name = featureData.getString("name");
            this.featureId = featureData.getString("feature_id");
            this.segmentRules = featureData.getJSONArray("segment_rules");
            this.type = ConfigurationType.valueOf(featureData.getString("type"));
            this.disabledValue = featureData.get("disabled_value");
            this.enabledValue = featureData.get("enabled_value");

        } catch (Exception e) {
            AppConfigException.logException("Feature", "Constructor", e, new Object[] { "Invalid action in Feature class."});
        }
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public String getFeatureName() {
        return this.name;
    }

    public String getFeatureId() {
        return this.featureId;
    }

    public Object getEnabledValue() {
        return enabledValue;
    }

    public Object getDisabledValue() {
        return disabledValue;
    }

    public ConfigurationType getFeatureDataType() {
        return type;
    }

    public JSONArray getSegmentRules() {
        return segmentRules;
    }

    public Object getCurrentValue(String entityId, JSONObject entityAttributes) {

        if (!Validators.validateString(entityId)) {
            BaseLogger.error("A valid entity id should be passed for this method.");
            return null;
        }
        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        return configurationHandler.featureEvaluation(this, entityId, entityAttributes);

    }
}
