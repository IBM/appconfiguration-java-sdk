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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class FileManager {

    private static final String fileName = "appconfiguration.json";

    private static String getCacheUrl() {
        Path source = Paths.get(FileManager.class.getResource("/").getPath());
        String path = source.toAbsolutePath() + "/" + fileName;
        return path;
    }

    public static Boolean storeFile(HashMap hashMapData) {
        if (hashMapData.isEmpty() || hashMapData == null) {
            return false;
        }
        try {
            String path = FileManager.getCacheUrl();
            File targetFile = new File(path);
            boolean success = targetFile.createNewFile();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get(path).toFile(), hashMapData);
        } catch (Exception e) {
            AppConfigException.logException("FileManager", "storeFile", e);
            return false;
        }
        return true;
    }
    public static JSONObject readFiles(String filePath) {

        String path = filePath;
        HashMap<?,?> data = new HashMap<>();
        if (filePath == null) {
            path = FileManager.getCacheUrl();
        }
        try {

            File targetFile = new File(path);
            boolean success = targetFile.createNewFile();
            ObjectMapper mapper = new ObjectMapper();
            data = mapper.readValue(Paths.get(path).toFile(), HashMap.class);
        } catch (Exception e) {
            AppConfigException.logException("FileManager", "readFiles", e);
        }
        return new JSONObject(data);
    }
}
