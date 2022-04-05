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
import com.ibm.cloud.appconfiguration.sdk.configurations.models.ConfigurationOptions;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal class to handle the configuration.
 */
public class ConfigurationHandler {

    private static ConfigurationHandler instance;

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
    private String bootstrapFile = null;
    private Boolean onSocketRetry = false;
    private String overrideServerHost = null;
    private String persistentCacheLocation = null;

    private RetryHandler configRetry;
    private RetryHandler socketRetry;

    private Socket socket = null;
    private SocketHandler socketHandler;
    private Connectivity connectivity = null;
    private Boolean isNetWorkConnected = true;
    private final String className = this.getClass().getName();

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
     * @param configOption configOption object contains three fields
     *                     configOption.persistentCacheDir : The SDK will create a file - 'appconfiguration.json'
     *                     in the specified directory and it will be used as the persistent cache to store the
     *                     App Configuration service information.
     *                     configOption.bootstrapFile : Absolute path of configuration file. This parameter
     *                     when passed along with `liveConfigUpdateEnabled` value will drive the SDK to use the
     *                     configurations present in this file to perform feature and property evaluations
     *                     configOption.liveConfigUpdateEnabled : live configurations update from the server.
     *                     Set this value to `false` if the new configuration values shouldn't be fetched from the server.
     */
    public void setContext(String collectionId, String environmentId, ConfigurationOptions configOption) {
        if (configOption != null) {
            this.liveConfigUpdateEnabled = configOption.getLiveConfigUpdateEnabled();
            this.bootstrapFile = configOption.getBootstrapFile();
            this.persistentCacheLocation = configOption.getPersistentCacheDirectory();
        }
        this.collectionId = collectionId;
        this.environmentId = environmentId;
        URLBuilder.initWithContext(collectionId, environmentId, region, guid, overrideServerHost);
        Metering.getInstance().setMeteringUrl(URLBuilder.getMeteringUrl(guid), apikey);
        this.isInitialized = true;

        if (this.liveConfigUpdateEnabled) {
            // if live config update is enabled, start a daemon Timer thread that periodically checks the internet connectivity
            connectivity = Connectivity.getInstance();
            connectivity.addConnectivityListener(this::connectionHandler);
        }
        loadDataByConfiguration();
    }

    private void loadDataByConfiguration() {
        if (Validators.validateString(persistentCacheLocation) && Validators.validateString(bootstrapFile)) {
            loadBootStrapFileAndPersistanceData(bootstrapFile, persistentCacheLocation, liveConfigUpdateEnabled);
        } else if (Validators.validateString(persistentCacheLocation)) {
            loadPersistanceCacheData(persistentCacheLocation);
        } else if (Validators.validateString(bootstrapFile)) {
            loadBootstrapFileData(bootstrapFile, liveConfigUpdateEnabled);
        } else if (!Validators.validateString(persistentCacheLocation) && !Validators.validateString(bootstrapFile)) {
            loadData();
        }
    }

    /*
     *  it will create the configuration file in given directory.
     *  it will read the data from the file and populate it map
     *  make the API call and populate the response in maps and also store the response in persistentCacheDir file
     */
    private void loadPersistanceCacheData(String persistentCacheDir) {
        JSONObject data = FileManager.readFiles(persistentCacheDir);
        loadConfigurationsAndPopulateInMap(data);
        loadData();
    }

    private void loadBootstrapFileData(String bootstrapFile, Boolean liveConfigUpdateEnabled) {
        JSONObject data = FileManager.readFiles(bootstrapFile);
        loadConfigurationsAndPopulateInMap(data);
        loadData();
    }

    private void loadBootStrapFileAndPersistanceData(String bootstrapFile, String persistentCacheDir, Boolean liveConfigUpdateEnabled) {
        JSONObject persistentData = FileManager.readFiles(persistentCacheDir);
        if (!persistentData.isEmpty()) {
            loadConfigurationsAndPopulateInMap(persistentData);
        } else {
            JSONObject data = FileManager.readFiles(bootstrapFile);
            loadConfigurationsAndPopulateInMap(data);
            try {
                HashMap<String, Object> map = new ObjectMapper().readValue(data.toString(), HashMap.class);
                FileManager.createAndStoreFile(map, persistentCacheDir);
            } catch (Exception e) {
                AppConfigException.logException(this.className, " loadBootStrapFileAndPersistanceData", e);
            }
        }
        loadData();
    }


    public synchronized void loadData() {
        if (this.isInitialized) {
            if (this.liveConfigUpdateEnabled) {
                this.fetchConfigData();
            } else if (this.socket != null) {
                this.socket.cancel();
                this.socket = null;
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
        }
        BaseLogger.error(ConfigMessages.PROPERTY_INVALID + propertyId);
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
        }
        //Removed code which is not required
        BaseLogger.error(ConfigMessages.FEATURE_INVALID + featureId);
        return null;
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
        this.fetchFromApi();
        initializeWebSocket();
    }

