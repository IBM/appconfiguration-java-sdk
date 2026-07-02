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

package com.ibm.cloud.appconfiguration.sdk.test.core;

import com.ibm.cloud.appconfiguration.sdk.core.ServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceImplTest {
    @Test public void testService() {
        assertNotEquals(ServiceImpl.getCurrentDateTime(), String.valueOf(Instant.now()),
            "These two values are not equal");

        assertEquals("0.4.1", ServiceImpl.getVersion(), "version are same in pom.xml");
        assertEquals("appconfiguration-java-sdk", ServiceImpl.getArtifactId(), "Artifact Id are same");

        ServiceImpl test = ServiceImpl.getInstance("apikey");
        assertThrows(Exception.class, () -> test.getConfig("http://testConfig"));
        assertThrows(Exception.class, () -> test.post("http://testMetering", "{}"));
    }
}
