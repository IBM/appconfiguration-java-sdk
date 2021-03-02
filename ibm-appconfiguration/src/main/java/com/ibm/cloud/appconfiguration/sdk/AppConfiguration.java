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

import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.feature.FeatureHandler;
import com.ibm.cloud.appconfiguration.sdk.feature.FeaturesUpdateListener;
import com.ibm.cloud.appconfiguration.sdk.feature.internal.FeatureMessages;
import com.ibm.cloud.appconfiguration.sdk.feature.internal.Validators;
import com.ibm.cloud.appconfiguration.sdk.feature.models.Feature;

import java.util.HashMap;

public class AppConfiguration {

    private static AppConfiguration instance;
    public final static String REGION_US_SOUTH = "us-south";
    public final static String REGION_EU_GB = "eu-gb";
    public static String overrideServerHost = null;

    private String apiKey = "";
    private String region = "";
    private String guid = "";
    private Boolean isInitialized = false;
    private Boolean isInitializedFeature = false;
    private FeatureHandler featureHandlerInstance = null;

    public synchronized static AppConfiguration getInstance() {
        if (instance == null) {
            instance = new AppConfiguration();
        }
        return instance;
    }

    private AppConfiguration(){ }

    public void init(String region, String guid, String apikey) {
        if (!Validators.validateString(region)) {
            BaseLogger.error(FeatureMessages.REGION_ERROR);
            return;
        }
        if (!Validators.validateString(apikey)) {
            BaseLogger.error(FeatureMessages.APIKEY_ERROR);
            return;
        }
        if (!Validators.validateString(guid)) {
            BaseLogger.error(FeatureMessages.GUID_ERROR);
            return;
        }

        this.apiKey = apikey;
        this.guid = guid;
        this.region = region;
        this.isInitialized = true;
        this.setupFeatureHandler();
    }

    private void setupFeatureHandler() {
        this.featureHandlerInstance = FeatureHandler.getInstance();
        this.featureHandlerInstance.init(this.apiKey, this.guid, this.region, overrideServerHost);
    }

    public void fetchFeaturesFromFile(String featureFile, Boolean liveFeatureUpdateEnabled) {

        if (!this.isInitializedFeature || !this.isInitialized) {
            BaseLogger.error(FeatureMessages.COLLECTION_ID_ERROR);
            return;
        }

        if (!liveFeatureUpdateEnabled && !Validators.validateString(featureFile)) {
            BaseLogger.error(FeatureMessages.FEATURE_FILE_NOT_FOUND_ERROR);
            return;
        }

        this.featureHandlerInstance.fetchFeaturesFromFile(liveFeatureUpdateEnabled, featureFile);
        new Thread(() -> {
            this.featureHandlerInstance.loadData();
        }).start();

    }

    public void setCollectionId(String collectionId) {
        if (!this.isInitialized) {
            BaseLogger.error(FeatureMessages.COLLECTION_ID_ERROR);
            return;
        }
        if (!Validators.validateString(collectionId)) {
            BaseLogger.error(FeatureMessages.COLLECTION_ID_VALUE_ERROR);
            return;
        }

        this.featureHandlerInstance.setCollectionId(collectionId);
        new Thread(() -> {
            this.featureHandlerInstance.loadData();
        }).start();
        this.isInitializedFeature = true;
    }

    public void fetchFeatureData() {
        if (this.isInitializedFeature && this.isInitialized) {
            new Thread(() -> {
                this.featureHandlerInstance.loadData();
            }).start();
        } else {
            BaseLogger.error(FeatureMessages.COLLECTION_SUB_ERROR);
        }
    }

    public void registerFeaturesUpdateListener(FeaturesUpdateListener listener) {
        if (this.isInitializedFeature && this.isInitialized) {
            this.featureHandlerInstance.registerFeaturesUpdateListener(listener);
        } else {
            BaseLogger.error(FeatureMessages.COLLECTION_SUB_ERROR);
        }
    }

    public Feature getFeature(String featureId) {
        if (this.isInitializedFeature && this.isInitialized) {
            return this.featureHandlerInstance.getFeature(featureId);
        } else {
            BaseLogger.error(FeatureMessages.COLLECTION_SUB_ERROR);
        }
        return null;
    }

    public HashMap<String, Feature> getFeatures() {
        if (this.isInitializedFeature && this.isInitialized) {
            return this.featureHandlerInstance.getFeatures();
        } else {
            BaseLogger.error(FeatureMessages.COLLECTION_SUB_ERROR);
        }
        return null;
    }

    public void enableDebug(Boolean enable) {
        BaseLogger.setDebug(enable);
    }
}
