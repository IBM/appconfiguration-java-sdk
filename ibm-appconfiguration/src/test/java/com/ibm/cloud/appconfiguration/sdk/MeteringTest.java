package com.ibm.cloud.appconfiguration.sdk;

import com.ibm.cloud.appconfiguration.sdk.configurations.internal.Metering;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

public class MeteringTest {

    @Test
    public void testMetering() {

        Metering metering = Metering.getInstance();

        metering.addMetering("guid1","collectionId1","environment_id1", "identityId1","segmentId1", "featureId1", null);
        metering.addMetering("guid1","collectionId2","environment_id1","identityId1","segmentId1", "featureId1", null);
        metering.addMetering("guid1","collectionId2","environment_id1","identityId1","segmentId1", "featureId1", null);

        metering.addMetering("guid1","collectionId1","environment_id2","identityId1","segmentId1", null, "property_id1");
        metering.addMetering("guid1","collectionId2","environment_id2", "identityId1","segmentId1", null, "property_id1");
        metering.addMetering("guid1","collectionId2","environment_id2", "identityId1","segmentId1", null, "property_id1");

        metering.addMetering("guid2","collectionId2","environment_id2", "identityId1","$$null$$", null, "property_id1");
        metering.addMetering("guid2","collectionId2","environment_id2", "identityId6","$$null$$", null, "property_id1");
        metering.addMetering("guid2","collectionId2","environment_id2", "identityId2","$$null$$", null, "property_id1");
        metering.addMetering("guid2","collectionId2","environment_id2", "identityId5","$$null$$", null, "property_id1");
        metering.addMetering("guid2","collectionId2","environment_id2", "identityId6","$$null$$", null, "property_id1");
        metering.addMetering("guid2","collectionId2","environment_id2", "identityId2","$$null$$", null, "property_id1");
        metering.addMetering("guid2","collectionId2","environment_id2", "identityId1","$$null$$", null, "property_id1");


        HashMap<String, JSONArray> result = metering.sendMetering();
        System.out.println(result);
        assertEquals(result.get("guid1").length(), 4);
    }
}
