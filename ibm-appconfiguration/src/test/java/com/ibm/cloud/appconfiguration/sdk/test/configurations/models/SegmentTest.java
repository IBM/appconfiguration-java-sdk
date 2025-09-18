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
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class SegmentTest {

    Segment sut;

    public void setUp() {
        ArrayList<Rule> rulesList = new ArrayList<>();
        rulesList.add(new Rule("radius", "lesserThanEquals", Collections.singletonList(100)));
        rulesList.add(new Rule("cityRadius", "lesserThan", Collections.singletonList(50)));
        this.sut = new Segment("RegionalUser", "kdu77n4s", rulesList);
    }

    @Test
    public void testSegment() {
        setUp();
        JSONObject clientAttributes = new JSONObject();
        try {
            clientAttributes.put("radius",100);
            clientAttributes.put("cityRadius",35);
            assertTrue(sut.evaluateRule(clientAttributes));

            clientAttributes.put("radius",101);
            clientAttributes.put("cityRadius",35);
            assertFalse(sut.evaluateRule(clientAttributes));

            assertEquals(sut.getName(),"RegionalUser" );
            assertEquals(sut.getSegmentId(),"kdu77n4s" );
            assertEquals(sut.getRules().size(),2 );
        } catch (Exception e) {
            System.out.println(e);
            assertFalse(true);
        }

    }

    @Test
    public void testSegmentException() {
        this.sut = new Segment(null, null, null);
        assertNull(this.sut.getName());
        assertNull(this.sut.getRules());
        assertNull(this.sut.getSegmentId());
    }
}
