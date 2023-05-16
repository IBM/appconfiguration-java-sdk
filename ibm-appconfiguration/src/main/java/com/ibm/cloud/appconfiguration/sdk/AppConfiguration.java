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

package com.ibm.cloud.appconfiguration.sdk;

import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationHandler;
import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationUpdateListener;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigMessages;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.ConfigurationOptions;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;

import java.util.HashMap;

/**
 * IBM Cloud App Configuration is a centralized feature management and configuration service on IBM
 * Cloud for use with web and mobile applications, microservices, and distributed environments.
 * Instrument your applications with App Configuration Java SDK, and use the App Configuration dashboard,
 * CLI or API to define feature flags or properties, organized into collections and targeted to segments.
 * Toggle feature flag states in the cloud to activate or deactivate features in your application or
 * environment, when required. You can also manage the properties for distributed applications centrally.
 *
 * @version 0.3.3
 * @see <a href="https://cloud.ibm.com/docs/app-configuration">App Configuration</a>
 */
public class AppConfiguration {

    private static AppConfiguration instance;
    public static final String REGION_US_SOUTH = "us-south";
    public static final String REGION_EU_GB = "eu-gb";
    public static final String REGION_AU_SYD = "au-syd";
    public static final String REGION_US_EAST = "us-east";
    private static String overrideServiceUrl = null;
    private String apiKey = "";
    private String region = "";
    private String guid = "";
    private Boolean isInitialized = false;
    private Boolean isInitializedConfig = false;
    private ConfigurationHandler configurationHandlerInstance = null;
    private String persistentCacheLocation = null;
    private String bootstrapFile = null;
    private Boolean liveConfigUpdateEnabled = true;
    private boolean usePrivateEndpoint = false;

    /**
     * Returns an instance of the {@link AppConfiguration} class. If the same {@link AppConfiguration} instance
     * is available in the cache, then that instance is returned.
     * Otherwise, a new {@link AppConfiguration} instance is created and cached.
     *
     * @return instance of {@link AppConfiguration}
     */
    public static synchronized AppConfiguration getInstance() {
        if (instance == null) {
            instance = new AppConfiguration();
        }
        return instance;
    }

    private AppConfiguration() {
    }

