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

package com.ibm.cloud.appconfiguration.sdk.test.configurations;

import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigurationHandlerTest {

    @Test
    public void testEvaluations() throws InterruptedException {

        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        configurationHandler.loadData();
        configurationHandler.init("apikey", "guid", "region", "");

        Path resourceDirectory = Paths.get("src","test","resources");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();

        configurationHandler.setContext("collectionId", "environmentId", absolutePath + "/user.json",false);
        configurationHandler.loadData();
        Thread.sleep(2500);


        String propertyJson = "{\"name\":\"numericProperty\",\"property_id\":\"numericproperty\",\"description\":\"testing prop\",\"value\":10,\"type\":\"NUMERIC\",\"tags\":\"test\",\"segment_rules\":[{\"rules\":[{\"segments\":[\"keuyclvf\"]}],\"value\":81,\"order\":1}],\"collections\":[{\"collection_id\":\"appcrash\"}]}";
        String featureJson = "{\"name\":\"defaultFeature\",\"feature_id\":\"defaultfeature\",\"type\":\"STRING\",\"enabled_value\":\"hello\",\"disabled_value\":\"Bye\",\"segment_rules\":[{\"rules\":[{\"segments\":[\"kg92d3wa\"]}],\"value\":\"Welcome\",\"order\":1}],\"segment_exists\":true,\"enabled\":true}";
        JSONObject featureJsonObject = new JSONObject(featureJson);
        JSONObject propertyJsonObject = new JSONObject(propertyJson);

        Feature featureObj = new Feature(featureJsonObject);
        Property propertyObj = new Property(propertyJsonObject);

        JSONObject entityObj = new JSONObject();
        entityObj.put("email", "test.dev@tester.com");

        Object value = configurationHandler.featureEvaluation(featureObj, "id1", entityObj);
        assertEquals(value, "Welcome");

        entityObj.put("email", "test@tester.com");
        value = configurationHandler.featureEvaluation(featureObj, "id1", entityObj);
        assertEquals(value, "hello");

        value = configurationHandler.featureEvaluation(featureObj, "id1", new JSONObject());
        assertEquals(value, "hello");


        entityObj.put("email", "test.dev@tester.com");
        value = configurationHandler.propertyEvaluation(propertyObj, "id1", entityObj);
        assertEquals(value, 81);


        entityObj.put("email", "test@f.com");
        value = configurationHandler.propertyEvaluation(propertyObj, "id1", entityObj);
        assertEquals(value, 10);

        value = configurationHandler.propertyEvaluation(propertyObj, "id1", new JSONObject());
        assertEquals(value, 10);

        Feature feature = configurationHandler.getFeature("defaultfeature");
        assertEquals(feature.getFeatureId(), "defaultfeature");

        HashMap<String, Feature> features = configurationHandler.getFeatures();
        assertEquals(features.size(), 3);

        Property property = configurationHandler.getProperty("numericproperty");
        assertEquals(property.getPropertyId(), "numericproperty");

        HashMap<String, Property> properties = configurationHandler.getProperties();
        assertEquals(properties.size(), 1);

    }


}
