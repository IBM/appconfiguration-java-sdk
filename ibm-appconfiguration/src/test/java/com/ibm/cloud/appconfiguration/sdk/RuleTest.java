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
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Rule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RuleTest {

    Rule sut;

    public void setUpEndsWith() {
        JSONObject rules = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("in.ibm.com");
        try {
            rules.put("values",values);
            rules.put("operator","endsWith");
            rules.put("attribute_name","email");

        } catch (Exception e) {
            System.out.println(e);
        }
        this.sut = new Rule(rules);
    }

    public void setUpEquals(Object value) {
        JSONObject rules = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(value);
        try {
            rules.put("values",values);
            rules.put("operator","is");
            rules.put("attribute_name","creditValues");

        } catch (Exception e) {
            System.out.println(e);
        }
        this.sut = new Rule(rules);
    }

    @Test
    public void testRules() throws JSONException {
        setUpEndsWith();
        assertEquals(sut.attributeName, "email");
        assertEquals(sut.operator, "endsWith");
        assertEquals(sut.values.length(), 1);
        assertEquals(sut.values.getString(0), "in.ibm.com");
    }

    @Test
    public void TestEvaluationEndsWithString() {
        setUpEndsWith();
        JSONObject clientAttributes = new JSONObject();
        try {
            clientAttributes.put("email","tester@in.ibm.com");
            assertTrue(sut.evaluateRule(clientAttributes));
            clientAttributes.put("email","tester@in.ibm.error");
            assertFalse(sut.evaluateRule(clientAttributes));
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    @Test
    public void TestEvaluationEndsWithDifferentValues() {

        JSONObject clientAttributes = new JSONObject();
        try {

            clientAttributes.put("creditValues","123");
            setUpEquals("123");
            assertTrue(sut.evaluateRule(clientAttributes));
            clientAttributes.put("creditValues","false");
            setUpEquals("false");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",123);
            setUpEquals(123);
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",false);
            setUpEquals("123");
            assertFalse(sut.evaluateRule(clientAttributes));
            setUpEquals(123);
            assertFalse(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",false);
            setUpEquals("false");
            assertFalse(sut.evaluateRule(clientAttributes));
            setUpEquals(false);
            assertTrue(sut.evaluateRule(clientAttributes));

        } catch (Exception e) {
            System.out.println(e);
        }

    }


}
