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

import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONArray;
import org.json.JSONObject;

public class Property {

    private String name;
    private String propertyId;
    private JSONArray segmentRules;
    private ConfigurationType type;
    private Object value;

    public Property(JSONObject propertyData) {

        try {
            this.name = propertyData.getString("name");
            this.propertyId = propertyData.getString("property_id");
            this.segmentRules = propertyData.getJSONArray("segment_rules");
            this.type = ConfigurationType.valueOf(propertyData.getString("type"));
            this.value = propertyData.get("value");
        } catch (Exception e) {
            AppConfigException.logException("Property", "Constructor", e, new Object[]{"Invalid action in Property class."});
        }
    }


    public String getPropertyName() {
        return this.name;
    }

    public String getPropertyId() {
        return this.propertyId;
    }

    public Object getValue() {
        return value;
    }

    public ConfigurationType getPropertyDataType() {
        return type;
    }

    public JSONArray getSegmentRules() {
        return segmentRules;
    }

    public Object getCurrentValue(String identityId, JSONObject identityAttributes) {

        if (!Validators.validateString(identityId)) {
            BaseLogger.error("A valid id should be passed for this method.");
            return null;
        }

        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        return configurationHandler.propertyEvaluation(this, identityId, identityAttributes);
    }

}
