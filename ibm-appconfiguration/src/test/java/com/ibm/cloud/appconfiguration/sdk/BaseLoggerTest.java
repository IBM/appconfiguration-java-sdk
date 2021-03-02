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

import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BaseLoggerTest {

    @Test public void testSetDebugMethod() {
        assertFalse(BaseLogger.isDebug(), "Logger isDebug should return 'false'");
        BaseLogger.setDebug(true);
        assertTrue(BaseLogger.isDebug(), "Logger isDebug should return 'true'");
        BaseLogger.setDebug(false);
        assertFalse(BaseLogger.isDebug(), "Logger isDebug should return 'false'");
    }
}
