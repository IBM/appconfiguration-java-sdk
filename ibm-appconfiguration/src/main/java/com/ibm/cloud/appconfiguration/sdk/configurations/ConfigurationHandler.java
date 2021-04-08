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

package com.ibm.cloud.appconfiguration.sdk.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import com.ibm.cloud.appconfiguration.sdk.core.*;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.*;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationHandler {

    private static ConfigurationHandler instance;

    private int retryCount = 3;
    private String collectionId = "";
    private String apikey = "";
    private String guid = "";
    private String region = "";
    private Boolean isInitialized = false;
    private ConfigurationUpdateListener configurationUpdateListener = null;
    private HashMap<String, Feature> featureMap = new HashMap();
    private HashMap<String, Property> propertyMap = new HashMap();
    private HashMap<String, Segment> segmentMap = new HashMap();
    private Boolean liveConfigUpdateEnabled = true;
    private String configurationFile = null;
    private Boolean onSocketRetry = false;
    private String overrideServerHost = null;

    private RetryHandler configRetry;
    private RetryHandler socketRetry;

    private Socket socket = null;
    private SocketHandler socketHandler;
    private Connectivity connectivity = null;
    private Boolean isNetWorkConnected = true;

    public synchronized static ConfigurationHandler getInstance() {
        if (instance == null) {
            instance = new ConfigurationHandler();
        }
        return instance;
    }

    private ConfigurationHandler() {
    }

    public void init(String apikey,
                     String guid,
                     String region,
                     String overrideServerHost) {

        this.apikey = apikey;
        this.guid = guid;
        this.region = region;
        this.overrideServerHost = overrideServerHost;

        this.featureMap = new HashMap<>();
        this.propertyMap = new HashMap<>();
        this.segmentMap = new HashMap<>();

        Metering.getInstance().setMeteringUrl(URLBuilder.getMeteringUrl(guid), apikey);

        new Thread(() -> {
            checkNetwork();
        }).start();
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
        URLBuilder.initWithCollectionId(collectionId, region, guid, overrideServerHost);
        this.isInitialized = true;
    }
    
    public void fetchConfigurationFromFile(Boolean liveConfigUpdateEnabled, String configurationFile) {
        this.liveConfigUpdateEnabled = liveConfigUpdateEnabled;
        this.configurationFile = configurationFile;

        if (this.liveConfigUpdateEnabled) {
            new Thread(() -> {
                checkNetwork();
            }).start();
        }
    }

    public synchronized void loadData() {

        if (this.isInitialized) {

            if (Validators.validateString(this.configurationFile)) {
                this.getFileData(this.configurationFile);
            }
            this.loadConfigurations();
            if (this.liveConfigUpdateEnabled) {
                this.fetchConfigData();
            } else {
                if (this.socket != null) {
                    this.socket.cancel();
                }
            }
        } else {
            BaseLogger.debug(ConfigMessages.CONFIG_HANDLER_INIT_ERROR);
        }
    }

    public void registerConfigurationUpdateListener(ConfigurationUpdateListener listener) {

        if (this.isInitialized) {
            this.configurationUpdateListener = listener;
        } else {
            BaseLogger.debug(ConfigMessages.CONFIG_HANDLER_INIT_ERROR);
        }
    }

    public HashMap<String, Property> getProperties() {
        return this.propertyMap;
    }

    public Property getProperty(String propertyId) {
        if (propertyMap.containsKey(propertyId)) {
            return propertyMap.get(propertyId);
        } else {
            this.loadConfigurations();
            if (propertyMap.containsKey(propertyId)) {
                return propertyMap.get(propertyId);
            } else {
                BaseLogger.error(ConfigMessages.PROPERTY_INVALID + propertyId);
            }
        }
        return null;
    }

    public HashMap<String, Feature> getFeatures() {
        return this.featureMap;
    }

    public Feature getFeature(String featureId) {
        if (featureMap.containsKey(featureId)) {
            return featureMap.get(featureId);
        } else {
            this.loadConfigurations();
            if (featureMap.containsKey(featureId)) {
                return featureMap.get(featureId);
            } else {
                BaseLogger.error(ConfigMessages.FEATURE_INVALID + featureId);
            }
        }
        return null;
    }

    private void checkNetwork() {

        if (this.liveConfigUpdateEnabled) {
            if (connectivity == null) {
                connectivity = Connectivity.getInstance();
                connectivity.addConnectivityListener(new ConnectivityListener() {
                    @Override
                    public void onConnectionChange(Boolean isConnected) {
                        connectionHandler(isConnected);
                    }
                });
                connectivity.checkConnection();
            }
        } else {
            connectivity = null;
        }
    }

    private void connectionHandler(Boolean isConnected) {
        if (!this.liveConfigUpdateEnabled) {
            connectivity = null;
            return;
        }
        if (isConnected) {
            if (!this.isNetWorkConnected) {
                this.isNetWorkConnected = true;
                this.fetchConfigData();
            }
        } else {
            BaseLogger.debug(ConfigMessages.NO_INTERNET_CONNECTION_ERROR);
            this.isNetWorkConnected = false;
        }
    }

    private void fetchConfigData() {
        if (this.isInitialized) {
            this.retryCount = 3;
            this.fetchFromApi();
            this.startWebSocket();
        }
    }

    private void startWebSocket() {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", this.apikey);

        if (this.socket != null) {
            this.socket.cancel();
        }
        String socketUrl = URLBuilder.getWebSocketUrl();
        this.socket = new Socket.Builder().url(socketUrl).headers(headers).listener(this.getSocketHandler()).build();
        this.socket.connect();
    }

    private void getFileData(String filePath) {
        JSONObject data = FileManager.readFiles(filePath);

        if (!data.isEmpty()) {
            try {
                HashMap<String, Object> result =
                        new ObjectMapper().readValue(data.toString(), HashMap.class);
                this.writeToFile(result);
            } catch (Exception e) {
                AppConfigException.logException(this.getClass().getName(), "getFileData", e);
            }

        }
    }

    private void loadConfigurations() {
        JSONObject data = FileManager.readFiles(null);
        if (!data.isEmpty()) {
            if (data.has("features")) {
                this.featureMap = new HashMap<>();
                try {
                    JSONArray array = (JSONArray) data.get("features");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject featureJson = array.getJSONObject(i);
                        Feature feature = new Feature(featureJson);
                        this.featureMap.put(feature.getFeatureId(), feature);
                    }
                } catch (Exception e) {
                    AppConfigException.logException(this.getClass().getName(), "loadConfigurations", e);
                }
            }

            if (data.has("properties")) {
                this.propertyMap = new HashMap<>();
                try {
                    JSONArray array = (JSONArray) data.get("properties");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject propertyJson = array.getJSONObject(i);
                        Property property = new Property(propertyJson);
                        this.propertyMap.put(property.getPropertyId(), property);
                    }
                } catch (Exception e) {
                    AppConfigException.logException(this.getClass().getName(), "loadConfigurations", e);
                }
            }

            if (data.has("segments")) {
                this.segmentMap = new HashMap<>();
                try {
                    JSONArray array = (JSONArray) data.get("segments");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject segmentJson = array.getJSONObject(i);
                        Segment segment = new Segment(segmentJson);
                        this.segmentMap.put(segment.getSegmentId(), segment);
                    }
                } catch (Exception e) {
                    AppConfigException.logException(this.getClass().getName(), "loadConfigurations", e);
                }
            }
        }
    }

    public void recordValuation(String featureId,String propertyId, String identityId, String segmentId ) {
        Metering.getInstance().addMetering(this.guid, this.collectionId, identityId, segmentId, featureId, propertyId);
    }

    public Object propertyEvaluation(Property property, String identityId, JSONObject identityAttributes) {

        JSONObject resultDict = new JSONObject();
        resultDict.put("evaluated_segment_id", ConfigConstants.DEFAULT_SEGMENT_ID);
        resultDict.put("value", new Object());

        try {
            if (identityAttributes == null || identityAttributes.isEmpty()) {
                return property.getValue();
            }
            JSONArray segmentRules = property.getSegmentRules();
            if (segmentRules.length() > 0) {
                Map<Integer, SegmentRules> rulesMap = this.parseRules(segmentRules);
                resultDict = evaluateRules(rulesMap,identityAttributes, null, property);
                return resultDict.get("value");
            } else {
                return property.getValue();
            }
        } finally {
            String propertyId = property.getPropertyId();
            this.recordValuation(null, propertyId, identityId, resultDict.getString("evaluated_segment_id"));
        }
    }

    public Object featureEvaluation(Feature feature, String identityId, JSONObject identityAttributes) {

        JSONObject resultDict = new JSONObject();
        resultDict.put("evaluated_segment_id", ConfigConstants.DEFAULT_SEGMENT_ID);
        resultDict.put("value", new Object());

        try {
            if (feature.isEnabled()) {
                if (identityAttributes == null || identityAttributes.isEmpty()) {
                    return feature.getEnabledValue();
                }
                JSONArray segmentRules = feature.getSegmentRules();
                if (segmentRules.length() > 0) {
                    Map<Integer, SegmentRules> rulesMap = this.parseRules(segmentRules);
                    resultDict = evaluateRules(rulesMap,identityAttributes, feature, null);
                    return resultDict.get("value");
                } else {
                    return feature.getEnabledValue();
                }
            } else {
                return feature.getDisabledValue();
            }
        } finally {
            String featureId = feature.getFeatureId();
            this.recordValuation(featureId, null, identityId, resultDict.getString("evaluated_segment_id"));
        }
    }

    private JSONObject evaluateRules(Map<Integer, SegmentRules> rulesMap, JSONObject identityAttributes, Feature feature, Property property ) {

        JSONObject resultDict = new JSONObject();
        resultDict.put("evaluated_segment_id", ConfigConstants.DEFAULT_SEGMENT_ID);
        resultDict.put("value", new Object());

        try {
            for (int i = 1; i <= rulesMap.size(); i++) {

                SegmentRules segmentRule = rulesMap.get(i);
                if (segmentRule != null) {
                    for (int level = 0; level < segmentRule.getRules().length(); level++) {
                        JSONObject rule = segmentRule.getRules().getJSONObject(level);
                        JSONArray segments = rule.getJSONArray("segments");
                        for (int innerLevel = 0; innerLevel < segments.length(); innerLevel++) {
                            String segmentKey = segments.getString(innerLevel);
                            if (this.evaluateSegment(segmentKey, identityAttributes)) {
                                if (segmentRule.getValue().equals("$default")) {
                                    resultDict.put("evaluated_segment_id", segmentKey);
                                    if (feature != null) {
                                        resultDict.put("value", feature.getEnabledValue());
                                    } else {
                                        resultDict.put("value", property.getValue());
                                    }
                                } else {
                                    resultDict.put("value", segmentRule.getValue());
                                }
                                return resultDict;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "RuleEvaluation", e);
        }
        if (feature != null) {
            resultDict.put("value", feature.getEnabledValue());
        } else {
            resultDict.put("value", property.getValue());
        }
        return resultDict;
    }

    private Boolean evaluateSegment(String segmentKey, JSONObject identityAttributes) {

        if (this.segmentMap.containsKey(segmentKey)) {
            Segment segment = this.segmentMap.get(segmentKey);
            return segment.evaluateRule(identityAttributes);
        }
        return false;

    }

    private Map<Integer, SegmentRules> parseRules(JSONArray segmentRulesList) {
        Map<Integer, SegmentRules> ruleMap = new HashMap<>();
        for (int i = 0; i < segmentRulesList.length(); i++) {
            try {
                JSONObject rule = segmentRulesList.getJSONObject(i);
                SegmentRules segmentRules = new SegmentRules(rule);
                ruleMap.put(segmentRules.getOrder(), segmentRules);
            } catch (Exception e) {
                AppConfigException.logException(this.getClass().getName(), "parseRules", e);
            }
        }
        return ruleMap;
    }

    private void writeServerFile(HashMap json) {
        if (this.liveConfigUpdateEnabled) {
            this.writeToFile(json);
        }
    }

    private void writeToFile(HashMap json) {
        FileManager.storeFile(json);
        this.loadConfigurations();
        if (this.configurationUpdateListener != null) {
            this.configurationUpdateListener.onConfigurationUpdate();
        }
    }

    private void fetchFromApi() {
        if (this.isInitialized) {

            String url = URLBuilder.getConfigUrl();
            this.retryCount -= 1;
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", this.apikey);
            headers.put("Content-Type", "application/json");

            BaseRequest baseRequest = new BaseRequest.Builder().url(url).method(RequestTypes.GET).headers(headers).build();
            baseRequest.execute(new AppConfigurationResponseListener() {
                @Override
                public void onSuccess(Integer statusCode, String responseBody) {
                    if (statusCode >= CoreConstants.REQUEST_SUCCESS_200 && statusCode <= CoreConstants.REQUEST_SUCCESS_299) {
                        try {
                            if (configRetry != null) {
                                configRetry.cancel();
                                configRetry = null;
                            }
                            HashMap<String, Object> map = new ObjectMapper().readValue(responseBody, HashMap.class);
                            writeServerFile(map);
                        } catch (Exception e) {
                            AppConfigException.logException(this.getClass().getName(), "fetchFromApi", e);
                        }
                    }
                }

                @Override
                public void onFailure(Integer statusCode, String responseBody) {
                    BaseLogger.error(ConfigMessages.CONFIG_API_ERROR);
                    if (retryCount > 0) {
                        fetchFromApi();
                    } else {
                        retryCount = 3;
                        startConfigRetryTimer();
                    }
                }
            });
        } else {
            BaseLogger.debug(ConfigMessages.CONFIG_HANDLER_INIT_ERROR);
        }
    }

    private synchronized void startConfigRetryTimer() {

        if (this.configRetry != null) {
            this.configRetry.cancel();
            this.configRetry = null;
        }
        configRetry = new RetryHandler(new RetryInterface() {
            @Override
            public void retryMethod() {
                fetchFromApi();
            }
        }, -1);
    }

    private synchronized void startSocketRetryTimer() {

        if (this.socketRetry != null) {
            this.socketRetry.cancel();
            this.socketRetry = null;
        }
        socketRetry = new RetryHandler(new RetryInterface() {
            @Override
            public void retryMethod() {
                startWebSocket();
            }
        }, -1);
    }

    private SocketHandler getSocketHandler() {

        if (socketHandler == null) {
            socketHandler = new SocketHandler() {
                @Override
                public void onOpen(String openMessage) {
                    if (onSocketRetry) {
                        onSocketRetry = false;
                        fetchFromApi();
                    }
                    if (socketRetry != null) {
                        socketRetry.cancel();
                        socketRetry = null;
                    }
                    BaseLogger.debug("Received opened connection from socket");
                }

                @Override
                public void onMessage(String message) {
                    fetchFromApi();
                    BaseLogger.debug("Received message from socket " + message);
                }

                @Override
                public void onClose(String closeMessage) {
                    BaseLogger.debug("Received close connection from socket");
                    onSocketRetry = true;
                }

                @Override
                public void onError(Exception e) {
                    BaseLogger.debug("Received error from socket " + e.toString());
                    startSocketRetryTimer();
                }
            };
        }
        return socketHandler;
    }
}
