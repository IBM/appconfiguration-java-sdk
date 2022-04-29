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

    private static final String wsUrl = "/wsfeature";
    private static final String path = "/feature/v1/instances/";
    private static final String service = "/apprapp";
    private static final String events = "/events/v1/instances/";
    private static final String config = "config";
    private static String overrideServiceUrl = null;
    private static String region = "";
    private static String httpBase = "";
    private static String webSocketBase = "";

    private URLBuilder() {
    }

    /**
     * @param collectionId collection id
     * @param environmentId environment id
     * @param region region name of App Configuration service instance
     * @param guid guid of App Configuration service instance
     * @param overrideServiceUrl service url. Use for testing purpose
     */
    public static void initWithContext(String collectionId, String environmentId, String region,
                                       String guid, String overrideServiceUrl) {

        if (Validators.validateString(collectionId) && Validators.validateString(environmentId)
                && Validators.validateString(region) && Validators.validateString(guid)) {

            URLBuilder.region = region;
            URLBuilder.overrideServiceUrl = overrideServiceUrl;

            webSocketBase = ConfigConstants.DEFAULT_WSS_TYPE;
            httpBase = ConfigConstants.DEFAULT_HTTP_TYPE;

            if (Validators.validateString(overrideServiceUrl)) {
                httpBase = overrideServiceUrl;
                webSocketBase += (overrideServiceUrl.replace("https://", "").replace("http://", ""));
            } else {
                httpBase += region;
                httpBase += ConfigConstants.DEFAULT_BASE_URL;
                webSocketBase += region;
                webSocketBase += ConfigConstants.DEFAULT_BASE_URL;
            }

            httpBase += String.format("%s%s%s/collections/%s/%s?environment_id=%s", service, path, guid,
                    collectionId, config, environmentId);
            webSocketBase += String.format("%s%s?instance_id=%s&collection_id=%s&environment_id=%s", service,
                    wsUrl, guid, collectionId, environmentId);
        }
    }

    /**
     * Return the Configuration URL.
     *
     * @return configuration url
     */
    public static String getConfigUrl() {
        return httpBase;
    }

    /**
     * Return the websocket URL.
     *
     * @return websocket url
     */
    public static String getWebSocketUrl() {
        return webSocketBase;
    }

    /**
     * Return the mettering URL.
     *
     * @param guid guid of App Configuration service instance
     * @return metering url
     */
    public static String getMeteringUrl(String guid) {
        String base = ConfigConstants.DEFAULT_HTTP_TYPE;
        if (Validators.validateString(overrideServiceUrl)) {
            base = overrideServiceUrl + service;
        } else {
            base += region + ConfigConstants.DEFAULT_BASE_URL + service;
        }
        return base + events + guid + "/usage";
    }
}
