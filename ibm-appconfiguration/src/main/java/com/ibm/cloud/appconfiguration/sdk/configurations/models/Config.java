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

public class Config {

    @JsonProperty("environments")
    private List<Environment> environments;

    @JsonProperty("collections")
    private List<Collection> collections;

    @JsonProperty("segments")
    private List<Segment> segments;

    // Constructor for Jackson with required field validation
    @JsonCreator
    public Config(
        @JsonProperty(value = "environments", required = true) List<Environment> environments,
        @JsonProperty(value = "collections", required = true) List<Collection> collections,
        @JsonProperty(value = "segments", required = true) List<Segment> segments
    ) {
        this.environments = environments;
        this.collections = collections;
        this.segments = segments;
    }

    public List<Environment> getEnvironments() {
        return environments;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public List<Segment> getSegments() {
        return segments;
    }
}

