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

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.ServiceImpl;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.core.CoreConstants;

import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class consisting of methods that stores the feature and property evaluations metrics and send the metrics
 * to App Configuration server in intervals.
 */
public class Metering {

    private static Metering instance;
    private final int sendInterval = 600000;
    private String meteringUrl = null;
    private String apikey = null;

    ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
        ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>>> meteringFeatureData =
            new ConcurrentHashMap();
    ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
        ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>>> meteringPropertyData =
            new ConcurrentHashMap();

    /**
     * @return instance of {@link Metering}
     */
    public static synchronized Metering getInstance() {
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

    /**
     * Sets the URL to which metering metrics is to be sent.
     * @param url url
     * @param apikey apikey of App Configuration service instance
     */
    public void setMeteringUrl(String url, String apikey) {
        this.meteringUrl = url;
        this.apikey = apikey;
    }

    /**
     * Stores the feature and property evaluation metrics into hashmaps.
     *
     * @param guid guid of App Configuration service instance
     * @param environmentId environment id of App Configuration service instance
     * @param collectionId collection id
     * @param entityId entity id
     * @param segmentId segment id
     * @param featureId feature id
     * @param propertyId property id
     */
    public synchronized void addMetering(String guid, String environmentId, String collectionId, String entityId,
                                        String segmentId, String featureId, String propertyId) {
        boolean hasData = false;
        HashMap<String, Object> featureJson = new HashMap();
        featureJson.put(ConfigConstants.COUNT, 1);
        String currentDateTime = ServiceImpl.getCurrentDateTime();
        featureJson.put(ConfigConstants.EVALUATION_TIME, currentDateTime);

        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
            ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>>> meteringData
                = featureId != null ? this.meteringFeatureData : this.meteringPropertyData;
        String modifyKey = featureId != null ? featureId : propertyId;

        if (meteringData.containsKey(guid)) {

            if (meteringData.get(guid).containsKey(environmentId)) {

                if (meteringData.get(guid).get(environmentId).containsKey(collectionId)) {
                    if (meteringData.get(guid).get(environmentId).get(collectionId).containsKey(modifyKey)) {
                        if (meteringData.get(guid).get(environmentId).get(collectionId)
                            .get(modifyKey).containsKey(entityId)) {
                            if (meteringData.get(guid).get(environmentId).get(collectionId).get(modifyKey).
                                get(entityId).containsKey(segmentId)) {
                                hasData = true;
                                meteringData.get(guid).get(environmentId).get(collectionId).get(modifyKey).
                                    get(entityId).get(segmentId).put(ConfigConstants.EVALUATION_TIME, currentDateTime);
                                int count = (int) meteringData.get(guid).get(environmentId).get(collectionId).
                                            get(modifyKey).get(entityId).get(segmentId).get(ConfigConstants.COUNT);
                                meteringData.get(guid).get(environmentId).get(collectionId).get(modifyKey).
                                    get(entityId).get(segmentId).put(ConfigConstants.COUNT, count + 1);
                            }
                        } else {
                            ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                            segmentIdMap.put(segmentId, new HashMap());
                            meteringData.get(guid).get(environmentId).get(collectionId).get(modifyKey).put(entityId, segmentIdMap);
                        }
                    } else {
                        ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                        ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> entityIdMap = new ConcurrentHashMap();
                        segmentIdMap.put(segmentId, new HashMap());
                        entityIdMap.put(entityId, segmentIdMap);
                        meteringData.get(guid).get(environmentId).get(collectionId).put(modifyKey, entityIdMap);
                    }
                } else {
                    ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                    ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> entityIdMap = new ConcurrentHashMap();
                    ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>> featureIdMap = new ConcurrentHashMap();
                    segmentIdMap.put(segmentId, new HashMap());
                    entityIdMap.put(entityId, segmentIdMap);
                    featureIdMap.put(modifyKey, entityIdMap);
                    meteringData.get(guid).get(environmentId).put(collectionId, featureIdMap);
                }
            } else {

                ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
                ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> entityIdMap = new ConcurrentHashMap();
                ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>> featureIdMap = new ConcurrentHashMap();
                ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>> collectionIdMap = new ConcurrentHashMap();
                segmentIdMap.put(segmentId, new HashMap());
                entityIdMap.put(entityId, segmentIdMap);
                featureIdMap.put(modifyKey, entityIdMap);
                collectionIdMap.put(collectionId, featureIdMap);
                meteringData.get(guid).put(environmentId, collectionIdMap);
            }
        } else {
            ConcurrentHashMap<String, HashMap<String, Object>> segmentIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> entityIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>> featureIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>> collectionIdMap = new ConcurrentHashMap();
            ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
                ConcurrentHashMap<String, HashMap<String, Object>>>>>> environmentIdMap = new ConcurrentHashMap();
            segmentIdMap.put(segmentId, new HashMap());
            entityIdMap.put(entityId, segmentIdMap);
            featureIdMap.put(modifyKey, entityIdMap);
            collectionIdMap.put(collectionId, featureIdMap);
            environmentIdMap.put(environmentId, collectionIdMap);
            meteringData.put(guid, environmentIdMap);
        }
        if (!hasData) {
            meteringData.get(guid).get(environmentId).get(collectionId).get(modifyKey).get(entityId).put(segmentId, featureJson);
        }
    }

    private synchronized void buildRequestBody(ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>>> sendMeteringData, HashMap<String, JSONArray> result, String key) {
        sendMeteringData.forEach((guid, guidMap) -> {

            if (!result.containsKey(guid)) {
                result.put(guid, new JSONArray());
            }
            guidMap.forEach((environmentId, environmentMap) -> {
                environmentMap.forEach((collectionId, collectionMap) -> {
                    JSONObject collections = new JSONObject();
                    collections.put(ConfigConstants.COLLECTION_ID, collectionId);
                    collections.put(ConfigConstants.ENVIRONMENT_ID, environmentId);
                    collections.put(ConfigConstants.USAGES, new JSONArray());

                    collectionMap.forEach((featureId, featureIdMap) -> {
                        featureIdMap.forEach((entityId, entityMap) -> {
                            entityMap.forEach((segmentId, segmentIdMap) -> {
                                JSONObject usages = new JSONObject();
                                usages.put(key, featureId);
                                usages.put(ConfigConstants.ENTITY_ID,
                                        entityId.equals(ConfigConstants.DEFAULT_ENTITY_ID) ? JSONObject.NULL : entityId);
                                usages.put(ConfigConstants.SEGMENT_ID,
                                        segmentId.equals(ConfigConstants.DEFAULT_SEGMENT_ID) ? JSONObject.NULL : segmentId);
                                usages.put(ConfigConstants.EVALUATION_TIME, segmentIdMap.get(ConfigConstants.EVALUATION_TIME));
                                usages.put(ConfigConstants.COUNT, segmentIdMap.get(ConfigConstants.COUNT));
                                collections.getJSONArray(ConfigConstants.USAGES).put(usages);
                            });
                        });
                    });
                    result.get(guid).put(collections);
                });
            });

        });
    }

    /**
     * Sends the evaluation metrics data to App Configuration billing server.
     *
     * @return JSON data constructed out of hashmaps
     */
    public synchronized HashMap sendMetering() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
            ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>>>>> sendFeatureData = this.meteringFeatureData;
        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String,
            ConcurrentHashMap<String, HashMap<String, Object>>>>>>> sendPropertyData = this.meteringPropertyData;

        this.meteringFeatureData = new ConcurrentHashMap();
        this.meteringPropertyData = new ConcurrentHashMap();

        if (sendFeatureData.size() <= 0 && sendPropertyData.size() <= 0) {
            return new HashMap();
        }

        HashMap<String, JSONArray> result = new HashMap();

        if (sendFeatureData.size() > 0) {
            this.buildRequestBody(sendFeatureData, result, ConfigConstants.FEATURE_ID);
        }

        if (sendPropertyData.size() > 0) {
            this.buildRequestBody(sendPropertyData, result, ConfigConstants.PROPERTY_ID);
        }
        result.forEach((guid, dataArray) -> {
            dataArray.forEach((json) -> {
                int count = ((JSONObject) json).getJSONArray(ConfigConstants.USAGES).length();
                if (count > 25) {
                    this.sendSplitMetering((JSONObject) json, count);
                } else {
                    this.sendToServer((JSONObject) json);
                }
            });
        });
        return result;
    }

