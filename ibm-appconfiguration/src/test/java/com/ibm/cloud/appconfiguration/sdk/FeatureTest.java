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
package com.ibm.cloud.appconfiguration.sdk;

import com.ibm.cloud.appconfiguration.sdk.feature.models.FeatureType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ibm.cloud.appconfiguration.sdk.feature.models.Feature;

public class FeatureTest {

    Feature sut;
    public void setUpStringFeature(FeatureType type, Object disabled, Object enaabled, Boolean isEnabled) {

        JSONObject feature = new JSONObject();
        try {
            feature.put("name","defaultFeature");
            feature.put("feature_id","defaultfeature");
            feature.put("type",type.toString());
            feature.put("disabled_value",disabled);
            feature.put("enabled_value",enaabled);
            feature.put("isEnabled",isEnabled);
            feature.put("segment_exists", false);
            feature.put("segment_rules",new JSONArray());

        } catch (Exception e) {
            System.out.println(e);
        }
        this.sut = new Feature(feature);
    }


    @Test public void testFeature() {
        setUpStringFeature(FeatureType.STRING, "unknown user","Org user", true );
        assertEquals(sut.getFeatureDataType(), FeatureType.STRING);
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), true);
    }

    @Test
    public void testBooleanFeature() {
        setUpStringFeature(FeatureType.BOOLEAN, false,true , true);
        assertEquals(sut.getFeatureDataType(), FeatureType.BOOLEAN);
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), true);

    }

    @Test
    public void testNumericFeature() {
        setUpStringFeature(FeatureType.NUMERIC, 20,50, false );
        assertEquals(sut.getFeatureDataType(), FeatureType.NUMERIC);
        assertEquals(sut.getFeatureName(), "defaultFeature");
        assertEquals(sut.getFeatureId(), "defaultfeature");
        assertEquals(sut.isEnabled(), false);

        assertEquals(sut.getCurrentValue("d",null),20);
    }
}
