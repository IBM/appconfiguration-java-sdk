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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Evaluation;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.ServiceImpl;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.core.CoreConstants;

import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;

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

    private static Metering instance;

    private String meteringUrl;
    private String environmentId;
    private String collectionId;
    private String apikey;

    private ConcurrentHashMap<String, MeteringRecord> meteringFeatureData = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, MeteringRecord> meteringPropertyData = new ConcurrentHashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        new RetryHandler(this::sendMetering, ConfigConstants.USAGE_SEND_INTERVAL);
    }

    /**
     * Initializes metering configuration.
     *
     * @param url           metering endpoint URL
     * @param collectionId collection identifier
     * @param environmentId environment identifier
     * @param apikey        service API key
     */
    public void init(String url, String collectionId, String environmentId, String apikey) {
        this.meteringUrl = url;
        this.collectionId = collectionId;
        this.environmentId = environmentId;
        this.apikey = apikey;
    }

    /**
     * Records a feature or property evaluation.
     *
     * @param entityId   entity identifier
     * @param segmentId  segment identifier (nullable)
     * @param featureId  feature identifier (nullable)
     * @param propertyId property identifier (nullable)
     */
    public void addMetering(String entityId, String segmentId, String featureId, String propertyId) {
        boolean isFeature = featureId != null;
        String modifyKey = isFeature ? featureId : propertyId;
        String key = buildCompositeKey(modifyKey, entityId, segmentId);
        (isFeature ? meteringFeatureData : meteringPropertyData).compute(key, (k, record) -> {
            String evaluationTime = ServiceImpl.getCurrentDateTime();
            if (record == null) {
                return new MeteringRecord(evaluationTime);
            }
            record.increment(evaluationTime);
            return record;
        });
    }

    /**
     * Populates the evaluation payload from metering records.
     *
     * @param data         metering data map
     * @param result       evaluation payload
     * @param resourceType feature or property identifier key
     */
    private static void buildRequestBody(ConcurrentHashMap<String, MeteringRecord> data, Evaluation result,
        String resourceType) {
        data.forEach((compositeKey, record) -> {
            String[] parts = parseCompositeKey(compositeKey);
            result.addUsage(resourceType, parts[0], parts[1], parts[2], record.getLatestTime(), record.getCount());
        });
    }

    /**
     * Sends the evaluation metrics data to App Configuration billing server.
     *
     * @return evaluation payload constructed from accumulated data
     */
    public synchronized Evaluation sendMetering() {
        // Atomic swap of data maps
        ConcurrentHashMap<String, MeteringRecord> currentFeatureData = this.meteringFeatureData;
        ConcurrentHashMap<String, MeteringRecord> currentPropertyData = this.meteringPropertyData;

        // Reset with new empty maps
        this.meteringFeatureData = new ConcurrentHashMap<>();
        this.meteringPropertyData = new ConcurrentHashMap<>();

        Evaluation result = new Evaluation();
        result.collectionId = collectionId;
        result.environmentId = environmentId;
        if (currentFeatureData.isEmpty() && currentPropertyData.isEmpty()) {
            return result;
        }
        if (!currentFeatureData.isEmpty()) {
            buildRequestBody(currentFeatureData, result, ConfigConstants.FEATURE_ID);
        }
        if (!currentPropertyData.isEmpty()) {
            buildRequestBody(currentPropertyData, result, ConfigConstants.PROPERTY_ID);
        }
        sendSplitMetering(result);
        return result;
    }

    /**
     * Sends evaluation data in batches based on configured usage limits.
     *
     * @param data evaluation payload
     */
    public void sendSplitMetering(Evaluation data) {
        int start = 0;
        int total = data.usages.size();
        while (start < total) {
            Evaluation batch = data.generateUsageLimitedCopy(start, ConfigConstants.DEFAULT_USAGE_LIMIT);
            sendToServer(batch);
            start += ConfigConstants.DEFAULT_USAGE_LIMIT;
        }
    }

    /**
     * Sends a single evaluation payload to the billing server.
     *
     * @param data evaluation payload
     */
    private void sendToServer(Evaluation data) {
        try {
            String payload = MAPPER.writeValueAsString(data);
            Response<String> response = ServiceImpl.getInstance(apikey).post(meteringUrl, payload);
            if (response.getStatusCode() == CoreConstants.REQUEST_SUCCESS_202) {
                BaseLogger.debug("Successfully pushed metering data.");
            }
        } catch (ServiceResponseException e) {
            handleRetry(data, e);
        } catch (Exception e) {
            AppConfigException.logException(getClass().getName(), "sendToServer", e);
            scheduleRetry(data);
        }
    }

    /**
     * Handles retryable server-side failures.
     *
     * @param data evaluation payload
     * @param e    service exception
     */
    private void handleRetry(Evaluation data, ServiceResponseException e) {
        BaseLogger.error("Failed to send metering data. Status code: "
                + e.getStatusCode() + ", message: " + e.getMessage());

        if (e.getStatusCode() == CoreConstants.TOO_MANY_REQUESTS
            || (e.getStatusCode() >= CoreConstants.SERVER_ERROR_BEGIN
            && e.getStatusCode() <= CoreConstants.SERVER_ERROR_END)) {
            scheduleRetry(data);
        }
    }

    /**
     * Schedules a retry attempt after the configured interval.
     *
     * @param data evaluation payload
     */
    private void scheduleRetry(Evaluation data) {
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendToServer(data);
                timer.cancel();
            }
        }, ConfigConstants.USAGE_SEND_INTERVAL);
    }

    /**
     * Builds a composite key for metering aggregation.
     *
     * @param modifyKey feature or property identifier
     * @param entityId  entity identifier
     * @param segmentId segment identifier (nullable)
     * @return composite key string
     */
    private static String buildCompositeKey(String modifyKey, String entityId, String segmentId) {
        return String.join(ConfigConstants.DELIMITER, modifyKey, entityId, segmentId == null ? "" : segmentId);
    }

    /**
     * Parses a composite metering key.
     *
     * @param compositeKey composite key string
     * @return parsed key parts
     */
    private static String[] parseCompositeKey(String compositeKey) {
        return compositeKey.split(ConfigConstants.DELIMITER, -1);
    }

    /**
     * Thread-safe metering record holding evaluation count and latest timestamp.
     */
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
}
