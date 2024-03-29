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

package com.ibm.cloud.appconfiguration.sdk.configurations.models.internal;

import com.ibm.cloud.appconfiguration.sdk.core.AppConfigException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Defines the model of a rule defined for a segment in App Configuration service.
 */
public class Rule {

    public String attributeName;
    public String operator;
    public JSONArray values;
    private final String className = this.getClass().getName();

    /**
     * @param ruleJson JSON object containing rule information
     */
    public Rule(JSONObject ruleJson) {

        try {
            this.attributeName = ruleJson.getString("attribute_name");
            this.operator = ruleJson.getString("operator");
            this.values = ruleJson.getJSONArray("values");
        } catch (Exception e) {
            AppConfigException.logException(this.className, "Constructor", e, new Object[]{"Invalid action in Rule class."});

        }
    }

    /**
     * Evaluates the the Rule. Returns {@code true} if evaluation is passed against respective operator.
     * Else return {@code false}
     *
     * @param entityAttributes entity attributes JSON object
     * @return {@code true} if evaluation is passed. {@code false} otherwise
     */
    public Boolean evaluateRule(JSONObject entityAttributes) {

        Boolean result = false;
        Object key;
        String methodName = "evaluateRule";

        if (entityAttributes.has(this.attributeName)) {
            try {
                key = entityAttributes.get(this.attributeName);
            } catch (Exception e) {
                AppConfigException.logException(this.className, methodName, e);
                return result;
            }
        } else {
            return result;
        }

        for (int i = 0; i < this.values.length(); i++) {
            try {
                Object value = this.values.get(i);
                if (operatorCheck(key, value)) {
                    result = true;
                }

            } catch (Exception e) {
                AppConfigException.logException(this.className, methodName, e);
                return result;
            }
        }
        return result;
    }

    private Boolean operatorCheck(Object keyData, Object valueData) {
        Object key = keyData;
        Object value = valueData;
        Conversions keyVal;
        Conversions valueVal;

        Boolean result = false;

        if (key == null || value == null) {
            return result;
        }

        switch (this.operator) {
            case "endsWith":
                result = key.toString().endsWith(value.toString());
                break;
            case "startsWith":
                result = key.toString().startsWith(value.toString());
                break;
            case "contains":
                result = key.toString().contains(value.toString());
                break;
            case "is":
                if (key.getClass().equals(value.getClass())) {
                    result = key.equals(value);
                } else {
                    try {
                        result = value.toString().equals(key.toString());
                    } catch (Exception e) {
                        AppConfigException.logException(this.className, "is", e);
                    }
                }
                break;
            case "greaterThan":
                keyVal = this.numberConversion(key);
                valueVal = this.numberConversion(value);

                if (keyVal.isNumber && valueVal.isNumber) {
                    result = keyVal.value > valueVal.value;
                }
                break;
            case "lesserThan":
                keyVal = this.numberConversion(key);
                valueVal = this.numberConversion(value);

                if (keyVal.isNumber && valueVal.isNumber) {
                    result = keyVal.value < valueVal.value;
                }
                break;
            case "greaterThanEquals":
                keyVal = this.numberConversion(key);
                valueVal = this.numberConversion(value);

                if (keyVal.isNumber && valueVal.isNumber) {
                    result = keyVal.value >= valueVal.value;
                }
                break;
            case "lesserThanEquals":
                keyVal = this.numberConversion(key);
                valueVal = this.numberConversion(value);

                if (keyVal.isNumber && valueVal.isNumber) {
                    result = keyVal.value <= valueVal.value;
                }
                break;
        }
        return result;
    }

    private Conversions numberConversion(Object value) {
        try {
            if (value instanceof Number) {
                Float keyValue = ((Number) value).floatValue();
                return new Conversions(true, keyValue);
            } else if (value instanceof String) {
                Float keyValue = Float.valueOf(value.toString());
                return new Conversions(true, keyValue);
            }
        } catch (Exception e) {
            AppConfigException.logException(this.className, "numberConversion", e);
        }
        return new Conversions(false, Float.valueOf(0));

    }

    private class Conversions {
        public Boolean isNumber;
        public Float value;

        Conversions(Boolean isNumber, Float value) {
            this.isNumber = isNumber;
            this.value = value;
        }
    }
}
