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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PropertyTest {


    Property sut;
    public void setUpStringProperty(ConfigurationType type, Object value, String format) {
        this.sut = new Property("defaultProperty", "defaultproperty", type, value, new ArrayList<>());
        this.sut.setFormat(format);
    }


    @Test public void testProperty() {
        setUpStringProperty(ConfigurationType.STRING, "unknown user", null);
        assertEquals(sut.getPropertyDataType(), ConfigurationType.STRING);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
        assertEquals(sut.getPropertyDataFormat(), "TEXT");
    }

    @Test
    public void testBooleanProperty() {
        setUpStringProperty(ConfigurationType.BOOLEAN, false, null);
        assertEquals(sut.getPropertyDataType(), ConfigurationType.BOOLEAN);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
        assertNull(sut.getPropertyDataFormat());

    }

    @Test
    public void testNumericProperty() {
        setUpStringProperty(ConfigurationType.NUMERIC, 20, null);
        assertEquals(sut.getPropertyDataType(), ConfigurationType.NUMERIC);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
        assertEquals(sut.getCurrentValue("d",null),20);
        assertNull(sut.getPropertyDataFormat());
    }

    @Test
    public void testYamlProperty() {
        String value = "name: tester\ndescription: testing\n---\nname: developer\ndescription: coding";
        setUpStringProperty(ConfigurationType.STRING, value, "YAML");
        assertEquals(sut.getPropertyDataType(), ConfigurationType.STRING);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
        assertEquals(sut.getValue(), value);
        assertEquals(sut.getCurrentValue("d",null), value);
        assertEquals(sut.getPropertyDataFormat(), "YAML");
    }

    @Test
    public void testJsonProperty() {
        JSONObject value = new JSONObject();
        value.put("name", "tester");
        value.put("description", "testing");

        setUpStringProperty(ConfigurationType.STRING, value, "JSON");
        assertEquals(sut.getPropertyDataType(), ConfigurationType.STRING);
        assertEquals(sut.getPropertyName(), "defaultProperty");
        assertEquals(sut.getPropertyId(), "defaultproperty");
        assertEquals(((JSONObject) sut.getValue()).getString("name"), "tester");
        assertEquals(((JSONObject)sut.getCurrentValue("d",null)).getString("description"),
            "testing");
        assertEquals(sut.getPropertyDataFormat(), "JSON");
    }

    @Test
    public void testPropertyException() {
        this.sut = new Property(null, null, null, null, null);
        assertNull(this.sut.getPropertyId());
        assertNull(this.sut.getPropertyName());
        assertNull(this.sut.getValue());
    }
}