    /**
     * Override the default App Configuration URL. This method should be invoked before the SDK initialization.
     * <pre>
     *     // Example
     *     AppConfiguration.overrideServiceUrl("https://testurl.com");
     * </pre>
     * NOTE: To be used for development purposes only.
     *
     * @param url The base url
     */
    public static void overrideServiceUrl(String url) {
        if (url != null && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        overrideServiceUrl = url;
    }

    /**
     * Initialize the sdk to connect with your App Configuration service instance.
     *
     * @param region region name of App Configuration service instance
     * @param guid guid/instanceId of App Configuration service instance
     * @param apikey apikey of App Configuration service instance
     */
    public void init(String region, String guid, String apikey) {
        if (!Validators.validateString(region)) {
            BaseLogger.error(ConfigMessages.REGION_ERROR);
            return;
        }
        if (!Validators.validateString(apikey)) {
            BaseLogger.error(ConfigMessages.APIKEY_ERROR);
            return;
        }
        if (!Validators.validateString(guid)) {
            BaseLogger.error(ConfigMessages.GUID_ERROR);
            return;
        }

        this.apiKey = apikey;
        this.guid = guid;
        this.region = region;
        this.isInitialized = true;
        this.setupConfigureHandler();
    }

    private void setupConfigureHandler() {
        this.configurationHandlerInstance = ConfigurationHandler.getInstance();
        this.configurationHandlerInstance.init(this.apiKey, this.guid, this.region, overrideServiceUrl, this.usePrivateEndpoint);
    }

    /**
     * Sets the context of the SDK.
     *
     * @param collectionId Id of the collection created in App Configuration service instance
     * @param environmentId Id of the environment created in App Configuration service instance
     */
    public void setContext(String collectionId, String environmentId) {
        this.setContext(collectionId, environmentId, null);
    }

    /**
     * Sets the context of the SDK.
     *
     * @param collectionId Id of the collection created in App Configuration service instance
     * @param environmentId Id of the environment created in App Configuration service instance
     * @param configOption ConfigurationOptions object that contains the configuration parameters.
     *                     There are three parameters that can be set.
     *                     configOption.persistentCacheDirectory : The SDK will create a file - 'appconfiguration.json'
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
            persistentCacheLocation = configOption.getPersistentCacheDirectory();
            bootstrapFile = configOption.getBootstrapFile();
            liveConfigUpdateEnabled = configOption.getLiveConfigUpdateEnabled();
        }

        if (liveConfigUpdateEnabled == null) {
            liveConfigUpdateEnabled = true;
            configOption.setLiveConfigUpdateEnabled(liveConfigUpdateEnabled);
        }

        if (persistentCacheLocation != null) {
            persistentCacheLocation = persistentCacheLocation.endsWith("/")
                    ? persistentCacheLocation + ConfigConstants.PERSISTENTCACHE_FILE
                    : persistentCacheLocation + "/" + ConfigConstants.PERSISTENTCACHE_FILE;
            configOption.setPersistentCacheDirectory(persistentCacheLocation);
        }

        //It will return if any one of the input is not valid
        if (!Validators.isValidRequest(collectionId, environmentId, isInitialized)) {
            return;
        }

        //It will return if liveConfigUpdateEnabled is false and bootstrapFile is not passed
        if (!liveConfigUpdateEnabled && !Validators.validateString(bootstrapFile)) {
            BaseLogger.error(ConfigMessages.BOOTSTRAP_FILE_NOT_FOUND_ERROR);
            return;
        }

        this.isInitializedConfig = true;
        this.configurationHandlerInstance.setContext(collectionId, environmentId, configOption);
    }

    /**
     * Sets the context of the SDK.
     *
     * @param collectionId            Id of the collection created in App Configuration service instance
     * @param environmentId           Id of the environment created in App Configuration service instance
     * @param configurationFile       local configuration file path. This parameter when passed along
     *                                with {@code liveConfigUpdateEnabled} will drive the SDK to use the local
     *                                configuration file to perform feature and property evaluations.
     * @param liveConfigUpdateEnabled live configurations update from the server. Set this value to {@code false}
     *                                if the new configuration values shouldn't be fetched from the server.
     *
     * @deprecated Use setContext(String, String, ConfigurationOptions) instead.
     */
    @Deprecated
    public void setContext(String collectionId, String environmentId,
                           String configurationFile, Boolean liveConfigUpdateEnabled) {

        //It will return if any one of the input is not valid
        if (!Validators.isValidRequest(collectionId, environmentId, isInitialized)) {
            return;
        }

        if (liveConfigUpdateEnabled == null) {
            liveConfigUpdateEnabled = true;
        }

        if (!liveConfigUpdateEnabled && !Validators.validateString(configurationFile)) {
            BaseLogger.error(ConfigMessages.CONFIG_FILE_NOT_FOUND_ERROR);
            return;
        }
        this.isInitializedConfig = true;
        ConfigurationOptions configOption = new ConfigurationOptions();
        configOption.setLiveConfigUpdateEnabled(liveConfigUpdateEnabled);
        configOption.setBootstrapFile(configurationFile);
        this.configurationHandlerInstance.setContext(collectionId, environmentId, configOption);
    }

    /**
     * Set the SDK to connect to App Configuration service by using a private endpoint that is
     * accessible only through the IBM Cloud private network.
     * <p>
     * This function must be called before calling the `init` function on the SDK.
     *
     * @param usePrivateEndpointParam Set to true if the SDK should connect to App Configuration using private endpoint.
     *                                Be default, it is set to false.
     */
    public void usePrivateEndpoint(boolean usePrivateEndpointParam) {
        this.usePrivateEndpoint = usePrivateEndpointParam;
    }

    /**
     * Fetch latest configurations data.
     */
    public void fetchConfigurations() {
        if (this.isInitializedConfig && this.isInitialized) {
            this.configurationHandlerInstance.loadData();
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
    }

    /**
     * Live listener for changes/updates to configurations.
     *
     * @param listener listener
     */
    public void registerConfigurationUpdateListener(ConfigurationUpdateListener listener) {
        if (this.isInitializedConfig && this.isInitialized) {
            this.configurationHandlerInstance.registerConfigurationUpdateListener(listener);
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
    }

    /**
     * Returns the {@link Feature} object with the details of the feature specified by the {@code featureId}.
     *
     * @param featureId the Feature Id
     * @return feature object
     */
    public Feature getFeature(String featureId) {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getFeature(featureId);
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    /**
     * Returns all features.
     *
     * @return hashmap of all features and their corresponding {@link Feature} objects
     */
    public HashMap<String, Feature> getFeatures() {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getFeatures();
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    /**
     * Returns all properties.
     *
     * @return hashmap of all properties and their corresponding {@link Property} objects
     */
    public HashMap<String, Property> getProperties() {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getProperties();
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    /**
     * Returns the {@link Property} object with the details of the property specified by the {@code propertyId}.
     *
     * @param propertyId the Property Id
     * @return property object
     */
    public Property getProperty(String propertyId) {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getProperty(propertyId);
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    /**
     * Method to enable or disable the logger. By default, logger is disabled.
     *
     * @param enable boolean value {@code true} or {@code false}
     */
    public void enableDebug(Boolean enable) {
        BaseLogger.setDebug(enable);
    }
}
