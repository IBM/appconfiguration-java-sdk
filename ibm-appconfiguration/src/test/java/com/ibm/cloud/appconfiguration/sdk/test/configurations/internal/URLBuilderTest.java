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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLBuilderTest {

    @Test
    public void testUrls() {
        String overrideServiceUrl = "https://region.apprapp.test.cloud.ibm.com";

        // test prod url
        URLBuilder.initWithContext("collection_id", "environment_id", "region", "guid", "", false);
        assertEquals("https://region.apprapp.cloud.ibm.com/apprapp/feature/v1/instances/guid/collections/collection_id/config?environment_id=environment_id", URLBuilder.getConfigUrl());
        assertEquals("https://region.apprapp.cloud.ibm.com/apprapp/events/v1/instances/guid/usage", URLBuilder.getMeteringUrl());
        assertEquals("https://iam.cloud.ibm.com", URLBuilder.getIamUrl());
        assertEquals("wss://region.apprapp.cloud.ibm.com/apprapp/wsfeature?instance_id=guid&collection_id=collection_id&environment_id=environment_id", URLBuilder.getWebSocketUrl());

        // test dev & stage url
        URLBuilder.initWithContext("collection_id", "environment_id", "region", "guid", overrideServiceUrl, false);
        assertEquals("https://region.apprapp.test.cloud.ibm.com/apprapp/feature/v1/instances/guid/collections/collection_id/config?environment_id=environment_id", URLBuilder.getConfigUrl());
        assertEquals("https://region.apprapp.test.cloud.ibm.com/apprapp/events/v1/instances/guid/usage", URLBuilder.getMeteringUrl());
        assertEquals("https://iam.test.cloud.ibm.com", URLBuilder.getIamUrl());
        assertEquals("wss://region.apprapp.test.cloud.ibm.com/apprapp/wsfeature?instance_id=guid&collection_id=collection_id&environment_id=environment_id", URLBuilder.getWebSocketUrl());

        // test prod url with private endpoint
        URLBuilder.initWithContext("collection_id", "environment_id", "region", "guid", "", true);
        assertEquals("https://private.region.apprapp.cloud.ibm.com/apprapp/feature/v1/instances/guid/collections/collection_id/config?environment_id=environment_id", URLBuilder.getConfigUrl());
        assertEquals("https://private.region.apprapp.cloud.ibm.com/apprapp/events/v1/instances/guid/usage", URLBuilder.getMeteringUrl());
        assertEquals("https://private.iam.cloud.ibm.com", URLBuilder.getIamUrl());
        assertEquals("wss://private.region.apprapp.cloud.ibm.com/apprapp/wsfeature?instance_id=guid&collection_id=collection_id&environment_id=environment_id", URLBuilder.getWebSocketUrl());

        // test dev & stage url with private endpoint
        URLBuilder.initWithContext("collection_id", "environment_id", "region", "guid", overrideServiceUrl, true);
        assertEquals("https://private.region.apprapp.test.cloud.ibm.com/apprapp/feature/v1/instances/guid/collections/collection_id/config?environment_id=environment_id", URLBuilder.getConfigUrl());
        assertEquals("https://private.region.apprapp.test.cloud.ibm.com/apprapp/events/v1/instances/guid/usage", URLBuilder.getMeteringUrl());
        assertEquals("https://private.iam.test.cloud.ibm.com", URLBuilder.getIamUrl());
        assertEquals("wss://private.region.apprapp.test.cloud.ibm.com/apprapp/wsfeature?instance_id=guid&collection_id=collection_id&environment_id=environment_id", URLBuilder.getWebSocketUrl());

    }
}
