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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection {

    @JsonProperty("name")
    private String name;

    @JsonProperty("collection_id")
    private String collectionId;

    // Constructor for Jackson with required field validation
    @JsonCreator
    public Collection(
        @JsonProperty(value = "collection_id", required = true) String collectionId
    ) {
        this.collectionId = collectionId;
    }

    // optional field, set it post-creation
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCollectionId() {
        return collectionId;
    }
}
