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

package com.ibm.cloud.appconfiguration.sdk.examples;

import com.ibm.cloud.appconfiguration.sdk.configurations.models.Property;
import com.ibm.cloud.appconfiguration.sdk.configurations.ConfigurationUpdateListener;
import com.ibm.cloud.appconfiguration.sdk.configurations.models.Feature;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import com.ibm.cloud.appconfiguration.sdk.AppConfiguration;

public class App {

    public static void main( String[] args ) throws Exception {
        setUp();
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                try (Socket client = serverSocket.accept()) {
                    handleClient(client);
                }
            }
        }
    }

    private static void handleClient(Socket client) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

        StringBuilder requestBuilder = new StringBuilder();
        String line;
        while (!(line = br.readLine()).isEmpty()) {
            requestBuilder.append(line + "\r\n");
        }

        String request = requestBuilder.toString();
        String[] requestsLines = request.split("\r\n");

        List<String> headers = new ArrayList<>();
        for (int h = 2; h < requestsLines.length; h++) {
            String header = requestsLines[h];
            headers.add(header);
        }

        AppConfiguration appConfiguration = AppConfiguration.getInstance();
        Feature feature = appConfiguration.getFeature("featureId");

        Property property = appConfiguration.getProperty("propertyId");

        JSONObject identityAttributes = new JSONObject();
        identityAttributes.put("city", "Bangalore");
        identityAttributes.put("country", "India");
        String value = (String) feature.getCurrentValue("pvrxe3", identityAttributes);
        Integer propertyValue = (Integer) property.getCurrentValue("pvrxe3", identityAttributes);


        byte[] notFoundContent = ("<h1>Hi Feature value is: " + value + ", Property value is: " + propertyvalue + "</h1> ").getBytes();
        sendResponse(client, "200 Not Found", "text/html", notFoundContent);

    }

    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 \r\n" + status).getBytes());
        clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }
    static void setUp() {
        AppConfiguration appConfiguration = AppConfiguration.getInstance();
        appConfiguration.enableDebug(true);
        appConfiguration.init(AppConfiguration.REGION_US_SOUTH, "AppGUID", "APIKEY");
        appConfiguration.setContext("collectionId", "environmentId");
        // appConfiguration.setContext("collectionId", "environmentId", "path_to_file", true);
        // appConfiguration.fetchConfigurations();
        appConfiguration.registerConfigurationUpdateListener(new ConfigurationUpdateListener() {
            @Override
            public void onConfigurationUpdate() {

                System.out.println("Got updates now");
            }
        });
    }
}
