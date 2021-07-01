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

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Metering;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MeteringTest {

    @Test
    public void testMetering() throws InterruptedException {

        Metering metering = Metering.getInstance();

        metering.sendMetering();
        Thread.sleep(2000);
        metering.addMetering("guid1","environment_id", "collection_id1","id_1","segment_id1","feature_id1",null);
        metering.addMetering("guid1","environment_id", "collection_id1","id_1","segment_id1","feature_id1",null);
        metering.addMetering("guid1","environment_id", "collection_id2","id_1","segment_id1","feature_id1",null);
        metering.addMetering("guid1","environment_id", "collection_id2","id_1","segment_id1","feature_id2",null);

        metering.addMetering("guid2","environment_id", "collection_id1","id_1","segment_id1","feature_id1",null);
        metering.addMetering("guid3","environment_id", "collection_id1","id_1","segment_id1","feature_id1",null);

        assertEquals(3, metering.sendMetering().size());

        for (int i=0; i<25; i++) {
            metering.addMetering("guid"+i,"environment_id"+i, "collection_id"+i,"id_"+i,"segment_id"+i,"feature_id"+i,
                null);
        }
        assertEquals(25, metering.sendMetering().size());
        List<JSONObject> jsonObjectList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("usages", jsonObjectList);
        jsonObject.put("collection_id", "");
        jsonObject.put("environment_id", "");
        metering.sendSplitMetering(jsonObject,0);
    }
}
