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
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Property class.
 */
public class Property {

    private String name;
    private String propertyId;
    private JSONArray segmentRules;
    private ConfigurationType type;
    private String format;
    private Object value;

    /**
     * @param propertyData properties JSON object that contains all the properties
     */
    public Property(JSONObject propertyData) {

        try {
            this.name = propertyData.getString(ConfigConstants.NAME);
            this.propertyId = propertyData.getString(ConfigConstants.PROPERTY_ID);
            this.segmentRules = propertyData.getJSONArray(ConfigConstants.SEGMENT_RULES);
            this.type = ConfigurationType.valueOf(propertyData.getString(ConfigConstants.TYPE));
            this.format = propertyData.optString(ConfigConstants.FORMAT, null);
            this.value = propertyData.get(ConfigConstants.VALUE);
        } catch (Exception e) {
            AppConfigException.logException("Property", "Constructor", e,
                    new Object[]{"Invalid action in Property class."});
        }
    }


    /**
     * Get the Property name.
     *
     * @return property name
     */
    public String getPropertyName() {
        return this.name;
    }

    /**
     * Get the Property Id.
     *
     * @return property id
     */
    public String getPropertyId() {
        return this.propertyId;
    }

    /**
     * Get the default property value.
     *
     * @return default property value
     */
    public Object getValue() {
        return value;
    }

    /**
     *  Get the Property data type.
     *
     * @return string named BOOLEAN/STRING/NUMERIC
     */
    public ConfigurationType getPropertyDataType() {
        return type;
    }

    /**
     *  Get the Property format type.
     *
     * @return string named TEXT/JSON/YAML/null
     */
    public String getPropertyDataFormat() {
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
     * Get the evaluated value of the property.
     *
     * @param entityId         Id of the Entity.
     *                         This will be a string identifier related to the Entity against which the property is evaluated.
     *                         For example, an entity might be an instance of an app that runs on a mobile device, a microservice that runs on the cloud, or a component of infrastructure that runs that microservice.
     *                         For any entity to interact with App Configuration, it must provide a unique entity ID.
     * @param entityAttributes A JSON object consisting of the attribute name and their values that defines the specified entity.
     *                         This is an optional parameter if the property is not configured with any targeting definition. If the targeting is configured,
     *                         then entityAttributes should be provided for the rule evaluation.
     *                         An attribute is a parameter that is used to define a segment. The SDK uses the attribute values to determine if the
     *                         specified entity satisfies the targeting rules, and returns the appropriate property value.
     * @return {boolean|string|number|null} Returns the default property value or its overridden value based on the evaluation.
     */
    public Object getCurrentValue(String entityId, JSONObject entityAttributes) {

        if (!Validators.validateString(entityId)) {
            BaseLogger.error("A valid id should be passed for this method.");
            return null;
        }

        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        return configurationHandler.propertyEvaluation(this, entityId, entityAttributes);
    }

    public Object getCurrentValue(String entityId) {
        return getCurrentValue(entityId, null);
    }

}
