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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class consisting of methods that stores the feature and property evaluations metrics and send the metrics
 * to App Configuration server in intervals.
 */
public class Metering {
    private static final String DELIMITER = "\u001F";
    private static Metering instance;
    private final int sendInterval = 600000;
    private String meteringUrl = null;
    private String apikey = null;


    private ConcurrentHashMap<String, MeteringRecord> meteringFeatureData = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, MeteringRecord> meteringPropertyData = new ConcurrentHashMap<>();


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
     *
     * @param url    url
     * @param apikey apikey of App Configuration service instance
     */
    public void setMeteringUrl(String url, String apikey) {
        this.meteringUrl = url;
        this.apikey = apikey;
    }

    /**
     * Stores the feature and property evaluation metrics into hashmaps.
     *
     * @param guid          guid of App Configuration service instance
     * @param environmentId environment id of App Configuration service instance
     * @param collectionId  collection id
     * @param entityId      entity id
     * @param segmentId     segment id
     * @param featureId     feature id
     * @param propertyId    property id
     */
    public void addMetering(String guid, String environmentId, String collectionId, String entityId,
                            String segmentId, String featureId, String propertyId) {

        String key = buildCompositeKey(guid, environmentId, collectionId,
                featureId != null ? featureId : propertyId,
                entityId, segmentId);

        ConcurrentHashMap<String, MeteringRecord> dataMap =
                featureId != null ? meteringFeatureData : meteringPropertyData;

        dataMap.compute(key, (k, record) -> {
            String evaluationTime = ServiceImpl.getCurrentDateTime(); // Time captured atomically

            if (record == null) {
                return new Metering.MeteringRecord(evaluationTime);
            }
            record.increment(evaluationTime);
            return record;
        });
    }

    private void buildRequestBody(ConcurrentHashMap<String, MeteringRecord> sendMeteringData,
                                  HashMap<String, JSONArray> result, String key) {

        sendMeteringData.forEach((compositeKey, meteringRecord) -> {
            String[] keyParts = parseCompositeKey(compositeKey);
            if (keyParts.length != 6) return;

            String guid = keyParts[0];
            String environmentId = keyParts[1];
            String collectionId = keyParts[2];
            String featureOrPropertyId = keyParts[3];
            String entityId = keyParts[4];
            String segmentId = keyParts[5];

            // Get or create the GUID entry in result
            if (!result.containsKey(guid)) {
                result.put(guid, new JSONArray());
            }

            JSONArray guidArray = result.get(guid);

            // Find or create the collection for this environment+collection
            JSONObject collection = findOrCreateCollection(guidArray, environmentId, collectionId);
            JSONArray usages = collection.getJSONArray(ConfigConstants.USAGES);

            // Create usage object - SAME FORMAT AS BEFORE
            JSONObject usage = new JSONObject();
            usage.put(key, featureOrPropertyId);
            usage.put(ConfigConstants.ENTITY_ID,
                    entityId.equals(ConfigConstants.DEFAULT_ENTITY_ID) ? JSONObject.NULL : entityId);
            usage.put(ConfigConstants.SEGMENT_ID,
                    segmentId.equals(ConfigConstants.DEFAULT_SEGMENT_ID) ? JSONObject.NULL : segmentId);
            usage.put(ConfigConstants.EVALUATION_TIME, meteringRecord.getLatestTime());
            usage.put(ConfigConstants.COUNT, meteringRecord.getCount());

            usages.put(usage);
        });
    }

    /**
     * Sends the evaluation metrics data to App Configuration billing server.
     *
     * @return JSON data constructed out of hashmaps
     */
    public synchronized HashMap sendMetering() {
        // Atomic swap of data maps
        ConcurrentHashMap<String, MeteringRecord> currentFeatureData = this.meteringFeatureData;
        ConcurrentHashMap<String, MeteringRecord> currentPropertyData = this.meteringPropertyData;

        // Reset with new empty maps
        this.meteringFeatureData = new ConcurrentHashMap<>();
        this.meteringPropertyData = new ConcurrentHashMap<>();

        if (currentFeatureData.isEmpty() && currentPropertyData.isEmpty()) {
            return new HashMap();
        }

        HashMap<String, JSONArray> result = new HashMap();

        if (!currentFeatureData.isEmpty()) {
            this.buildRequestBody(currentFeatureData, result, ConfigConstants.FEATURE_ID);
        }

        if (!currentPropertyData.isEmpty()) {
            this.buildRequestBody(currentPropertyData, result, ConfigConstants.PROPERTY_ID);
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

    private JSONObject findOrCreateCollection(JSONArray guidArray, String environmentId, String collectionId) {
        // Look for existing collection with same environment and collection ID
        for (int i = 0; i < guidArray.length(); i++) {
            JSONObject collection = guidArray.getJSONObject(i);
            if (collection.getString(ConfigConstants.ENVIRONMENT_ID).equals(environmentId)
                    && collection.getString(ConfigConstants.COLLECTION_ID).equals(collectionId)) {
                return collection;
            }
        }

        // Create new collection if not found
        JSONObject newCollection = new JSONObject();
        newCollection.put(ConfigConstants.COLLECTION_ID, collectionId);
        newCollection.put(ConfigConstants.ENVIRONMENT_ID, environmentId);
        newCollection.put(ConfigConstants.USAGES, new JSONArray());
        guidArray.put(newCollection);

        return newCollection;
    }
    private static class MeteringRecord {
        private final AtomicInteger count = new AtomicInteger(1);
        private final AtomicReference<String> latestTime;

         MeteringRecord(String time) {
            this.latestTime = new AtomicReference<>(time);
        }

        public void increment(String newTime) {
            count.incrementAndGet();
            // Atomic update - always keeps the latest timestamp
            latestTime.getAndUpdate(currentTime ->
                    newTime.compareTo(currentTime) > 0 ? newTime : currentTime
            );
        }

        public int getCount() {
            return count.get();
        }

        public String getLatestTime() {
            return latestTime.get();
        }
    }

    private String buildCompositeKey(String guid, String environmentId, String collectionId,
                                     String modifyKey, String entityId, String segmentId) {
        return String.join(DELIMITER,
                nullToEmpty(guid),
                nullToEmpty(environmentId),
                nullToEmpty(collectionId),
                nullToEmpty(modifyKey),
                nullToEmpty(entityId),
                nullToEmpty(segmentId)
        );
    }

    private String[] parseCompositeKey(String compositeKey) {
        return compositeKey.split(DELIMITER, -1);
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }
}


