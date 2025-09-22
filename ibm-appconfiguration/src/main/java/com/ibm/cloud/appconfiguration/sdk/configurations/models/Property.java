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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONObject;

import java.util.List;

/**
 * Property class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Property {

    @JsonProperty("name")
    private String name;

    @JsonProperty("property_id")
    private String propertyId;

    @JsonProperty("type")
    private ConfigurationType type;

    @JsonProperty("format")
    private String format;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("segment_rules")
    private List<SegmentRules> segmentRules;

    @JsonProperty("collections")
    private List<Collection> collections;

    // Constructor for Jackson with required field validation
    @JsonCreator
    public Property(
        @JsonProperty(value = "name", required = true) String name,
        @JsonProperty(value = "property_id", required = true) String propertyId,
        @JsonProperty(value = "type", required = true) ConfigurationType type,
        @JsonProperty(value = "value", required = true) Object value,
        @JsonProperty(value = "segment_rules", required = true) List<SegmentRules> segmentRules
    ) {
        this.name = name;
        this.propertyId = propertyId;
        this.type = type;
        this.value = value;
        this.segmentRules = segmentRules;
    }

    // optional field, set it post-creation
    @JsonProperty("format")
    public void setFormat(String format) {
        this.format = format;
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
     * Kept for backward compatibility. Same as "getName()".
     */
    @JsonIgnore
    public String getPropertyName() {
        return this.getName();
    }

    public String getPropertyId() {
        return propertyId;
    }

    public ConfigurationType getType() {
        return type;
    }

    /**
     *  Kept for backward compatibility. Same as "getType()".
     *
     * @return string named BOOLEAN/STRING/NUMERIC
     */
    @JsonIgnore
    public ConfigurationType getPropertyDataType() {
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
    public String getPropertyDataFormat() {
        return this.getFormat();
    }

    public Object getValue() {
        return value;
    }

    public List<SegmentRules> getSegmentRules() {
        return segmentRules;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    /**
     * Get the evaluated value of the property.
     *
     * @param entityId         Id of the Entity.
     *                         This will be a string identifier related to the Entity against which the property is evaluated.
     *                         For example, an entity might be an instance of an app that runs on a mobile device, a microservice that runs on the cloud,
     *                         or a component of infrastructure that runs that microservice. For any entity to interact with App Configuration, it must provide a unique entity ID.
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

    /**
     * Get the default property value.
     *
     * @param entityId Id of the Entity.
     * @return {boolean|string|number} Returns the default property value.
     */
    public Object getCurrentValue(String entityId) {
        return getCurrentValue(entityId, null);
    }

}