    private void initializeWebSocket() {
        if (this.isInitialized) {
            this.onSocketRetry = false;
            Thread websocketThread = new Thread(() -> this.startWebSocket());
            websocketThread.setDaemon(true);
            websocketThread.start();
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


    public void loadConfigurationsAndPopulateInMap(JSONObject data) {

        String methodName = "loadConfigurationsAndPopulateInMap";

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
                    AppConfigException.logException(this.className, methodName, e);
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
                    AppConfigException.logException(this.className, methodName, e);
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
                    AppConfigException.logException(this.className, methodName, e);
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
            AppConfigException.logException(this.className, "RuleEvaluation", e);
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
                AppConfigException.logException(this.className, "parseRules", e);
            }
        }
        return ruleMap;
    }


    private void fetchFromApi() {
        String methodName = "fetchFromApi";
        /*
            2xx - Do not retry (Success)
            3xx - Do not retry (Redirect)
            4xx - Do not retry (Client errors)
            429 - Retry ("Too Many Requests")
            5xx - Retry (Server errors)

            The imported package `com.ibm.cloud.sdk.core` is configured to retry the API request in case of failure.
            Hence, we no need to write the retry logic again.
            The API call gets retried within getConfig() for 3 times in an exponential interval(1s, 2s, 4s) between each retry.
            If all the 3 retries fails, appropriate exceptions are raised.
            For 429 error code - The getConfig() will retry the request 3 times in an interval of time mentioned in ["retry-after"] header.
            If all the 3 retries exhausts the call is returned and appropriate exceptions are raised.

            When all the above retries fails, we schedule our own Timer to retry after 10 minutes for the response status_codes [429 & 5xx].
            All other status codes are not retried nor a retry is scheduled.
            User has to take immediate action and resolve it themselves by looking at the error logs.
         */
        if (this.isInitialized) {
            String url = URLBuilder.getConfigUrl();
            Response response;
            try {
                response = ServiceImpl.getInstance(apikey, overrideServerHost).getConfig(url);
            } catch (ServiceResponseException e) {
                BaseLogger.error("Exception occurred while fetching configurations. Status code:" + e.getStatusCode() + " message: " + e.getMessage());
                if (e.getStatusCode() == CoreConstants.TOO_MANY_REQUESTS || (e.getStatusCode() >= CoreConstants.SERVER_ERROR_BEGIN && e.getStatusCode() <= CoreConstants.SERVER_ERROR_END)) {
                    BaseLogger.info(ConfigMessages.API_RETRY_SCHEDULED_MESSAGE);
                    startConfigRetryTimer();
                }
                return;
            } catch (Exception e) {
                AppConfigException.logException(this.className, methodName, e);
                BaseLogger.info(ConfigMessages.API_RETRY_SCHEDULED_MESSAGE);
                startConfigRetryTimer();
                return;
            }

            // API request was successful
            if (response.getStatusCode() == CoreConstants.REQUEST_SUCCESS_200) {
                BaseLogger.debug(ConfigMessages.FETCH_API_SUCCESSFUL);
                try {
                    if (configRetry != null) {
                        configRetry.cancel();
                        configRetry = null;
                    }
                    HashMap<String, Object> map = new ObjectMapper().readValue((String) response.getResult(),
                            HashMap.class);
                    JSONObject obj = new JSONObject(map);
                    loadConfigurationsAndPopulateInMap(obj);
                    if (this.persistentCacheLocation != null) {
                        FileManager.createAndStoreFile(map, persistentCacheLocation);
                    }
                } catch (Exception e) {
                    AppConfigException.logException(this.className, methodName, e);
                }
            } else {
                // rare or impossible case
                BaseLogger.error("Failed to fetch configurations. " + response.getResult());
            }
        } else {
            BaseLogger.debug(ConfigMessages.CONFIG_HANDLER_INIT_ERROR);
        }
    }


    private void updatedConfiguration() {
        if (this.configurationUpdateListener != null) {
            this.configurationUpdateListener.onConfigurationUpdate();
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
                    BaseLogger.debug("Received opened connection from socket.");
                }

                @Override
                public void onMessage(String message) {
                    fetchFromApi();
                    updatedConfiguration();
                    BaseLogger.debug("Received message from socket. " + message);
                }

                @Override
                public void onClose(String closeMessage) {
                    BaseLogger.debug("Received close connection from socket. " + closeMessage);
                    onSocketRetry = true;
                    startSocketRetryTimer();
                }

                @Override
                public void onError(Exception e) {
                    BaseLogger.error("Received error from socket. " + e.toString());
                    onSocketRetry = true;
                    startSocketRetryTimer();
                }
            };
        }
        return socketHandler;
    }
}