    public void sendSplitMetering(JSONObject data, int count) {
        int lim = 0;

        JSONArray subUsagesArray = data.getJSONArray(ConfigConstants.USAGES);

        while (lim < count) {
            int endIndex = Math.min(lim + ConfigConstants.DEFAULT_USAGE_LIMIT, count);
            JSONObject collectionsMap = new JSONObject();
            collectionsMap.put(ConfigConstants.COLLECTION_ID, data.getString(ConfigConstants.COLLECTION_ID));
            collectionsMap.put(ConfigConstants.ENVIRONMENT_ID, data.getString(ConfigConstants.ENVIRONMENT_ID));
            JSONArray usagesArray = new JSONArray();
            for (int i = lim; i < endIndex; i++) {
                usagesArray.put(subUsagesArray.get(i));
            }
            collectionsMap.put(ConfigConstants.USAGES, usagesArray);
            this.sendToServer(collectionsMap);
            lim += ConfigConstants.DEFAULT_USAGE_LIMIT;
        }
    }

    private void sendToServer(JSONObject data) {
        Response response;
        try {
            response = ServiceImpl.getInstance(this.apikey).postMetering(this.meteringUrl, data);
            if (response.getStatusCode() == CoreConstants.REQUEST_SUCCESS_202) {
                BaseLogger.debug("Successfully pushed the metering data.");
            }
        } catch (ServiceResponseException e) {
            BaseLogger.error("Exception occurred while sending metering data to server. Status code:" + e.getStatusCode() + " message: " + e.getMessage());
            if (e.getStatusCode() == CoreConstants.TOO_MANY_REQUESTS || (e.getStatusCode() >= CoreConstants.SERVER_ERROR_BEGIN && e.getStatusCode() <= CoreConstants.SERVER_ERROR_END)) {
                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendToServer(data);
                        timer.cancel();
                    }
                }, sendInterval);
            }
        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "sendToServer", e);
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sendToServer(data);
                    timer.cancel();
                }
            }, sendInterval);
        }
    }
}


