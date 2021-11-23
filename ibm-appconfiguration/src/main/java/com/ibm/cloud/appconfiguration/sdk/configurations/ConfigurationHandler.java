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
import com.ibm.cloud.appconfiguration.sdk.core.ServiceImpl;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.core.CoreConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigMessages;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Connectivity;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.FileManager;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Metering;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.RetryInterface;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.RetryHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Socket;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.SocketHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.URLBuilder;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal class to handle the configuration.
 */
public class ConfigurationHandler {

    private static ConfigurationHandler instance;

    private int retryCount = 3;
    private String collectionId = "";
    private String environmentId = "";
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

    /**
     * @return instance of {@link ConfigurationHandler}
     */
    public static synchronized ConfigurationHandler getInstance() {
        if (instance == null) {
            instance = new ConfigurationHandler();
        }
        return instance;
    }

    private ConfigurationHandler() {
    }

    /**
     * Initialize the configurations.
     *
     * @param apikey apikey of App Configuration service instance
     * @param guid guid/instanceId of App Configuration service instance
     * @param region region name of App Configuration service instance
     * @param overrideServerHost server host. Use for testing purpose
     */
    public void init(String apikey, String guid, String region, String overrideServerHost) {
        this.apikey = apikey;
        this.guid = guid;
        this.region = region;
        this.overrideServerHost = overrideServerHost;

        this.featureMap = new HashMap<>();
        this.propertyMap = new HashMap<>();
        this.segmentMap = new HashMap<>();
    }

    /**
     * @param collectionId collection id
     * @param environmentId environment id
     * @param configurationFile local configuration file path.
     * @param liveConfigUpdateEnabled live configurations update from the server
     */
    public void setContext(String collectionId, String environmentId,
        String configurationFile, Boolean liveConfigUpdateEnabled) {
        this.collectionId = collectionId;
        this.environmentId = environmentId;
        URLBuilder.initWithContext(collectionId, environmentId, region, guid, overrideServerHost);
        Metering.getInstance().setMeteringUrl(URLBuilder.getMeteringUrl(guid), apikey);
        this.liveConfigUpdateEnabled = liveConfigUpdateEnabled;
        this.configurationFile = configurationFile;
        this.isInitialized = true;

        if (this.liveConfigUpdateEnabled) {
            new Thread(() -> checkNetwork()).start();
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

    /**
     * Returns all properties.
     *
     * @return hashmap of all properties and their corresponding {@link Property} objects
     */
    public HashMap<String, Property> getProperties() {
        return this.propertyMap;
    }

    /**
     * Returns the {@link Property} object with the details of the property specified by the {@code propertyId}.
     *
     * @param propertyId the Property Id
     * @return property object
     */
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

    /**
     * Returns all features.
     *
     * @return hashmap of all features and their corresponding {@link Feature} objects
     */
    public HashMap<String, Feature> getFeatures() {
        return this.featureMap;
    }

    /**
     * Returns the {@link Feature} object with the details of the feature specified by the {@code featureId}.
     *
     * @param featureId the Feature Id
     * @return feature object
     */
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
                connectivity.addConnectivityListener(isConnected -> connectionHandler(isConnected));
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
            this.onSocketRetry = false;
            new Thread(() -> this.startWebSocket()).start();
        }
    }

