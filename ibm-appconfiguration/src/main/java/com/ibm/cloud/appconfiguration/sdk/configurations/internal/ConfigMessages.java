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

/**
 * Class consisting of various member variables used as logger messages by the SDK.
 */
public class ConfigMessages {

    private ConfigMessages() { }

    public static final String NO_INTERNET_CONNECTION_ERROR = "No connection to internet. Please re-connect.";
    public static final String REGION_ERROR = "Provide a valid region in App Configuration init";
    public static final String GUID_ERROR = "Provide a valid guid in App Configuration init";
    public static final String APIKEY_ERROR = "Provide a valid apiKey in App Configuration init";
    public static final String ENVIRONMENT_ID_VALUE_ERROR = "Provide a valid environmentId in App Configuration setContext method";
    public static final String COLLECTION_ID_VALUE_ERROR = "Provide a valid collectionId in App Configuration setContext method";
    public static final String COLLECTION_ID_ERROR = "Invalid action in App Configuration. This action can be performed only after a"
                                                      + " successful initialization. Please check the initialization section for errors.";
    public static final String COLLECTION_INIT_ERROR = "Invalid action in App Configuration. This action can be performed only after a successful initialization"
                                                       + " and setContext operation. Please check the initialization and setContext sections for errors.";
    public static final String CONFIG_FILE_NOT_FOUND_ERROR = "configurationFile parameter should be provided while liveConfigUpdateEnabled is false during initialization";
    public static final String CONFIG_HANDLER_INIT_ERROR = "Invalid action in ConfigurationHandler. This action can be performed only after a successful initialization."
                                                           + " Please check the initialization section for errors.";
    public static final String CONFIG_API_ERROR = "Invalid configuration. Verify the collectionId, environmentId, apikey, guid and region.";
    public static final String FEATURE_INVALID = "Invalid featureId - ";
    public static final String PROPERTY_INVALID = "Invalid propertyId - ";
}
