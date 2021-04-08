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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.FileManager;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
            JSONObject dataa = FileManager.readFiles(getFile());
            assertNotNull(dataa);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @Test
    public void testFileWrite() {
        try {
            writeToLocalFile();

            JSONObject dataa = FileManager.readFiles(getFile());
            assertNotNull(dataa);

            HashMap<String,Object> result = null;
            try {
               result =
                        new ObjectMapper().readValue(dataa.toString(), HashMap.class);
            } catch (Exception e) {
                BaseLogger.debug(e.toString());
            }

            Boolean saved = FileManager.storeFile(result);
            assertTrue(saved);

            JSONObject data1 = FileManager.readFiles(null);
            assertNotNull(data1);
            assertEquals(dataa.toString(), data1.toString());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
