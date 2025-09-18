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

import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class consisting of methods that perform all the file related operations.
 */
public class FileManager {

    private static final String FILE_NAME = "appconfiguration.json";

    private FileManager() {
    }

    public static Path sanitizePath(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        return Paths.get(input).normalize().toAbsolutePath();
    }

    public static Path joinPath(String directory, String fileName) {
        return sanitizePath(directory).resolve(fileName).normalize();
    }

    public static String readFile(String filePath) {
        Path path = sanitizePath(filePath);
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            BaseLogger.error("Error while reading file: " + path + " -> " + e);
            return "";
        }
    }

    public static void writeFile(String data, String directoryPath) {
        if (data == null) data = "";
        Path dir = sanitizePath(directoryPath);
        Path file = joinPath(directoryPath, FILE_NAME);
        try {
            Files.createDirectories(dir);
            Files.write(file, data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            BaseLogger.error("Error while writing to file: " + file + " -> " + e);
        }
    }
}