    private void startWebSocket() {
        try {
            IamAuthenticator iamAuthenticator = ServiceImpl.getIamAuthenticator();
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION,
            iamAuthenticator.requestToken().getTokenType() + " " + iamAuthenticator.getToken());

            if (this.socket != null) {
                this.socket.cancel();
                this.socket = null;
            }
            String socketUrl = URLBuilder.getWebSocketUrl();
            this.socket = new Socket.Builder().url(socketUrl).headers(headers).listener(this.getSocketHandler()).build();
            this.socket.connect();
        } catch (Exception e) {
            BaseLogger.error("web socket failed " + e.getLocalizedMessage());
        }
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
            if (data.has(ConfigConstants.FEATURES)) {
                this.featureMap = new HashMap<>();
                try {
                    JSONArray array = (JSONArray) data.get(ConfigConstants.FEATURES);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject featureJson = array.getJSONObject(i);
                        Feature feature = new Feature(featureJson);
                        this.featureMap.put(feature.getFeatureId(), feature);
                    }
                } catch (Exception e) {
                    AppConfigException.logException(this.getClass().getName(), "loadConfigurations", e);
                }
            }

            if (data.has(ConfigConstants.PROPERTIES)) {
                this.propertyMap = new HashMap<>();
                try {
                    JSONArray array = (JSONArray) data.get(ConfigConstants.PROPERTIES);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject propertyJson = array.getJSONObject(i);
                        Property property = new Property(propertyJson);
                        this.propertyMap.put(property.getPropertyId(), property);
                    }
                } catch (Exception e) {
                    AppConfigException.logException(this.getClass().getName(), "loadConfigurations", e);
                }
            }

            if (data.has(ConfigConstants.SEGMENTS)) {
                this.segmentMap = new HashMap<>();
                try {
                    JSONArray array = (JSONArray) data.get(ConfigConstants.SEGMENTS);
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

    /**
     * Records each of feature and property evaluations done by sending it to {@link Metering}.
     *
     * @param featureId feature id
     * @param propertyId property id
     * @param entityId entity id
     * @param segmentId segment id
     */
    public void recordValuation(String featureId, String propertyId, String entityId, String segmentId) {

        Metering.getInstance().addMetering(guid, environmentId,
            collectionId, entityId, segmentId, featureId, propertyId);
    }

    /**
     * Property evaluation.
     *
     * @param property property object
     * @param entityId entity id
     * @param entityAttributes entity attributes JSON object
     * @return property evaluated value
     */
    public Object propertyEvaluation(Property property, String entityId, JSONObject entityAttributes) {
        JSONObject resultDict = new JSONObject();
        resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, ConfigConstants.DEFAULT_SEGMENT_ID);
        resultDict.put(ConfigConstants.VALUE, new Object());

        try {
            if (entityAttributes == null || entityAttributes.isEmpty()) {
                return property.getValue();
            }
            JSONArray segmentRules = property.getSegmentRules();
            if (segmentRules.length() > 0) {
                Map<Integer, SegmentRules> rulesMap = this.parseRules(segmentRules);
                resultDict = evaluateRules(rulesMap, entityAttributes, null, property);
                return resultDict.get(ConfigConstants.VALUE);
            } else {
                return property.getValue();
            }
        } finally {
            String propertyId = property.getPropertyId();
            this.recordValuation(null, propertyId, entityId,
                resultDict.getString(ConfigConstants.EVALUATED_SEGMENT_ID));
        }
    }

    /**
     * Feature evaluation.
     *
     * @param feature feature object
     * @param isEnabled feature object's "enabled" value (true/false)
     * @param entityId entity id
     * @param entityAttributes entity attributes JSON object
     * @return feature evaluated value
     */
    public Object featureEvaluation(Feature feature, Boolean isEnabled, String entityId, JSONObject entityAttributes) {

        JSONObject resultDict = new JSONObject();
        resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, ConfigConstants.DEFAULT_SEGMENT_ID);
        resultDict.put(ConfigConstants.VALUE, new Object());

        try {
            if (isEnabled) {
                if (entityAttributes == null || entityAttributes.isEmpty()) {
                    return feature.getEnabledValue();
                }
                JSONArray segmentRules = feature.getSegmentRules();
                if (segmentRules.length() > 0) {
                    Map<Integer, SegmentRules> rulesMap = this.parseRules(segmentRules);
                    resultDict = evaluateRules(rulesMap, entityAttributes, feature, null);
                    return resultDict.get(ConfigConstants.VALUE);
                } else {
                    return feature.getEnabledValue();
                }
            } else {
                return feature.getDisabledValue();
            }
        } finally {
            String featureId = feature.getFeatureId();
            this.recordValuation(featureId, null, entityId,
                resultDict.getString(ConfigConstants.EVALUATED_SEGMENT_ID));
        }
    }

    private JSONObject evaluateRules(Map<Integer, SegmentRules> rulesMap, JSONObject entityAttributes,
                                    Feature feature, Property property) {

        JSONObject resultDict = new JSONObject();
        resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, ConfigConstants.DEFAULT_SEGMENT_ID);
        resultDict.put(ConfigConstants.VALUE, new Object());

        try {
            for (int i = 1; i <= rulesMap.size(); i++) {

                SegmentRules segmentRule = rulesMap.get(i);
                if (segmentRule != null) {
                    for (int level = 0; level < segmentRule.getRules().length(); level++) {
                        JSONObject rule = segmentRule.getRules().getJSONObject(level);
                        JSONArray segments = rule.getJSONArray(ConfigConstants.SEGMENTS);
                        for (int innerLevel = 0; innerLevel < segments.length(); innerLevel++) {
                            String segmentKey = segments.getString(innerLevel);
                            if (this.evaluateSegment(segmentKey, entityAttributes)) {
                                resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, segmentKey);
                                if (segmentRule.getValue().equals("$default")) {
                                    if (feature != null) {
                                        resultDict.put(ConfigConstants.VALUE, feature.getEnabledValue());
                                    } else {
                                        resultDict.put(ConfigConstants.VALUE, property.getValue());
                                    }
                                } else {
                                    resultDict.put(ConfigConstants.VALUE, segmentRule.getValue());
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
            resultDict.put(ConfigConstants.VALUE, feature.getEnabledValue());
        } else {
            resultDict.put(ConfigConstants.VALUE, property.getValue());
        }
        return resultDict;
    }

    private Boolean evaluateSegment(String segmentKey, JSONObject entityAttributes) {

        if (this.segmentMap.containsKey(segmentKey)) {
            Segment segment = this.segmentMap.get(segmentKey);
            return segment.evaluateRule(entityAttributes);
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
            try {
                Response response = ServiceImpl.getInstance(apikey, overrideServerHost).getConfig(url);
                if (response.getStatusCode() >= CoreConstants.REQUEST_SUCCESS_200
                && response.getStatusCode() <= CoreConstants.REQUEST_SUCCESS_299) {
                    try {
                        if (configRetry != null) {
                            configRetry.cancel();
                            configRetry = null;
                        }
                        HashMap<String, Object> map = new ObjectMapper().readValue((String) response.getResult(),
                        HashMap.class);
                        writeServerFile(map);
                    } catch (Exception e) {
                        AppConfigException.logException(this.getClass().getName(), "fetchFromApi", e);
                    }
                } else {
                    BaseLogger.error(ConfigMessages.CONFIG_API_ERROR);
                    if (retryCount > 0) {
                        fetchFromApi();
                    } else {
                        retryCount = 3;
                        startConfigRetryTimer();
                    }
                }
            } catch (Exception e) {
                BaseLogger.error("Response Object is " + e.getLocalizedMessage());
            }
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
        if (this.socket == null) {
            return;
        }
        socketRetry = new RetryHandler(new RetryInterface() {
            @Override
            public void retryMethod() {
                startWebSocket();
                socketRetry.cancel();
                socketRetry = null;
            }
        }, 5000);
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
                    startSocketRetryTimer();
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
