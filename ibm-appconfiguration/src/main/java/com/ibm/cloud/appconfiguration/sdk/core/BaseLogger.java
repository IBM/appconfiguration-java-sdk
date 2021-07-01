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

package com.ibm.cloud.appconfiguration.sdk.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Class for handling the sdk logging.
 */
public class BaseLogger {

    private static boolean isDebug = false;
    private static final Logger LOGGER =
            Logger.getLogger(CoreMessages.GLOBAL_LOGGER_NAME);

    private BaseLogger() {

    }

    private static String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return formatter.format(date);
    }

    /**
     * Enable or disable the logging.
     *
     * @param value boolean value for the debug logger
     */
    public static void setDebug(boolean value) {
        isDebug = value;
    }

    /**
     * Method to check current status of debug logger.
     *
     * @return a boolean value
     */
    public static boolean isDebug() {
        return isDebug;
    }

    /**
     * Method to pass the info logger message.
     *
     * @param message message in string format
     */
    public static void info(String message) {
        LOGGER.info(getTime() + " " + message);
    }

    /**
     * Method to pass the info error message.
     *
     * @param message message in string format
     */
    public static void error(String message) {
        LOGGER.severe(getTime() + " " + message);
    }

    /**
     * Method to pass the warning logger message.
     *
     * @param message message in string format
     */
    public static void warning(String message) {
        if (isDebug) {
            LOGGER.warning(getTime() + " " + message);
        }
    }

    /**
     * Method to pass the success logger message.
     *
     * @param message message in string format
     */
    public static void success(String message) {
        if (isDebug) {
            LOGGER.info(getTime() + " " + message);
        }
    }

    /**
     * Method to pass the debug logger message.
     *
     * @param message message in string format
     */
    public static void debug(String message) {
        if (isDebug) {
            LOGGER.info(getTime() + " " + message);
        }
    }

    /**
     * method print logger message.
     * @param message - Message in string format.
     */
//    public static void printLogs(String message, Level level) {
//        LOGGER.log(level, message);
//    }
}
