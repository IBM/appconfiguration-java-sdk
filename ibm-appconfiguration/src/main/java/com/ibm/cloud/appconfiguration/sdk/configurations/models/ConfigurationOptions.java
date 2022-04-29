/**
 * Copyright 2022 IBM Corp. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.cloud.appconfiguration.sdk.configurations.models;

public class ConfigurationOptions {

    private String persistentCacheDirectory;

    private String bootstrapFile;

    private Boolean liveConfigUpdateEnabled;

    /**
     * Get the Persistent Cache Directory.
     *
     * @return the Persistent Cache Directory
     */
    public String getPersistentCacheDirectory() {
        return persistentCacheDirectory;
    }

    /**
     * Set the Persistent Cache Directory.
     *
     * @param persistentCacheDirectory
     */
    public void setPersistentCacheDirectory(String persistentCacheDirectory) {
        this.persistentCacheDirectory = persistentCacheDirectory;
    }

    /**
     * Get the value of LiveConfigUpdateEnabled.
     *
     * @return the LiveConfigUpdateEnabled
     */
    public Boolean getLiveConfigUpdateEnabled() {
        return liveConfigUpdateEnabled;
    }

    /**
     * Set the value of LiveConfigUpdateEnabled.
     *
     * @param liveConfigUpdateEnabled
     */
    public void setLiveConfigUpdateEnabled(Boolean liveConfigUpdateEnabled) {
        this.liveConfigUpdateEnabled = liveConfigUpdateEnabled;
    }

    /**
     * Get the Bootstrap File.
     *
     * @return the Bootstrap File.
     */
    public String getBootstrapFile() {
        return bootstrapFile;
    }

    /**
     * Set the Bootstrap file.
     *
     * @param bootstrapFile
     */
    public void setBootstrapFile(String bootstrapFile) {
        this.bootstrapFile = bootstrapFile;
    }

}
