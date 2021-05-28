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

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigurationResponseListener;
import com.ibm.cloud.appconfiguration.sdk.core.BaseRequest;
import com.ibm.cloud.appconfiguration.sdk.core.RequestTypes;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BaseRequestTest {

    @Test
    public void testBaseRequest() throws InterruptedException {
        BaseRequest baseRequest = new BaseRequest.Builder().url("url").method(RequestTypes.POST).body(new JSONObject(){{put("data","value");}}).headers(new HashMap(){{put("key1", "value1");}}).build();
        assertNotNull(baseRequest);

        baseRequest = new BaseRequest.Builder().url("https://cloud.ibm.com").method(RequestTypes.GET).headers(new HashMap(){{put("key1", "value1");}}).build();
        assertNotNull(baseRequest);
        final Boolean[] result = {false, false};
        baseRequest.execute(new AppConfigurationResponseListener() {
            @Override
            public void onSuccess(Integer statusCode, String responseBody) {
                result[0] = true;
            }

            @Override
            public void onFailure(Integer statusCode, String responseBody) {
                result[1] = true;
            }
        });

        TimeUnit.SECONDS.sleep(2);
        assertTrue(result[1]);
    }
}
