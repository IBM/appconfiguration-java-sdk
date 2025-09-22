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

import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Rule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleTest {

    Rule sut;

    public void setUp(List<Object> value, String operator, String attribute) {
        this.sut = new Rule(attribute, operator, value);
    }

    @Test
    public void testRules() throws JSONException {
        setUp(Collections.singletonList("in.ibm.com"), "endsWith", "email");
        assertEquals(sut.getAttributeName(), "email");
        assertEquals(sut.getOperator(), "endsWith");
        assertEquals(sut.getValues().size(), 1);
        assertEquals(sut.getValues().get(0), "in.ibm.com");
        assertFalse(sut.evaluateRule(new JSONObject()));
    }

    @Test
    public void testEvaluationEndsWithString() {
        setUp(Collections.singletonList("in.ibm.com"), "endsWith", "email");
        JSONObject clientAttributes = new JSONObject();
        try {
            clientAttributes.put("email","tester@in.ibm.com");
            assertTrue(sut.evaluateRule(clientAttributes));
            clientAttributes.put("email","tester@in.ibm.error");
            assertFalse(sut.evaluateRule(clientAttributes));
            clientAttributes.put("name","tester");
            assertFalse(sut.evaluateRule(clientAttributes));
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    @Test
    public void testEvaluationEndsWithDifferentValues() {

        JSONObject clientAttributes = new JSONObject();
        try {

            clientAttributes.put("creditValues","123");
            setUp(Collections.singletonList("123"), "is", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));
            clientAttributes.put("creditValues","false");
            setUp(Collections.singletonList("false"), "is", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",123);
            setUp(Collections.singletonList(123), "is", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",false);
            setUp(Collections.singletonList("123"), "is", "creditValues");

            assertFalse(sut.evaluateRule(clientAttributes));
            setUp(Collections.singletonList(123), "is", "creditValues");
            assertFalse(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",false);
            setUp(Collections.singletonList("false"), "is", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            setUp(Collections.singletonList(false), "is", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));


            clientAttributes.put("email","tester@in.ibm.com");
            setUp(Collections.singletonList("tester"), "startsWith", "email");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("email","tester@in.ibm.com");
            setUp(Collections.singletonList("bob"), "notStartsWith", "email");
            assertTrue(sut.evaluateRule(clientAttributes));
            setUp(Collections.singletonList("@google.com"), "notEndsWith", "email");
            assertTrue(sut.evaluateRule(clientAttributes));
            setUp(Collections.singletonList("#"), "notContains", "email");
            assertTrue(sut.evaluateRule(clientAttributes));
            setUp(Collections.singletonList("bob@ibm.com"), "isNot", "email");
            assertTrue(sut.evaluateRule(clientAttributes));


            clientAttributes.put("email","tester@in.ibm.com");
            setUp(Collections.singletonList("ibm"), "contains", "email");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",100);
            setUp(Collections.singletonList(200), "greaterThan", "creditValues");
            assertFalse(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",200);
            setUp(Collections.singletonList(100), "greaterThan", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",100);
            setUp(Collections.singletonList(200), "lesserThan", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",200);
            setUp(Collections.singletonList(100), "lesserThan", "creditValues");
            assertFalse(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",100);
            setUp(Collections.singletonList(100), "greaterThanEquals", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",20);
            setUp(Collections.singletonList(200), "greaterThanEquals", "creditValues");
            assertFalse(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",100);
            setUp(Collections.singletonList(100), "lesserThanEquals", "creditValues");
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues",200);
            setUp(Collections.singletonList(20), "lesserThanEquals", "creditValues");
            assertFalse(sut.evaluateRule(clientAttributes));

            clientAttributes.put("creditValues","200");
            setUp(Collections.singletonList("20"), "lesserThanEquals", "creditValues");
            assertFalse(sut.evaluateRule(clientAttributes));

        } catch (Exception e) {
            System.out.println(e);
        }

    }

    @Test
    public void testRulesException() {
        this.sut = new Rule(null, null, null);
        assertNull(this.sut.getAttributeName());
        assertNull(this.sut.getOperator());
        assertNull(this.sut.getValues());
    }


}
