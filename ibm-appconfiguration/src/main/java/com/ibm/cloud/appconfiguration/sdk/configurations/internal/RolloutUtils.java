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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.cloud.appconfiguration.sdk.core.BaseLogger;

/**
 * Utility class for handling progressive rollout configurations.
 */
public class RolloutUtils {

    private RolloutUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Represents a single phase in a progressive rollout.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RolloutPhase {
        @JsonProperty("percentage")
        private int percentage;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("duration_type")
        private String durationType;

        @JsonCreator
        public RolloutPhase(
            @JsonProperty(value = "percentage", required = true) int percentage
        ) {
            this.percentage = percentage;
        }

        @JsonProperty("duration")
        public void setDuration(int duration) {
            this.duration = duration;
        }

        @JsonProperty("duration_type")
        public void setDurationType(String durationType) {
            this.durationType = durationType;
        }

        public int getPercentage() {
            return percentage;
        }

        public Integer getDuration() {
            return duration;
        }

        public String getDurationType() {
            return durationType;
        }
    }

    /**
     * Represents the complete rollout configuration.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RolloutConfiguration {
        @JsonProperty("start_at")
        private String startAt;

        @JsonProperty("phases")
        private List<RolloutPhase> phases;

        @JsonCreator
        public RolloutConfiguration(
            @JsonProperty(value = "start_at", required = true) String startAt,
            @JsonProperty(value = "phases", required = true) List<RolloutPhase> phases
        ) {
            this.startAt = startAt;
            this.phases = phases;
        }

        public String getStartAt() {
            return startAt;
        }

        public List<RolloutPhase> getPhases() {
            return phases;
        }
    }

    /**
     * Parses progressive rollout phases into a TreeMap for efficient timestamp-to-percentage lookups.
     *
     * @param configuration the rollout configuration
     * @return TreeMap with timestamps as keys and percentages as values, or null if parsing fails
     */
    public static TreeMap<Long, Integer> parseRolloutConfigurationPhases(RolloutConfiguration configuration) {
        if (configuration == null || configuration.getStartAt() == null
                || configuration.getPhases() == null || configuration.getPhases().isEmpty()) {
            return null;
        }

        try {
            // Parse start timestamp
            Instant startTime = Instant.parse(configuration.getStartAt());
            long transitionTime = startTime.toEpochMilli();

            // Create TreeMap for efficient lookups
            TreeMap<Long, Integer> result = new TreeMap<>();
            result.put(0L, 0);

            // Duration multipliers in milliseconds
            Map<String, Long> multipliers = new HashMap<>();
            multipliers.put("days", 86400000L); // days
            multipliers.put("hours", 3600000L); // hours
            multipliers.put("minutes", 60000L); // minutes

            for (RolloutPhase phase : configuration.getPhases()) {
                // Add phase entry
                result.put(transitionTime, phase.getPercentage());

                // Calculate next transition time if duration is specified
                if (phase.getDuration() != null) {
                    transitionTime += phase.getDuration() * multipliers.get(phase.getDurationType());
                }
            }

            return result;
        } catch (DateTimeParseException e) {
            BaseLogger.error("Invalid start_at timestamp: " + configuration.getStartAt() + ", error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            BaseLogger.error("Error parsing rollout configuration: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the current rollout percentage based on current time.
     *
     * @param rolloutMap the TreeMap containing timestamp-to-percentage mappings
     * @return the current rollout percentage, or 0 if rolloutMap is null or empty
     */
    public static int getCurrentRolloutPercentage(TreeMap<Long, Integer> rolloutMap) {
        if (rolloutMap == null || rolloutMap.isEmpty()) {
            return 0;
        }

        long currentTime = Instant.now().toEpochMilli();

        // Find the entry with the largest timestamp that is <= currentTime
        Map.Entry<Long, Integer> floorEntry = rolloutMap.floorEntry(currentTime);
        if (floorEntry != null) {
            return floorEntry.getValue();
        }

        return 0;
    }
}
