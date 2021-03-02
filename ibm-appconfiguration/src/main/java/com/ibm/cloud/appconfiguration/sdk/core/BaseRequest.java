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

package com.ibm.cloud.appconfiguration.sdk.core;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Network request class
 */
public class BaseRequest {

    // Header values
    private static final String JSON_CONTENT_TYPE = "application/json";

    // TLS
    private static final String TLS_VERSION = "TLSv1.2";
    private static final String UTF_ENCODE = "UTF-8";

    private CloseableHttpClient httpClient;
    private RequestTypes requestType;
    private String url;
    private HttpRequestBase request;

    public BaseRequest(Builder builder) {

        try {

            this.httpClient = this.getHttpClient();
            this.requestType = builder.method;
            this.url = builder.url;
            if (isValidRequest()) {

                if (builder.method == RequestTypes.GET) {
                    request = new HttpGet(this.url);
                } else {
                    HttpPost postReq = new HttpPost(url);
                    postReq.addHeader(HTTP.CONTENT_TYPE, JSON_CONTENT_TYPE);
                    StringEntity body = new StringEntity(builder.bodyJson.toString(), UTF_ENCODE);
                    postReq.setEntity(body);
                    request = postReq;
                }
                builder.headers.forEach((k,v) -> request.addHeader(k,v));
            }

        } catch (Exception e) {
            AppConfigException.logException(this.getClass().getName(), "Builder()", e);
        }
    }

    public void execute(AppConfigurationResponseListener listener) {

        if(isValidRequest()) {

            CloseableHttpResponse response = null;

            try {
                if(httpClient != null) {
                    response = httpClient.execute(this.request);
                    sendResponseToListener(response, listener);

                } else {
                    BaseLogger.debug(CoreMessages.REQUEST_CLIENT_INVALID);
                }

            } catch (Exception e) {
                AppConfigException.logException(this.getClass().getName(), "execute", e);
                listener.onFailure(CoreConstants.REQUEST_ERROR, e.toString());
            }

        } else {
            BaseLogger.debug(CoreMessages.REQUEST_TYPE_NOT_SUPPORTED);
            listener.onFailure(CoreConstants.REQUEST_ERROR_NOT_SUPPORTED, CoreMessages.REQUEST_TYPE_NOT_SUPPORTED);
            return;
        }
    }
    protected static void sendResponseToListener(CloseableHttpResponse response,
                                                 AppConfigurationResponseListener listener) throws IOException {
        String responseBody = null;

        if (response.getEntity() != null) {
            ByteArrayOutputStream outputAsByteArray = new ByteArrayOutputStream();
            response.getEntity().writeTo(outputAsByteArray);

            responseBody = new String(outputAsByteArray.toByteArray());
        }

        Integer statusCode = null;

        if (response.getStatusLine() != null) {
            statusCode = response.getStatusLine().getStatusCode();
        }

        if (statusCode != null && statusCode >= CoreConstants.REQUEST_SUCCESS_200 && statusCode <= CoreConstants.REQUEST_SUCCESS_299) {
            listener.onSuccess(statusCode, responseBody);
        } else {
            if(statusCode != null && statusCode == CoreConstants.REQUEST_ERROR_AUTH) {
                BaseLogger.error(CoreMessages.REQUEST_RESPONSE_ERROR);
            }
            listener.onFailure(statusCode, responseBody);
        }
    }

    private boolean isValidRequest() {
        return (this.requestType == RequestTypes.GET || this.requestType == RequestTypes.POST);
    }


    private static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = null;
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance(TLS_VERSION);
            sslContext.init(null, null, null);
            httpClient = HttpClients.custom().setSSLContext(sslContext).build();
        } catch (Exception e) {
            AppConfigException.logException("BaseRequest", "getHttpClient", e);
        }
        return httpClient;

    }

    /**
     * Builder for {@link BaseRequest}
     */
    public static class Builder {
        private RequestTypes method;
        private String url;
        private Map<String, String> headers;
        private JSONObject bodyJson;

        public final Builder method(RequestTypes method) {
            this.method = method;
            return this;
        }

        public final Builder url(String url) {
            this.url = url;
            return this;
        }

        public final Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public final Builder body(JSONObject body) {
            this.bodyJson = body;
            return this;
        }

        public BaseRequest build() {
            return new BaseRequest(this);
        }
    }
}