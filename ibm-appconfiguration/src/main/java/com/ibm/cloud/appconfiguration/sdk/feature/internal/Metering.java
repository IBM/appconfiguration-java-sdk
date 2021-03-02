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

package com.ibm.cloud.appconfiguration.sdk.feature.internal;

import com.ibm.cloud.appconfiguration.sdk.core.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metering {

    private static Metering instance;
    private int sendInterval = 600000;
    private String meteringUrl = null;
    private String apikey = null;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>> meteringData =
            new ConcurrentHashMap();

    public synchronized static Metering getInstance() {
        if (instance == null) {
            instance = new Metering();
        }
        return instance;
    }

    private Metering() {

        new RetryHandler(new RetryInterface() {
            @Override
            public void retryMethod() {
                sendMetering();
            }
        }, sendInterval);
    }

    public void setMeteringUrl(String url, String apikey) {
        this.meteringUrl = url;
        this.apikey = apikey;
    }

    public synchronized void addMetering(String guid, String collectionId, String feature) {

        Boolean hasData = false;
        HashMap<String, Object> featureJson = new HashMap();
        featureJson.put("count", 1);
        Instant currentDate = Instant.now();
        featureJson.put("evaluation_time", currentDate);

        if (this.meteringData.containsKey(guid)) {
            if (meteringData.get(guid).containsKey(collectionId)) {

                if (meteringData.get(guid).get(collectionId).containsKey(feature)) {
                    hasData = true;
                    meteringData.get(guid).get(collectionId).get(feature).put("evaluation_time", currentDate);
                    int count = (int) meteringData.get(guid).get(collectionId).get(feature).get("count");
                    meteringData.get(guid).get(collectionId).get(feature).put("count", count + 1);
                } else {
                    meteringData.get(guid).get(collectionId).put(feature, new HashMap());
                }
            } else {
                ConcurrentHashMap<String, HashMap<String, Object>> featureMap = new ConcurrentHashMap();
                featureMap.put(feature, new HashMap());
                this.meteringData.get(guid).put(collectionId, featureMap);
            }

        } else {
            ConcurrentHashMap<String, HashMap<String, Object>> featureMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> collectionIdMap = new ConcurrentHashMap();
            featureMap.put(feature, new HashMap());
            collectionIdMap.put(collectionId, featureMap);
            this.meteringData.put(guid, collectionIdMap);
        }

        if (!hasData) {
            meteringData.get(guid).get(collectionId).put(feature, featureJson);
        }
    }

    private synchronized void sendMetering() {

        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, java.util.HashMap<String, Object>>>> sendMeteringData = meteringData;
        meteringData = new ConcurrentHashMap();

        if (sendMeteringData.size() <= 0) {
            return;
        }

        HashMap<String, JSONArray> result = new HashMap();

        sendMeteringData.forEach( (guid, guidMap) -> {

            result.put(guid, new JSONArray());

            guidMap.forEach((collectionId, collectionMap) -> {

                JSONObject collections = new JSONObject();
                collections.put("collection_id", collectionId);
                collections.put("usages", new JSONArray());

                collectionMap.forEach((featureId, featureMap) -> {
                    JSONObject usages = new JSONObject();
                    usages.put("feature_id", featureId);
                    usages.put("evaluation_time", featureMap.get("evaluation_time"));
                    usages.put("count", featureMap.get("count"));
                    collections.getJSONArray("usages").put(usages);
                });
                result.get(guid).put(collections);
            });
        });

        result.forEach( (guid, dataArray) -> {
            dataArray.forEach((json) -> {
                this.sendToServer(guid, (JSONObject) json);
            });
        });
    }

    private void sendToServer(String guid, JSONObject data ) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", this.apikey);
        headers.put("Content-Type", "application/json");

        BaseRequest baseRequest = new BaseRequest.Builder().url(URLBuilder.getMeteringUrl(guid)).method(RequestTypes.POST).body(data).headers(headers).build();

        baseRequest.execute(new AppConfigurationResponseListener() {
            @Override
            public void onSuccess(Integer statusCode, String responseBody) {
                if (statusCode >= CoreConstants.REQUEST_SUCCESS_200 && statusCode <= CoreConstants.REQUEST_SUCCESS_299) {
                    BaseLogger.debug("Successfully pushed the data to metering");
                } else {
                    BaseLogger.error("Error while sending the metering data. Status code " + statusCode);
                }
            }

            @Override
            public void onFailure(Integer statusCode, String responseBody) {
                BaseLogger.debug(responseBody);
            }
        });

    }

}
