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
 * Class consisting of methods that constructs all the URL's required by the SDK.
 */
public class URLBuilder {

    private static final String https = "https://";
    private static final String wss = "wss://";
    private static final String basePath = ".apprapp.cloud.ibm.com";
    private static final String wsPath = "/wsfeature";
    private static final String service = "/apprapp";
    private static final String featurePath = "/feature/v1/instances/";
    private static final String eventsPath = "/events/v1/instances/";
    private static final String privateEndpointPrefix = "private.";
    private static final String config = "config";
    private static final String usage = "usage";
    private static String httpBase = "";
    private static String iamUrl = "";
    private static String configUrl = "";
    private static String meteringUrl = "";
    private static String webSocketUrl = "";

    private URLBuilder() {
    }

    /**
     * @param collectionId       collection id
     * @param environmentId      environment id
     * @param region             region name of App Configuration service instance
     * @param guid               guid of App Configuration service instance
     * @param overrideServiceUrl service url. Use for testing purpose
     * @param usePrivateEndpoint If true, use private endpoint to connect to App Configuration service instance.
     */
    public static void initWithContext(String collectionId, String environmentId, String region,
                                       String guid, String overrideServiceUrl, boolean usePrivateEndpoint) {

        if (Validators.validateString(collectionId) && Validators.validateString(environmentId)
                && Validators.validateString(region) && Validators.validateString(guid)) {

            // for dev & stage
            if (Validators.validateString(overrideServiceUrl)) {
                String[] temp = overrideServiceUrl.split("://");
                if (usePrivateEndpoint) {
                    httpBase = temp[0] + "://" + privateEndpointPrefix + temp[1];
                    iamUrl = "https://private.iam.test.cloud.ibm.com";
                    webSocketUrl = wss + privateEndpointPrefix + temp[1];
                } else {
                    httpBase = overrideServiceUrl;
                    iamUrl = "https://iam.test.cloud.ibm.com";
                    webSocketUrl = wss + temp[1];
                }
                // for prod
            } else {
                if (usePrivateEndpoint) {
                    httpBase = https + privateEndpointPrefix + region + basePath;
                    iamUrl = "https://private.iam.cloud.ibm.com";
                    webSocketUrl = wss + privateEndpointPrefix + region + basePath;
                } else {
                    httpBase = https + region + basePath;
                    iamUrl = "https://iam.cloud.ibm.com";
                    webSocketUrl = wss + region + basePath;
                }
            }

            configUrl = httpBase + String.format("%s%s%s/collections/%s/%s?environment_id=%s", service, featurePath, guid,
                    collectionId, config, environmentId);
            meteringUrl = httpBase + String.format("%s%s%s/%s", service, eventsPath, guid, usage);
            webSocketUrl = webSocketUrl + String.format("%s%s?instance_id=%s&collection_id=%s&environment_id=%s", service,
                    wsPath, guid, collectionId, environmentId);
        }
    }

    /**
     * Return the Base service URL.
     *
     * @return Base service url
     */
    public static String getBaseUrl() {
        return httpBase;
    }

    /**
     * Return the IAM URL.
     *
     * @return IAM url
     */
    public static String getIamUrl() {
        return iamUrl;
    }

    /**
     * Return the Config URL.
     *
     * @return config url
     */
    public static String getConfigUrl() {
        return configUrl;
    }

    /**
     * Return the websocket URL.
     *
     * @return websocket url
     */
    public static String getWebSocketUrl() {
        return webSocketUrl;
    }

    /**
     * Return the metering URL.
     *
     * @return metering url
     */
    public static String getMeteringUrl() {
        return meteringUrl;
    }
}
