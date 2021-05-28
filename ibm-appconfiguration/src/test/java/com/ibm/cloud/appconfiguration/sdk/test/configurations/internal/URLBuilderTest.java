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

package com.ibm.cloud.appconfiguration.sdk.test.configurations.internal;

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.URLBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URLBuilderTest {

    @Test
    public void testUrls() {
        URLBuilder.initWithContext("collection_id", "environment_id", "region", "guid", "");

        assertTrue(URLBuilder.getConfigUrl().startsWith("https://region"));
        assertTrue(URLBuilder.getConfigUrl().contains("/collection_id/"));
        assertTrue(URLBuilder.getConfigUrl().contains("/guid/"));
        assertTrue(URLBuilder.getConfigUrl().contains("environment_id=environment_id"));


        assertTrue(URLBuilder.getMeteringUrl("guid").contains("/guid/"));
        assertTrue(URLBuilder.getMeteringUrl("guid").startsWith("https://region"));

        assertTrue(URLBuilder.getWebSocketUrl().startsWith("wss://region"));
        assertTrue(URLBuilder.getWebSocketUrl().contains("instance_id=guid"));
        assertTrue(URLBuilder.getWebSocketUrl().contains("collection_id=collection_id"));

        URLBuilder.initWithContext("collection_id","environment_id" ,"region", "guid", "https://customRegion.cloud.base");
        assertTrue(URLBuilder.getConfigUrl().startsWith("https://customRegion.cloud.base"));


    }
}
