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

import com.ibm.cloud.appconfiguration.sdk.configurations.models.ConfigurationType;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PropertyTest {


    Property sut;
    public void setUpStringProperty(ConfigurationType type, Object value) {

        JSONObject property = new JSONObject();
        try {
            property.put("name","defaultProperty");
            property.put("property_id","defaultproperty");
            property.put("type",type.toString());
            property.put("value",value);
            property.put("segment_rules",new JSONArray());
        } catch (Exception e) {
            System.out.println(e);
        }
        this.sut = new Property(property);
    }


    @Test public void testProperty() {
        setUpStringProperty(ConfigurationType.STRING, "unknown user");
        assertEquals(sut.getPropertyDataType(), ConfigurationType.STRING);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
    }

    @Test
    public void testBooleanProperty() {
        setUpStringProperty(ConfigurationType.BOOLEAN, false);
        assertEquals(sut.getPropertyDataType(), ConfigurationType.BOOLEAN);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");

    }

    @Test
    public void testNumericProperty() {
        setUpStringProperty(ConfigurationType.NUMERIC, 20 );
        assertEquals(sut.getPropertyDataType(), ConfigurationType.NUMERIC);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
        assertEquals(sut.getCurrentValue("d",null),20);
    }

    @Test
    public void testPropertyException() {
        this.sut = new Property(new JSONObject());
        assertNull(this.sut.getPropertyId());
        assertNull(this.sut.getPropertyName());
        assertNull(this.sut.getValue());
    }
}
