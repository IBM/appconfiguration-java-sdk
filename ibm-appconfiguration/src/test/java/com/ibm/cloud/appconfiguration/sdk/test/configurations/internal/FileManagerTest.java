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

package com.ibm.cloud.appconfiguration.sdk.test.configurations.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.FileManager;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FileManagerTest {

    private static String getFile() throws IOException {

        Path source = Paths.get(FileManager.class.getResource("/").getPath());
        System.out.println(source.toAbsolutePath());
        String path = source.toAbsolutePath() + "/user.json";
        File targetFile = new File(path);
        boolean success = targetFile.createNewFile();

        return path;
    }

    private static void writeToLocalFile()  {

        JSONObject jsonObj = new JSONObject(
                "{" +
                        "Name : Jai," +
                        "Age : 25, " +
                        "Salary: 25000.00 " +
                        "}"
        );
        try {
            Map<?,?> map = new ObjectMapper().readValue(jsonObj.toString(), HashMap.class);
            ObjectMapper mapper = new ObjectMapper();
            String path = FileManagerTest.getFile();
            mapper.writeValue(Paths.get(path).toFile(), map);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }
    @Test
    public void testFileRead() throws IOException {
        try {
            writeToLocalFile();
            JsonNode data = new ObjectMapper().readTree(FileManager.readFile(getFile()));
            assertNotNull(data);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Test
    public void testFileWrite() {
        try {
            writeToLocalFile();

            JsonNode dataa = new ObjectMapper().readTree(FileManager.readFile(getFile()));
            assertNotNull(dataa);

            HashMap<String,Object> result = null;
            try {
               result = new ObjectMapper().readValue(dataa.toString(), HashMap.class);
            } catch (Exception e) {
                BaseLogger.debug(e.toString());
            }

            assertNotNull(result);

            JsonNode data1 = new ObjectMapper().readTree(FileManager.readFile(getFile()));
            assertNotNull(data1);
            assertEquals(dataa.toString(), data1.toString());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Test
    public void testSanitizePath() {
        // Valid path
        String testPath = "src/./test/../test";
        Path sanitizedPath = FileManager.sanitizePath(testPath);
        assertNotNull(sanitizedPath, "Sanitized path should not be null.");
        assertTrue(sanitizedPath.toString().endsWith("test"), "Sanitized path should resolve correctly.");

        // Null path
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            FileManager.sanitizePath(null);
        });
        assertEquals("Path cannot be null or empty", exception.getMessage(), "Exception message should match expected text.");

        // Empty path
        exception = assertThrows(IllegalArgumentException.class, () -> {
            FileManager.sanitizePath("   ");
        });
        assertEquals("Path cannot be null or empty", exception.getMessage(), "Exception message should match expected text.");
    }

    @Test
    public void testJoinPath() {
        String directory = "src/test/resources";
        String fileName = "testFile.json";
        Path joinedPath = FileManager.joinPath(directory, fileName);
        assertNotNull(joinedPath, "Joined path should not be null.");
        assertTrue(joinedPath.toString().endsWith("src/test/resources/testFile.json"), "Joined path should resolve correctly.");
    }

    @Test
    public void testReadFileSuccessfully() throws IOException {
        String testData = "{\"key\":\"value\"}";
        String testDirectory = Paths.get(FileManager.class.getResource("/").getPath()).toAbsolutePath().toString();
        Path filePath = FileManager.sanitizePath(testDirectory).resolve("testFile.json");
        Files.write(filePath, testData.getBytes(StandardCharsets.UTF_8));

        String fileContent = FileManager.readFile(filePath.toString());
        assertEquals(testData, fileContent, "File content should match what was written.");
        Files.deleteIfExists(filePath);
    }

    @Test
    public void testReadFileWithNonexistentPath() {
        String nonexistentPath = "nonexistent/path/to/file.json";
        String fileContent = FileManager.readFile(nonexistentPath);
        assertEquals("", fileContent, "Reading a nonexistent file should return an empty string.");
    }

    @Test
    public void testWriteFileSuccessfully() throws IOException {
        String testDirectory = Paths.get(FileManager.class.getResource("/").getPath()).toAbsolutePath().toString();
        String testData = "{\"key\":\"value\"}";
        FileManager.writeFile(testData, testDirectory);

        Path writtenFilePath = FileManager.sanitizePath(testDirectory).resolve("appconfiguration.json");
        assertTrue(Files.exists(writtenFilePath), "File should be created by writeFile.");

        byte[] bytes = Files.readAllBytes(writtenFilePath);
        String fileContent = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(testData, fileContent, "File content should match the written data.");
        Files.deleteIfExists(writtenFilePath);
    }
}
