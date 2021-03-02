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

package com.ibm.cloud.appconfiguration.sdk.feature.internal;

public class FeatureMessages {

    public final static String NO_INTERNET_CONNECTION_ERROR = "No connection to internet. Please re-connect.";
    public final static String REGION_ERROR = "Provide a valid region in App Configuration init";
    public final static String GUID_ERROR = "Provide a valid guid in App Configuration init";
    public final static String APIKEY_ERROR = "Provide a valid apiKey in App Configuration init";
    public final static String COLLECTION_ID_VALUE_ERROR = "Provide a valid collectionId in App Configuration setCollectionId method";
    public final static String COLLECTION_ID_ERROR = "Invalid action in App Configuration. This action can be performed only after a successful initialization. Please check the initialization section for errors.";
    public final static String COLLECTION_SUB_ERROR = "Invalid action in App Configuration. This action can be performed only after a successful initialization and set collectionId value operation. Please check the initialization and setCollectionId sections for errors.";
    public final static String FEATURE_FILE_NOT_FOUND_ERROR = "featureFile parameter should be provided while liveFeatureUpdateEnabled is false during initialization";
    public final static String FEATURE_HANDLER_INIT_ERROR = "Invalid action in FeatureHandler. This action can be performed only after a successful initialization. Please check the initialization section for errors.";
    public final static String FEATURE_HANDLER_METHOD_ERROR = "Invalid action in FeatureHandler. Should be a method/function";
    public final static String FEATURE_API_ERROR = "Invalid configuration. Verify the collectionId, apikey, guid and region.";
    public final static String SINGLETON_EXCEPTION = "class must be initialized using the getInstance() method.";
    public final static String FEATURE_INVALID = "Invalid featureId - ";
}
