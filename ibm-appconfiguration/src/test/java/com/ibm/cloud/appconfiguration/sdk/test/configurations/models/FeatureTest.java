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
package com.ibm.cloud.appconfiguration.sdk.test.configurations.models;

import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.ConfigurationOptions;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.ConfigurationType;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.nio.file.Paths;


public class FeatureTest {

    Feature sut;


    static {
        ConfigurationHandler configurationHandler = ConfigurationHandler.getInstance();
        Path resourceDirectory = Paths.get("src","test","resources");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();
        ConfigurationOptions configOption = new ConfigurationOptions();
        configOption.setBootstrapFile(absolutePath + "/user.json");
        configOption.setLiveConfigUpdateEnabled(false);
        configurationHandler.setContext(ConfigConstants.COLLECTION_ID, ConfigConstants.ENVIRONMENT_ID, configOption);
        configurationHandler.loadData();
    }

    public void setUpFeature(ConfigurationType type, Object disabled, Object enaabled, Boolean isEnabled,
                             String format) {
        JSONObject feature = new JSONObject();
        try {
            feature.put("name","defaultFeature");
            feature.put("feature_id","defaultfeature");
            feature.put("type",type.toString());
            feature.put("disabled_value",disabled);
            feature.put("enabled_value",enaabled);
            feature.put("enabled",isEnabled);
            feature.put("segment_exists", false);
            feature.put("segment_rules",new JSONArray());
            feature.put("rollout_percentage",99);
            if (format != null) {
                feature.put("format", format);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        this.sut = new Feature(feature);
    }


    @Test public void testFeature() {
        setUpFeature(ConfigurationType.STRING, "unknown user","Org user", true, null);
        assertEquals(sut.getFeatureDataType(), ConfigurationType.STRING);
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), true);
        assertEquals(sut.getCurrentValue("d",null),"Org user");
        assertEquals(sut.getFeatureDataFormat(), "TEXT");

    }

    @Test
    public void testBooleanFeature() {
        setUpFeature(ConfigurationType.BOOLEAN, false,true , true, null);
        assertEquals(sut.getFeatureDataType(), ConfigurationType.BOOLEAN);
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), true);
        assertEquals(sut.getFeatureDataFormat(), null);

    }

    @Test
    public void testNumericFeature() {
        setUpFeature(ConfigurationType.NUMERIC, 20,50, false, null);
        assertEquals(sut.getFeatureDataType(), ConfigurationType.NUMERIC);
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), false);
        assertEquals(sut.getFeatureDataFormat(), null);

        assertEquals(sut.getCurrentValue("d",null),20);
        assertEquals(sut.getCurrentValue(null,null),null);

    }

    @Test
    public void testYamlFeature() {
        String enabled = "name: tester\ndescription: testing\n---\nname: developer\ndescription: coding";
        String disabled = "name: devops\ndescription: deploying\n";
        setUpFeature(ConfigurationType.STRING, disabled, enabled, true, "YAML");
        assertEquals(sut.getFeatureDataType(), ConfigurationType.STRING);
        assertEquals(sut.getFeatureDataFormat(), "YAML");
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), true);
        assertEquals(sut.getEnabledValue(), enabled);
        assertEquals(sut.getDisabledValue(), disabled);

        assertEquals(sut.getCurrentValue("d",null), enabled);
        assertEquals(sut.getCurrentValue(null,null),null);
    }

    @Test
    public void testJSONFeature() {
        JSONObject enabled = new JSONObject();
        enabled.put("name", "tester");
        enabled.put("description", "testing");

        JSONObject disabled = new JSONObject();
        disabled.put("name", "developer");
        disabled.put("description", "coding");

        setUpFeature(ConfigurationType.STRING, disabled, enabled, true, "JSON");
        assertEquals(sut.getFeatureDataType(), ConfigurationType.STRING);
        assertEquals(sut.getFeatureDataFormat(), "JSON");
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), true);
        assertEquals(((JSONObject) sut.getEnabledValue()).get("name"), "tester");
        assertEquals(((JSONObject) sut.getDisabledValue()).get("name"), "developer");

        assertEquals(sut.getCurrentValue(null,null),null);
    }

    @Test
    public void testFeatureException() {
        this.sut = new Feature(new JSONObject());
        assertNull(this.sut.getFeatureId());
        assertNull(this.sut.getFeatureName());
        assertNull(this.sut.getFeatureDataType());
        assertNull(this.sut.getDisabledValue());
        assertNull(this.sut.getEnabledValue());
        assertNull(this.sut.getSegmentRules());
    }

    @Test
    public void testFeatureCurrentValueWithEntityId() {
        setUpFeature(ConfigurationType.BOOLEAN, false,true , true, null);
        assertEquals(sut.getFeatureDataType(), ConfigurationType.BOOLEAN);
        Boolean currentValue = (Boolean) sut.getCurrentValue("test");
        assertEquals(currentValue, true);
    }
}
