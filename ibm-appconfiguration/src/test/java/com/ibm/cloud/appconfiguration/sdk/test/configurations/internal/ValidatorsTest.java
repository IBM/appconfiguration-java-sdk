/**
 * Copyright 2022 IBM Corp. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Validators;

public class ValidatorsTest {
	
	@Test
	public void testValidateString() {
		String guid = null;
		boolean isValidString = true;
		isValidString = Validators.validateString(guid);
		assertFalse(isValidString);
		guid = "";
		isValidString = Validators.validateString(guid);
		assertFalse(isValidString);
		guid = "guid";
		isValidString = Validators.validateString(guid);
		assertTrue(isValidString);
	}
	
	@Test
	public void testIsValidRequest() {
		String collectionId = "collectionId";
		String environmentId = "environmentId";
		Boolean isInitialized = false;
		boolean isValidRequest= true;
		
		isValidRequest = Validators.isValidRequest(collectionId, environmentId, isInitialized);
		assertFalse(isValidRequest);
			
		isValidRequest = Validators.isValidRequest(null, environmentId, isInitialized);
		assertFalse(isValidRequest);
		
		isValidRequest = Validators.isValidRequest(collectionId, "", isInitialized);
		assertFalse(isValidRequest);
		
	}

}
