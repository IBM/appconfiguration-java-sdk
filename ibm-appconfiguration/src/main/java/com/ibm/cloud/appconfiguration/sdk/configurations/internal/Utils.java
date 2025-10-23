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

package com.ibm.cloud.appconfiguration.sdk.configurations.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.CacheConfig;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Collection;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Config;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Environment;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.RuleElem;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Utils {

    private Utils() {
    }

    /**
     * Unified parser for app-config data for sdk-config/export/promote format.
     *
     * @param jsonData      string JSON input
     * @param environmentId selected environment ID
     * @param collectionId  required collection ID
     * @return ResultExtractConfigurations results after extracting configurations from json input
     *
     * 1. Parse the jsonData into Config class
     * 2. Extract all the Features, Properties and Segments that are under environmentId and assigned to collectionId
     * 3. If collections array is not present - feature/property is still considered as part of the config.
     * 4. And creates a new CacheConfig from the extracted data
     */
    public static ResultExtractConfigurations extractConfigurations(String jsonData, String environmentId, String collectionId) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
            Config config = objectMapper.readValue(jsonData, Config.class);

            // validate environmentId & pick the environment
            Environment matchingEnv = null;
            for (Environment env : config.getEnvironments()) {
                if (environmentId.equals(env.getEnvironmentId())) {
                    matchingEnv = env;
                    break;
                }
            }
            if (matchingEnv == null) {
                return new ResultExtractConfigurations(null, false, "no data matching for environment id: " + environmentId);
            }

            // validate collectionId
            Collection matchingCollection = null;
            for (Collection collection : config.getCollections()) {
                if (collectionId.equals(collection.getCollectionId())) {
                    matchingCollection = collection;
                    break;
                }
            }
            if (matchingCollection == null) {
                return new ResultExtractConfigurations(null, false, "no data matching for collection id: " + collectionId);
            }

            // lists to store the extracted features, properties & segments
            List<Feature> features = new ArrayList<>();
            List<Property> properties = new ArrayList<>();
            List<Segment> segments = new ArrayList<>();

            List<String> segmentIds = new ArrayList<>();
            Map<String, Boolean> uniqueSegmentIdsMap = new HashMap<>();

            // loop through features in the matching environment, and pick all the features matching the collectionId
            for (Feature feature : matchingEnv.getFeatures()) {
                boolean match = false;
                // special case for collections field non-existing
                if (feature.getCollections() == null) {
                    match = true;
                } else {
                    for (Collection collection : feature.getCollections()) {
                        if (collectionId.equals(collection.getCollectionId())) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    features.add(feature);
                    // get the segmentIds from the extracted feature. Use the segmentId to extract segments.
                    for (SegmentRules segmentRules : feature.getSegmentRules()) {
                        for (RuleElem ruleElem : segmentRules.getRules()) {
                            segmentIds.addAll(ruleElem.getSegments());
                        }
                    }
                }
            }

            // loop through properties in the matching environment, and pick all the properties matching the collectionId
            for (Property property : matchingEnv.getProperties()) {
                boolean match = false;
                // special case for collections field non-existing
                if (property.getCollections() == null) {
                    match = true;
                } else {
                    for (Collection collection : property.getCollections()) {
                        if (collectionId.equals(collection.getCollectionId())) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    properties.add(property);
                    // get the segmentIds from the extracted feature. Use the segmentId to extract segments.
                    for (SegmentRules segmentRules : property.getSegmentRules()) {
                        for (RuleElem ruleElem : segmentRules.getRules()) {
                            segmentIds.addAll(ruleElem.getSegments());
                        }
                    }
                }
            }

            // segmentIds can contain duplicates. Filter them out using the map
            for (String segmentId : segmentIds) {
                uniqueSegmentIdsMap.put(segmentId, true);
            }
            List<String> uniqueSegmentIds = new ArrayList<>(uniqueSegmentIdsMap.keySet());

            // For all the unique segmentIds extract their segment
            for (String segmentId : uniqueSegmentIds) {
                Segment matchingSegment = null;
                for (Segment segment : config.getSegments()) {
                    if (segmentId.equals(segment.getSegmentId())) {
                        matchingSegment = segment;
                        break;
                    }
                }
                if (matchingSegment == null) {
                    return new ResultExtractConfigurations(null, false, "no data matching for segment id: " + segmentId);
                }
                segments.add(matchingSegment);
            }

            CacheConfig cacheConfig = new CacheConfig(features, properties, segments);
            return new ResultExtractConfigurations(cacheConfig, true, null);

        } catch (JsonProcessingException e) {
            String errorMsg = "JSON processing error: " + e.getMessage();
            return new ResultExtractConfigurations(null, false, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage();
            return new ResultExtractConfigurations(null, false, errorMsg);
        }
    }


    /**
     * @param data Cached resources
     * @param environmentId environment ID
     * @param collectionId collection ID
     * @return String reformated json string representaion of resources
     * will reformat the configurations from CacheConfig to Config.
     */
    public static String formatConfig(CacheConfig data, String environmentId, String collectionId) {
        try {
            Environment environment = new Environment(
                    environmentId,
                    environmentId,
                    data.getFeatures(),
                    data.getProperties()
            );
            Collection collection = new Collection(
                    collectionId
            );
            Config config = new Config(
                    Collections.singletonList(environment),
                    Collections.singletonList(collection),
                    data.getSegments()
            );
            ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentArraysWith(new DefaultIndenter("  ", "\n"));
            printer.indentObjectsWith(new DefaultIndenter("  ", "\n"));
            objectMapper.setDefaultPrettyPrinter(printer);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        } catch (JsonProcessingException e) {
            BaseLogger.error("Error in reformatting the configurations" + e.getMessage());
            return null;
        }
    }
}
