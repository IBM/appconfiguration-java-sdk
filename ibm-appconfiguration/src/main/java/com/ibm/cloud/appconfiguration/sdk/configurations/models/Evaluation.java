package com.ibm.cloud.appconfiguration.sdk.configurations.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.cloud.appconfiguration.sdk.configurations.internal.ConfigConstants;

import java.util.ArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Evaluation {

    @JsonProperty("collection_id")
    public String collectionId;

    @JsonProperty("environment_id")
    public String environmentId;

    public ArrayList<Usage> usages = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Usage {

        @JsonProperty("feature_id")
        public String featureId;

        @JsonProperty("property_id")
        public String propertyId;

        @JsonProperty("entity_id")
        public String entityId;

        @JsonProperty("segment_id")
        public String segmentId;

        @JsonProperty("evaluation_time")
        public String evaluationTime;

        public int count;
    }

    @JsonIgnore
    public void addUsage(String resourceType, String modifyKey, String entityId, String segmentId, String time, int count) {
        Usage usage = new Usage();
        if (resourceType.equals(ConfigConstants.FEATURE_ID)) {
            usage.featureId = modifyKey;
        } else {
            usage.propertyId = modifyKey;
        }
        usage.entityId = entityId;
        usage.segmentId = segmentId.isEmpty() ? null : segmentId;
        usage.evaluationTime = time;
        usage.count = count;
        usages.add(usage);
    }

    /**
     * This function helps in creating chunked versions of usages' record from parent class.
     * @param start the starting index of usage record in usages list
     * @param count the number of records to be present in usages of the returned object
     * @return returns a new Evaluation object containing less than or equal to `count` number of usage elements
     * of parent starting from index `start`.
     */
    @JsonIgnore
    public Evaluation generateUsageLimitedCopy(int start, int count) {
        Evaluation result = new Evaluation();
        result.collectionId = this.collectionId;
        result.environmentId = this.environmentId;
        result.usages = new ArrayList<>(this.usages.subList(
                Math.min(start, this.usages.size()),
                Math.min(start + count, this.usages.size())
        ));
        return result;
    }
}
