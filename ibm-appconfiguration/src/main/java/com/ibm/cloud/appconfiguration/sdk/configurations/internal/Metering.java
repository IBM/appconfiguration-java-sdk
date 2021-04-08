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

package com.ibm.cloud.appconfiguration.sdk.configurations.internal;

import com.ibm.cloud.appconfiguration.sdk.core.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metering {

    private static Metering instance;
    private int sendInterval = 10000;
    private String meteringUrl = null;
    private String apikey = null;

    ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>> meteringFeatureData =
            new ConcurrentHashMap();
    ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>> meteringPropertyData =
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

    public synchronized void addMetering(String guid, String collectionId, String identityId, String segmentId, String featureId, String propertyId) {

        Boolean hasData = false;
        HashMap<String, Object> featureJson = new HashMap();
        featureJson.put("count", 1);
        Instant currentDate = Instant.now();
        featureJson.put("evaluation_time", currentDate);

        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>> meteringData = featureId != null ? this.meteringFeatureData : this.meteringPropertyData;
        String modifyKey = featureId != null ? featureId : propertyId;

        if (meteringData.containsKey(guid)) {
            if (meteringData.get(guid).containsKey(collectionId)) {
                if (meteringData.get(guid).get(collectionId).containsKey(modifyKey)) {
                    if (meteringData.get(guid).get(collectionId).get(modifyKey).containsKey(identityId)) {
                        if (meteringData.get(guid).get(collectionId).get(modifyKey).get(identityId).containsKey(segmentId)) {
                            hasData = true;
                            meteringData.get(guid).get(collectionId).get(modifyKey).get(identityId).get(segmentId).put("evaluation_time", currentDate);
                            int count = (int) meteringData.get(guid).get(collectionId).get(modifyKey).get(identityId).get(segmentId).get("count");
                            meteringData.get(guid).get(collectionId).get(modifyKey).get(identityId).get(segmentId).put("count", count + 1);
                        }
                    } else {
                        ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                        segmentIdMap.put(segmentId, new HashMap());
                        meteringData.get(guid).get(collectionId).get(modifyKey).put(identityId, segmentIdMap);
                    }
                } else {
                    ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                    ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> identityIdMap = new ConcurrentHashMap();
                    segmentIdMap.put(segmentId, new HashMap());
                    identityIdMap.put(identityId, segmentIdMap);
                    meteringData.get(guid).get(collectionId).put(modifyKey, identityIdMap);
                }
            } else {
                ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> identityIdMap = new ConcurrentHashMap();
                ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>> featureIdMap = new ConcurrentHashMap();
                segmentIdMap.put(segmentId, new HashMap());
                identityIdMap.put(identityId, segmentIdMap);
                featureIdMap.put(modifyKey, identityIdMap);
                meteringData.get(guid).put(collectionId, featureIdMap);
            }
        } else {
            ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> identityIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>> featureIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>> collectionIdMap = new ConcurrentHashMap();
            segmentIdMap.put(segmentId, new HashMap());
            identityIdMap.put(identityId, segmentIdMap);
            featureIdMap.put(modifyKey, identityIdMap);
            collectionIdMap.put(collectionId, featureIdMap);
            meteringData.put(guid, collectionIdMap);
        }
        if (!hasData) {
            meteringData.get(guid).get(collectionId).get(modifyKey).get(identityId).put(segmentId, featureJson);
        }
    }

    private synchronized void buildRequestBody(ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>> sendMeteringData, HashMap<String, JSONArray> result, String key) {

        sendMeteringData.forEach((guid, guidMap) -> {

            if (!result.containsKey(guid)) {
                result.put(guid, new JSONArray());
            }

            guidMap.forEach((collectionId, collectionMap) -> {

                JSONObject collections = new JSONObject();
                collections.put("collection_id", collectionId);
                collections.put("usages", new JSONArray());

                collectionMap.forEach((featureId, featureIdMap) -> {
                    featureIdMap.forEach((identityId, identityMap) -> {
                        identityMap.forEach((segmentId, segmentIdMap) -> {
                            JSONObject usages = new JSONObject();
                            usages.put(key, featureId);
                            usages.put("identity_id", identityId);
                            usages.put("segment_id", segmentId == "$$null$$" ? JSONObject.NULL : segmentId);
                            usages.put("evaluation_time", segmentIdMap.get("evaluation_time"));
                            usages.put("count", segmentIdMap.get("count"));
                            collections.getJSONArray("usages").put(usages);
                        });
                    });
                });
                result.get(guid).put(collections);
            });
        });
    }

    public synchronized HashMap sendMetering() {

        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>> sendFeatureData =
                this.meteringFeatureData;
        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>> sendPropertyData =
                this.meteringPropertyData;

        this.meteringFeatureData = new ConcurrentHashMap();
        this.meteringPropertyData = new ConcurrentHashMap();

        if (sendFeatureData.size() <= 0 && sendPropertyData.size() <= 0) {
            return new HashMap();
        }

        HashMap<String, JSONArray> result = new HashMap();

        if (sendFeatureData.size() > 0) {
            this.buildRequestBody(sendFeatureData, result, "feature_id");
        }

        if (sendPropertyData.size() > 0) {
            this.buildRequestBody(sendPropertyData, result, "property_id");
        }

        result.forEach((guid, dataArray) -> {
            dataArray.forEach((json) -> {
                this.sendToServer(guid, (JSONObject) json);
            });
        });

        return result;
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
                    BaseLogger.error("Error while sending the metering data. Status code is : " + statusCode + ". Response body: " + responseBody);
                }
            }

            @Override
            public void onFailure(Integer statusCode, String responseBody) {
                BaseLogger.debug(responseBody);
            }
        });

    }
}


