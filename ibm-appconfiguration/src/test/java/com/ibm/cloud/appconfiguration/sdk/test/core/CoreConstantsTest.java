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

package com.ibm.cloud.appconfiguration.sdk.test.core;

import com.ibm.cloud.appconfiguration.sdk.core.CoreConstants;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CoreConstantsTest {
    @Test public void testCoreConstants() {
        assertEquals(CoreConstants.REQUEST_SUCCESS_200, new Integer(200));
        assertEquals(CoreConstants.REQUEST_SUCCESS_299, new Integer(299));
        assertEquals(CoreConstants.REQUEST_ERROR, new Integer(400));
        assertEquals(CoreConstants.REQUEST_ERROR_AUTH, new Integer(401));
        assertEquals(CoreConstants.REQUEST_ERROR_NOT_SUPPORTED, new Integer(405));
    }
}
