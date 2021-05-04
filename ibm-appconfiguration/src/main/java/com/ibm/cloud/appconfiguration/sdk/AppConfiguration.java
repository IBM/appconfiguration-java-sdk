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
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigMessages;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;

import java.util.HashMap;

public class AppConfiguration {

    private static AppConfiguration instance;
    public final static String REGION_US_SOUTH = "us-south";
    public final static String REGION_EU_GB = "eu-gb";
    public final static String REGION_AU_SYD = "au-syd";
    public static String overrideServerHost = null;

    private String apiKey = "";
    private String region = "";
    private String guid = "";
    private Boolean isInitialized = false;
    private Boolean isInitializedConfig = false;
    private ConfigurationHandler configurationHandlerInstance = null;

    public synchronized static AppConfiguration getInstance() {
        if (instance == null) {
            instance = new AppConfiguration();
        }
        return instance;
    }

    private AppConfiguration(){ }

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
        this.configurationHandlerInstance.init(this.apiKey, this.guid, this.region, overrideServerHost);
    }

    public void setContext(String collectionId, String environmentId) {
        this.setContext(collectionId, environmentId, null, true);
    }

    public void setContext(String collectionId, String environmentId, String configurationFile, Boolean liveConfigUpdateEnabled) {

        if (!this.isInitialized) {
            BaseLogger.error(ConfigMessages.COLLECTION_ID_ERROR);
            return;
        }
        if (!Validators.validateString(collectionId)) {
            BaseLogger.error(ConfigMessages.COLLECTION_ID_VALUE_ERROR);
            return;
        }

        if (!Validators.validateString(environmentId)) {
            BaseLogger.error(ConfigMessages.ENVIRONMENT_ID_VALUE_ERROR);
            return;
        }
        if (!liveConfigUpdateEnabled && !Validators.validateString(configurationFile)) {
            BaseLogger.error(ConfigMessages.CONFIG_FILE_NOT_FOUND_ERROR);
            return;
        }
        this.isInitializedConfig = true;

        this.configurationHandlerInstance.setContext(collectionId, environmentId, configurationFile, liveConfigUpdateEnabled);
        this.configurationHandlerInstance.loadData();
    }

    public void fetchConfigurations() {
        if (this.isInitializedConfig && this.isInitialized) {
            this.configurationHandlerInstance.loadData();
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
    }

    public void registerConfigurationUpdateListener(ConfigurationUpdateListener listener) {
        if (this.isInitializedConfig && this.isInitialized) {
            this.configurationHandlerInstance.registerConfigurationUpdateListener(listener);
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
    }

    public Feature getFeature(String featureId) {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getFeature(featureId);
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    public HashMap<String, Feature> getFeatures() {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getFeatures();
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    public HashMap<String, Property> getProperties() {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getProperties();
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    public Property getProperty(String propertyId) {
        if (this.isInitializedConfig && this.isInitialized) {
            return this.configurationHandlerInstance.getProperty(propertyId);
        } else {
            BaseLogger.error(ConfigMessages.COLLECTION_INIT_ERROR);
        }
        return null;
    }

    public void enableDebug(Boolean enable) {
        BaseLogger.setDebug(enable);
    }
}
