/**
 * Copyright 2025 IBM Corp. All Rights Reserved.
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;

import java.util.List;

public class CacheConfig {

    @JsonProperty("features")
    private List<Feature> features;

    @JsonProperty("properties")
    private List<Property> properties;

    @JsonProperty("segments")
    private List<Segment> segments;

    // Constructor for Jackson with required field validation
    @JsonCreator
    public CacheConfig(
        @JsonProperty(value = "features", required = true) List<Feature> features,
        @JsonProperty(value = "properties", required = true) List<Property> properties,
        @JsonProperty(value = "segments", required = true) List<Segment> segments
    ) {
        this.features = features;
        this.properties = properties;
        this.segments = segments;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Segment> getSegments() {
        return segments;
    }
}
