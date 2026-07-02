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

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ResultExtractConfigurations;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.RolloutUtils;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.CacheConfig;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.RuleElem;
import com.ibm.cloud.appconfiguration.sdk.core.ServiceImpl;
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
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Utils;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.Segment;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.internal.SegmentRules;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.ConfigurationOptions;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import com.ibm.cloud.sdk.core.http.HttpHeaders;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;

import org.apache.commons.codec.digest.MurmurHash3;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;

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
    private HashMap<String, Feature> featureMap = new HashMap<>();
    private HashMap<String, Property> propertyMap = new HashMap<>();
    private HashMap<String, Segment> segmentMap = new HashMap<>();
    private HashMap<String, TreeMap<Long, Integer>> rolloutConfigMap = new HashMap<>();
    private Boolean liveConfigUpdateEnabled = true;
    private String bootstrapFile = null;
    private Boolean onSocketRetry = false;
    private String overrideServiceUrl = null;
    private String persistentCacheLocation = null;
    private boolean usePrivateEndpoint = false;

    private RetryHandler configRetry;
    private RetryHandler socketRetry;
    private final int configRetryInterval = 120000; // 2 minutes
    private final int socketRetryInterval = 15000; // 15 seconds

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
     * @param apikey             apikey of App Configuration service instance
     * @param guid               guid/instanceId of App Configuration service instance
     * @param region             region name of App Configuration service instance
     * @param overrideServiceUrl service url. Use for testing purpose
     * @param usePrivateEndpoint If true, use private endpoint to connect to App Configuration service instance.
     */
    public void init(String apikey, String guid, String region, String overrideServiceUrl, boolean usePrivateEndpoint) {
        this.apikey = apikey;
        this.guid = guid;
        this.region = region;
        this.overrideServiceUrl = overrideServiceUrl;
        this.usePrivateEndpoint = usePrivateEndpoint;

        this.featureMap = new HashMap<>();
        this.propertyMap = new HashMap<>();
        this.segmentMap = new HashMap<>();
    }

    /**
     * @param collectionId  collection id
     * @param environmentId environment id
     * @param configOption  configOption object contains three fields
     *                      configOption.persistentCacheDir : The SDK will create a file - 'appconfiguration.json'
     *                      in the specified directory and it will be used as the persistent cache to store the
     *                      App Configuration service information.
     *                      configOption.bootstrapFile : Absolute path of configuration file. This parameter
     *                      when passed along with `liveConfigUpdateEnabled` value will drive the SDK to use the
     *                      configurations present in this file to perform feature and property evaluations
     *                      configOption.liveConfigUpdateEnabled : live configurations update from the server.
     *                      Set this value to `false` if the new configuration values shouldn't be fetched from the server.
     */
    public void setContext(String collectionId, String environmentId, ConfigurationOptions configOption) {
        if (configOption != null) {
            this.liveConfigUpdateEnabled = configOption.getLiveConfigUpdateEnabled();
            this.bootstrapFile = configOption.getBootstrapFile();
            this.persistentCacheLocation = configOption.getPersistentCacheDirectory();
        }
        this.liveConfigUpdateEnabled = this.liveConfigUpdateEnabled != null ? this.liveConfigUpdateEnabled : true;
        this.collectionId = collectionId;
        this.environmentId = environmentId;
        URLBuilder.initWithContext(collectionId, environmentId, region, guid, overrideServiceUrl, usePrivateEndpoint);
        Metering.getInstance().init(URLBuilder.getMeteringUrl(), collectionId, environmentId, apikey);
        this.isInitialized = true;

        loadData();
    }

    public synchronized void loadData() {
        String methodName = "loadData";
        boolean persistentCacheRead = false;

        if (Validators.validateString(persistentCacheLocation)) {
            Path path = FileManager.joinPath(persistentCacheLocation, ConfigConstants.PERSISTENCE_CACHE_FILE);
            BaseLogger.info(ConfigMessages.READ_PERSISTENCE_CACHE + path);
            String persistentData = FileManager.readFile(path.toString());
            if (!persistentData.isEmpty()) {
                ResultExtractConfigurations persistenceConfig = Utils.extractConfigurations(persistentData, this.environmentId, this.collectionId);
                if (persistenceConfig.isSuccess()) {
                    loadConfigurationsInCache(persistenceConfig.getConfig());
                    persistentCacheRead = true;
                } else {
                    BaseLogger.error(ConfigMessages.ERROR_READ_PERSISTENCE_CACHE + persistenceConfig.getErrorMessage());
                }
            }
        }
        if (Validators.validateString(bootstrapFile)) {
            Path path = FileManager.sanitizePath(bootstrapFile);
            if (Validators.validateString(persistentCacheLocation)) {
                if (!persistentCacheRead) {
                    BaseLogger.info(ConfigMessages.READ_BOOTSTRAP_CONFIGURATIONS + path);
                    String bootstrapFileData = FileManager.readFile(path.toString());
                    ResultExtractConfigurations bootstrapConfig = Utils.extractConfigurations(bootstrapFileData, this.environmentId, this.collectionId);
                    if (bootstrapConfig.isSuccess()) {
                        loadConfigurationsInCache(bootstrapConfig.getConfig());
                        FileManager.writeFile(Utils.formatConfig(bootstrapConfig.getConfig(), this.environmentId, this.collectionId), persistentCacheLocation);
                    } else {
                        BaseLogger.error(ConfigMessages.ERROR_READ_BOOTSTRAP_CONFIGURATIONS + bootstrapConfig.getErrorMessage());
                    }
                }
            } else {
                BaseLogger.info(ConfigMessages.READ_BOOTSTRAP_CONFIGURATIONS + path);
                String bootstrapFileData = FileManager.readFile(path.toString());
                ResultExtractConfigurations bootstrapConfig = Utils.extractConfigurations(bootstrapFileData, this.environmentId, this.collectionId);
                if (bootstrapConfig.isSuccess()) {
                    loadConfigurationsInCache(bootstrapConfig.getConfig());
                } else {
                    BaseLogger.error(ConfigMessages.ERROR_READ_BOOTSTRAP_CONFIGURATIONS + bootstrapConfig.getErrorMessage());
                }
            }
        }
        if (this.liveConfigUpdateEnabled) {
            // start a daemon Timer thread that periodically checks the internet connectivity
            connectivity = Connectivity.getInstance();
            connectivity.addConnectivityListener(this::connectionHandler);
            this.fetchConfigData();
        } else if (this.socket != null) {
            this.socket.cancel();
            this.socket = null;
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
            headers.put(HttpHeaders.USER_AGENT, ServiceImpl.getArtifactId() + "/" + ServiceImpl.getVersion());

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

    public void loadConfigurationsInCache(CacheConfig config) {
        if (config != null) {
            // Load features
            if (config.getFeatures() != null) {
                this.featureMap = new HashMap<>();
                this.rolloutConfigMap = new HashMap<>();

                for (Feature feature : config.getFeatures()) {
                    if (feature.getFeatureId() != null) {
                        this.featureMap.put(feature.getFeatureId(), feature);

                        // Parse feature-level progressive rollout
                        if (feature.getRolloutConfiguration() != null) {
                            TreeMap<Long, Integer> rolloutMap = RolloutUtils.parseRolloutConfigurationPhases(feature.getRolloutConfiguration());
                            if (rolloutMap != null) {
                                this.rolloutConfigMap.put(feature.getFeatureId(), rolloutMap);
                            }
                        }

                        // Parse segment-level progressive rollout
                        if (feature.getSegmentRules() != null && !feature.getSegmentRules().isEmpty()) {
                            for (SegmentRules segmentRule : feature.getSegmentRules()) {
                                if (segmentRule.getRolloutConfiguration() != null) {
                                    TreeMap<Long, Integer> rolloutMap = RolloutUtils.parseRolloutConfigurationPhases(segmentRule.getRolloutConfiguration());
                                    if (rolloutMap != null) {
                                        String key = feature.getFeatureId() + ConfigConstants.DELIMITER + segmentRule.getRuleId();
                                        this.rolloutConfigMap.put(key, rolloutMap);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Load properties
            if (config.getProperties() != null) {
                this.propertyMap = new HashMap<>();
                for (Property property : config.getProperties()) {
                    if (property.getPropertyId() != null) {
                        this.propertyMap.put(property.getPropertyId(), property);
                    }
                }
            }
            // Load segments
            if (config.getSegments() != null) {
                this.segmentMap = new HashMap<>();
                for (Segment segment : config.getSegments()) {
                    if (segment.getSegmentId() != null) {
                        this.segmentMap.put(segment.getSegmentId(), segment);
                    }
                }
            }
        }
    }

    /**
     * Records each of feature and property evaluations done by sending it to {@link Metering}.
     *
     * @param featureId  feature id
     * @param propertyId property id
     * @param entityId   entity id
     * @param segmentId  segment id
     */
    public void recordValuation(String featureId, String propertyId, String entityId, String segmentId) {
        Metering.getInstance().addMetering(entityId, segmentId, featureId, propertyId);
    }

    /**
     * Property evaluation.
     *
     * @param property         property object
     * @param entityId         entity id
     * @param entityAttributes entity attributes JSON object
     * @return property evaluated value
     */
    public Object propertyEvaluation(Property property, String entityId, JSONObject entityAttributes) {
        JSONObject resultDict = new JSONObject();
        resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, ConfigConstants.EMPTY_STRING);
        resultDict.put(ConfigConstants.VALUE, new Object());

        try {
            List<SegmentRules> segmentRules = property.getSegmentRules();
            if (segmentRules != null && !segmentRules.isEmpty() && entityAttributes != null && !entityAttributes.isEmpty()) {
                Map<Integer, SegmentRules> rulesMap = this.parseRules(segmentRules);
                resultDict = evaluateRules(rulesMap, entityAttributes, null, property, null);
                return resultDict.get(ConfigConstants.VALUE);
            }
            return property.getValue();
        } finally {
            String propertyId = property.getPropertyId();
            this.recordValuation(null, propertyId, entityId,
                    resultDict.getString(ConfigConstants.EVALUATED_SEGMENT_ID));
        }
    }

    /**
     * Feature evaluation.
     *
     * @param feature          feature object
     * @param entityId         entity id
     * @param entityAttributes entity attributes JSON object
     * @return feature evaluated value in map
     */
    public HashMap<String, Object> featureEvaluation(Feature feature, String entityId, JSONObject entityAttributes) {

        JSONObject resultDict = new JSONObject();
        resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, ConfigConstants.EMPTY_STRING);
        resultDict.put(ConfigConstants.VALUE, new Object());
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            if (feature.isEnabled()) {
                List<SegmentRules> segmentRules = feature.getSegmentRules();
                if (segmentRules != null && !segmentRules.isEmpty() && entityAttributes != null && !entityAttributes.isEmpty()) {
                    Map<Integer, SegmentRules> rulesMap = this.parseRules(segmentRules);
                    resultDict = evaluateRules(rulesMap, entityAttributes, feature, null, entityId);
                    map.put(ConfigConstants.CURRENT_VALUE, resultDict.get(ConfigConstants.VALUE));
                    map.put(ConfigConstants.IS_ENABLED, resultDict.get(ConfigConstants.FEATURE_ENABLED));
                    return map;
                }
                // Check feature-level rollout
                int rolloutPercentage;
                if (feature.getRolloutConfiguration() != null) {
                    TreeMap<Long, Integer> rolloutMap = this.rolloutConfigMap.get(feature.getFeatureId());
                    if (rolloutMap != null) {
                        entityId += feature.getRolloutConfiguration().getStartAt();
                        rolloutPercentage = RolloutUtils.getCurrentRolloutPercentage(rolloutMap);
                    } else {
                        rolloutPercentage = 0;
                    }
                } else {
                    rolloutPercentage = feature.getRolloutPercentage();
                }

                // Murmurhash will be calculated if rollout_percentage is not 100 or 0
                if (rolloutPercentage == ConfigConstants.DEFAULT_ROLLOUT_PERCENTAGE
                        || (rolloutPercentage != ConfigConstants.LOWEST_POSSIBLE_ROLLOUT_PERCENTAGE
                        && calculateMurmurHash(entityId, feature.getFeatureId()) < rolloutPercentage)) {
                    map.put(ConfigConstants.CURRENT_VALUE, feature.getEnabledValue());
                    map.put(ConfigConstants.IS_ENABLED, true);
                    return map;
                }

                map.put(ConfigConstants.CURRENT_VALUE, feature.getDisabledValue());
                map.put(ConfigConstants.IS_ENABLED, false);
                return map;
            } else {
                map.put(ConfigConstants.CURRENT_VALUE, feature.getDisabledValue());
                map.put(ConfigConstants.IS_ENABLED, false);
                return map;
            }
        } finally {
            String featureId = feature.getFeatureId();
            this.recordValuation(featureId, null, entityId,
                    resultDict.getString(ConfigConstants.EVALUATED_SEGMENT_ID));
        }
    }

    private JSONObject evaluateRules(Map<Integer, SegmentRules> rulesMap, JSONObject entityAttributes,
                                     Feature feature, Property property, String entityId) {

        JSONObject resultDict = new JSONObject();
        resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, ConfigConstants.EMPTY_STRING);
        resultDict.put(ConfigConstants.VALUE, new Object());
        try {
            for (int i = 1; i <= rulesMap.size(); i++) {

                SegmentRules segmentRule = rulesMap.get(i);
                if (segmentRule != null) {

                    for (int level = 0; level < segmentRule.getRules().size(); level++) {
                        RuleElem rule = segmentRule.getRules().get(level);
                        List<String> segments = rule.getSegments();
                        for (int innerLevel = 0; innerLevel < segments.size(); innerLevel++) {
                            String segmentKey = segments.get(innerLevel);
                            if (this.evaluateSegment(segmentKey, entityAttributes)) {
                                resultDict.put(ConfigConstants.EVALUATED_SEGMENT_ID, segmentKey);
                                if (feature != null) {
                                    int rolloutPercentage;

                                    if (segmentRule.getRolloutConfiguration() != null
                                        || Objects.equals(segmentRule.getRolloutType(), ConfigConstants.PROGRESSIVE)) {
                                        TreeMap<Long, Integer> rolloutMap;
                                        if (segmentRule.getRolloutPercentage().equals(ConfigConstants.DEFAULT)) {
                                            // Use feature-level rollout configuration
                                            rolloutMap = this.rolloutConfigMap.get(feature.getFeatureId());
                                        } else {
                                            // Use segment-level rollout configuration
                                            String key = feature.getFeatureId() + ConfigConstants.DELIMITER + segmentRule.getRuleId();
                                            rolloutMap = this.rolloutConfigMap.get(key);
                                        }
                                        if (rolloutMap != null) {
                                            entityId += segmentRule.getRolloutConfiguration().getStartAt();
                                            rolloutPercentage = RolloutUtils.getCurrentRolloutPercentage(rolloutMap);
                                        } else {
                                            rolloutPercentage = 0;
                                        }
                                    } else {
                                        // Use manual rollout percentage
                                        rolloutPercentage = getRolloutPercentage(segmentRule.getRolloutPercentage(), feature.getRolloutPercentage());
                                    }

                                    if (rolloutPercentage == ConfigConstants.DEFAULT_ROLLOUT_PERCENTAGE
                                            || (rolloutPercentage != ConfigConstants.LOWEST_POSSIBLE_ROLLOUT_PERCENTAGE
                                            && calculateMurmurHash(entityId, feature.getFeatureId()) < rolloutPercentage)) {
                                        if (segmentRule.getValue().equals(ConfigConstants.DEFAULT)) {
                                            resultDict.put(ConfigConstants.VALUE, feature.getEnabledValue());
                                        } else {
                                            resultDict.put(ConfigConstants.VALUE, segmentRule.getValue());
                                        }
                                        resultDict.put(ConfigConstants.FEATURE_ENABLED, true);
                                    } else {
                                        resultDict.put(ConfigConstants.VALUE, feature.getDisabledValue());
                                        resultDict.put(ConfigConstants.FEATURE_ENABLED, false);
                                    }
                                } else {
                                    // property
                                    if (segmentRule.getValue().equals(ConfigConstants.DEFAULT)) {
                                        // inherited of value property.value
                                        resultDict.put(ConfigConstants.VALUE, property.getValue());
                                    } else {
                                        // segment level overridden value
                                        resultDict.put(ConfigConstants.VALUE, segmentRule.getValue());
                                    }
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
            // Check feature-level rollout
            int rolloutPercentage;
            if (feature.getRolloutConfiguration() != null) {
                TreeMap<Long, Integer> rolloutMap = this.rolloutConfigMap.get(feature.getFeatureId());
                if (rolloutMap != null) {
                    rolloutPercentage = RolloutUtils.getCurrentRolloutPercentage(rolloutMap);
                } else {
                    rolloutPercentage = 0;
                }
            } else {
                rolloutPercentage = feature.getRolloutPercentage();
            }

            if (rolloutPercentage == ConfigConstants.DEFAULT_ROLLOUT_PERCENTAGE
                    || (rolloutPercentage != ConfigConstants.LOWEST_POSSIBLE_ROLLOUT_PERCENTAGE
                    && calculateMurmurHash(entityId, feature.getFeatureId()) < rolloutPercentage)) {
                resultDict.put(ConfigConstants.VALUE, feature.getEnabledValue());
                resultDict.put(ConfigConstants.FEATURE_ENABLED, true);
            } else {
                resultDict.put(ConfigConstants.VALUE, feature.getDisabledValue());
                resultDict.put(ConfigConstants.FEATURE_ENABLED, false);
            }
        } else {
            resultDict.put(ConfigConstants.VALUE, property.getValue());
        }
        return resultDict;
    }

    private int getRolloutPercentage(Object segmentRolloutPer, int featureRolloutPer) {
        if (segmentRolloutPer.equals(ConfigConstants.DEFAULT)) {
            return featureRolloutPer;
        } else {
            return (int) segmentRolloutPer;
        }
    }

    private Boolean evaluateSegment(String segmentKey, JSONObject entityAttributes) {

        if (this.segmentMap.containsKey(segmentKey)) {
            Segment segment = this.segmentMap.get(segmentKey);
            return segment.evaluateRule(entityAttributes);
        }
        return false;

    }

    private Map<Integer, SegmentRules> parseRules(List<SegmentRules> segmentRulesList) {
        Map<Integer, SegmentRules> ruleMap = new HashMap<>();
        if (segmentRulesList != null) {
            for (SegmentRules segmentRule : segmentRulesList) {
                try {
                    ruleMap.put(segmentRule.getOrder(), segmentRule);
                } catch (Exception e) {
                    AppConfigException.logException(this.className, "parseRules", e);
                }
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
            Hence, we don't need to write the retry logic again.
            The API call gets retried within getConfig() for 3 times in an exponential interval(1s, 2s, 4s) between each retry.
            If all the 3 retries fails, appropriate exceptions are raised.
            For 429 error code - The getConfig() will retry the request 3 times in an interval of time mentioned in ["retry-after"] header.
            If all the 3 retries exhausts the call is returned and appropriate exceptions are raised.

            When all the above retries fails, we schedule our own Timer to retry after 2 minutes for the response status_codes [429 & 5xx].
            All other status codes are not retried nor a retry is scheduled.
            User has to take immediate action and resolve it themselves by looking at the error logs.
         */
        if (this.isInitialized) {
            String url = URLBuilder.getConfigUrl();
            Response response;
            try {
                response = ServiceImpl.getInstance(apikey).getConfig(url);
            } catch (ServiceResponseException e) {
                BaseLogger.error("Exception occurred while fetching configurations. Status code:" + e.getStatusCode() + " message: " + e.getMessage());
                if (e.getStatusCode() == CoreConstants.TOO_MANY_REQUESTS || (e.getStatusCode() >= CoreConstants.SERVER_ERROR_BEGIN && e.getStatusCode() <= CoreConstants.SERVER_ERROR_END)) {
                    BaseLogger.info(String.format(ConfigMessages.API_RETRY_SCHEDULED_MESSAGE, this.configRetryInterval / 1000));
                    startConfigRetryTimer();
                }
                return;
            } catch (Exception e) {
                AppConfigException.logException(this.className, methodName, e);
                BaseLogger.info(String.format(ConfigMessages.API_RETRY_SCHEDULED_MESSAGE, this.configRetryInterval / 1000));
                startConfigRetryTimer();
                return;
            }

            // API request was successful
            if (response.getStatusCode() == CoreConstants.REQUEST_SUCCESS_200) {
                BaseLogger.debug(ConfigMessages.FETCH_API_SUCCESSFUL);
                if (configRetry != null) {
                    configRetry.cancel();
                    configRetry = null;
                }
                ResultExtractConfigurations serverConfigurations =
                        Utils.extractConfigurations((String) response.getResult(), this.environmentId, this.collectionId);
                if (serverConfigurations.isSuccess()) {
                    loadConfigurationsInCache(serverConfigurations.getConfig());
                    if (Validators.validateString(this.persistentCacheLocation)) {
                        FileManager.writeFile(Utils.formatConfig(serverConfigurations.getConfig(), this.environmentId, this.collectionId), this.persistentCacheLocation);
                    }
                } else {
                    BaseLogger.error(ConfigMessages.ERROR_READ_FETCHED_CONFIGURATIONS + serverConfigurations.getErrorMessage());
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
        }, this.configRetryInterval);
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
        }, this.socketRetryInterval);
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
                    BaseLogger.debug("Received close connection from socket. " + closeMessage
                        + ". Reconnecting in " + socketRetryInterval / 1000 + " seconds.");
                    onSocketRetry = true;
                    startSocketRetryTimer();
                }

                @Override
                public void onError(Exception e) {
                    BaseLogger.debug("Received error from socket. " + e.toString()
                        + ". Reconnecting in " + socketRetryInterval / 1000 + " seconds.");
                    onSocketRetry = true;
                    startSocketRetryTimer();
                }
            };
        }
        return socketHandler;
    }

    public int calculateMurmurHash(String entityId, String featureId) {
        String data = entityId + ":" + featureId;
        long hashResult = MurmurHash3.hash32x86(data.getBytes(), ConfigConstants.OFFSET, data.getBytes().length, ConfigConstants.SEED);

        //The decimal equivalent of 0x00000000ffffffff = 2^32 (4294967295).
        if (hashResult < 0) {
            hashResult = hashResult & 0x00000000ffffffffL;
        }
        return (int) ((hashResult / ConfigConstants.MAX_VAL) * 100);
    }

    public boolean isConnected() {
        if (socket != null) {
            return socket.isConnected();
        }
        return false;
    }
}
