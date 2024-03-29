/**
 * Copyright 2021 IBM Corp. All Rights Reserved.
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

package com.ibm.cloud.appconfiguration.sdk.configurations.internal;

import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;

public final class Validators {

    private Validators() {
    }

    /**
     * Validate a string.
     *
     * @param value a string
     * @return {@code true} if given value is string. Else return {@code false}
     */
    public static Boolean validateString(String value) {
        return !(value == null || value.isEmpty() || value == "");
    }

    public static Boolean isValidRequest(String collectionId, String environmentId, Boolean isInitialized) {

        if (!isInitialized) {
            BaseLogger.error(ConfigMessages.COLLECTION_ID_ERROR);
            return false;
        }

        if (!validateString(collectionId)) {
            BaseLogger.error(ConfigMessages.COLLECTION_ID_VALUE_ERROR);
            return false;
        }

        if (!validateString(environmentId)) {
            BaseLogger.error(ConfigMessages.ENVIRONMENT_ID_VALUE_ERROR);
            return false;
        }
        return true;
    }
}
