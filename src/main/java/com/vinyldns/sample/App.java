package com.vinyldns.sample;

import com.vinyldns.sample.helper.APtrRecordItem;
import com.vinyldns.sample.helper.BatchRequestBuilder;
import com.vinyldns.sample.helper.RecordItem;
import com.vinyldns.sample.helper.VinylDNSHelper;
import io.vinyldns.java.model.batch.BatchChangeStatus;
import io.vinyldns.java.model.batch.BatchResponse;
import io.vinyldns.java.model.batch.CreateBatchRequest;
import io.vinyldns.java.model.membership.CreateGroupRequest;
import io.vinyldns.java.model.membership.DeleteGroupRequest;
import io.vinyldns.java.model.membership.Group;
import io.vinyldns.java.model.membership.MemberId;
import io.vinyldns.java.model.record.data.RecordData;
import io.vinyldns.java.model.record.set.ListRecordSetsRequest;
import io.vinyldns.java.model.record.set.ListRecordSetsResponse;
import io.vinyldns.java.model.record.set.RecordSet;
import io.vinyldns.java.model.zone.GetZoneResponse;
import io.vinyldns.java.model.zone.Zone;
import io.vinyldns.java.model.zone.ZoneRequest;
import io.vinyldns.java.model.zone.ZoneResponse;
import io.vinyldns.java.responses.VinylDNSResponse;

import java.net.Inet4Address;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class App {
    private final VinylDNSHelper vinylHelper;
    private Group group;
    private Zone forwardZone;
    private Zone reverseZone;

    private App(VinylDNSHelper vinylDNSHelper) {
        this.vinylHelper = vinylDNSHelper;
        setup();
    }

    public static void main(String[] args) {
        // Everything here assumes running docker locally, if you are hitting the dev environment, you
        // will need to use your own
        // accessKey, secretKey, and ownerGroupId and the url should be
        // https://dev-api.vinyldns.comcast.net:9443
        VinylDNSHelper vinylHelper =
                new VinylDNSHelper("testUserAccessKey", "testUserSecretKey", "http://localhost:9000");

        // Fire up our application
        new App(vinylHelper).run();
    }

    /* THIS IS THE ONLY THING TO LOOK AT FOR MAKING BATCH CHANGES */
    private void run() {
        try {
            System.out.println("\r\n!!! STEP ONE, ADD SOME TEST RECORDS !!!");
            // Important!  This is how you would build a DNS request to submit changes
            // Here, we are doing multiple adds
            RecordItem aPtr1 =
                    new APtrRecordItem("test-java-1.ok.", Inet4Address.getByName("192.0.2.110"));
            RecordItem aPtr2 =
                    new APtrRecordItem("test-java-2.ok.", Inet4Address.getByName("192.0.2.111"));

            // Build a request, this helper makes that simple
            CreateBatchRequest request1 =
                    new BatchRequestBuilder(group.getId()).withAddMany(Arrays.asList(aPtr1, aPtr2)).build();

            // Important!  Actually runs the request, submitting it to VinylDNS
            // At this point, it may not be complete, but as long as you don't get an error you are good!
            BatchResponse batchResponse1 = vinylHelper.submitBatchRequest(request1);
            System.out.println("Batch change 1 submitted!");

            // NEVER DO THIS IN PRODUCTION, only useful for this test
            waitUntilBatchChangeComplete(batchResponse1);

            // NEVER DO THIS IN PRODUCTION, Just does a print out of records in the zone
            printRecordsInZone(forwardZone);

            // Let's replace our record sets changing their IP addresses using A+PTR
            System.out.println("\r\n!!! STEP TWO, REPLACE OUR TEST RECORDS WITH NEW IP ADDRESSES !!!");

            // Create another batch request, this time REPLACING one record with another
            RecordItem aPtrReplace1 =
                    new APtrRecordItem("test-java-1.ok.", Inet4Address.getByName("192.0.2.115"));
            RecordItem aPtrReplace2 =
                    new APtrRecordItem("test-java-2.ok.", Inet4Address.getByName("192.0.2.116"));
            CreateBatchRequest request2 =
                    new BatchRequestBuilder(group.getId())
                            .withReplaceOne(aPtr1, aPtrReplace1)
                            .withReplaceOne(aPtr2, aPtrReplace2)
                            .build();
            BatchResponse batchResponse2 = vinylHelper.submitBatchRequest(request2);
            System.out.println("Batch change 2 submitted!");

            // NEVER DO THIS IN PRODUCTION, only useful for this test
            waitUntilBatchChangeComplete(batchResponse2);
            printRecordsInZone(forwardZone);

            // Let's go ahead and clean up after ourselves, this demonstrates how to do a delete
            System.out.println("\r\n!!! STEP THREE, DELETE OUR TEST RECORDS !!!");
            CreateBatchRequest request3 =
                    new BatchRequestBuilder(group.getId())
                            .withDeleteMany(Arrays.asList(aPtrReplace1, aPtrReplace2))
                            .build();
            BatchResponse batchResponse3 = vinylHelper.submitBatchRequest(request3);
            System.out.println("Batch change 3 submitted!");

            // NEVER DO THIS IN PRODUCTION, only useful for this test
            waitUntilBatchChangeComplete(batchResponse3);
        } catch (Throwable ex) {
            System.out.println("\r\n!!! ENCOUNTERED ERROR !!!");
            ex.printStackTrace();
        } finally {
            tearDown();
        }
    }


    /* Note: This better be called before anything else */
    private void setup() {
        group = createGroup("ok", "test@test.com", "ok");

        System.out.println("Connecting to zone...");

        // Important! You should never do this !!
        // We setup a forward zone (A, AAAA) and a reverse zone (PTR) so we can do different batch
        // changes
        forwardZone = connectZone("ok.", "test@test.com", group.getId());

        System.out.println("Connecting to reverse zone...");

        // We have to have the reverse space loaded or this will not work
        reverseZone = connectZone("2.0.192.in-addr.arpa.", "test@test.com", group.getId());
    }

    private void tearDown() {
        System.out.println("Cleaning up...");
        abandonZone(forwardZone.getId());
        abandonZone(reverseZone.getId());
        deleteGroup(group.getId());
    }

    /* **************************************************************************
     * EVERYTHING BELOW HERE YOU SHOULD IGNORE, JUST USED FOR TEST PURPOSES
     ***************************************************************************/
    private void printRecordsInZone(Zone zone) {
        Collection<RecordSet> newRecordSets = listRecordSets(zone.getId(), "test-java-");

        for (RecordSet rs : newRecordSets) {
            System.out.println("Record name='" + rs.getName() + "'; data=");
            rs.getRecords().forEach((RecordData r) -> System.out.println(r));
        }
    }

    private void waitUntilBatchChangeComplete(BatchResponse batch) {
        waitUntilTrue(
                () -> {
                    VinylDNSResponse<BatchResponse> r =
                            vinylHelper.getVinylDNSClient().getBatchChanges(batch.getId());
                    return r.getStatusCode() != 404
                            && r.getValue() != null
                            && r.getValue().getStatus() == BatchChangeStatus.Complete;
                });
    }

    private Zone connectZone(String zoneName, String email, String adminGroupId) {
        // Connect to the zone name provided using all default settings
        Zone z = new Zone();
        z.setName(zoneName);
        z.setEmail(email);
        z.setAdminGroupId(adminGroupId);
        VinylDNSResponse<ZoneResponse> response = vinylHelper.getVinylDNSClient().createZone(z);

        if (response.getStatusCode() > 202) {
            throw new RuntimeException("Unable to connect to zone: " + response.getMessageBody());
        }

        // We have to wait until the zone exists as it is an async operation
        String zoneId = response.getValue().getZone().getId();
        waitUntilTrue(
                () -> {
                    VinylDNSResponse<GetZoneResponse> r =
                            vinylHelper.getVinylDNSClient().getZone(new ZoneRequest(zoneId));
                    return r.getStatusCode() == 200;
                });

        return response.getValue().getZone();
    }

    private Group createGroup(String groupName, String groupEmail, String userGuid) {
        Set<MemberId> memberIds = new HashSet<>();
        memberIds.add(new MemberId(userGuid));

        Set<MemberId> adminIds = new HashSet<>();
        adminIds.add(new MemberId(userGuid));
        VinylDNSResponse<Group> response =
                vinylHelper
                        .getVinylDNSClient()
                        .createGroup(new CreateGroupRequest(groupName, groupEmail, memberIds, adminIds));
        if (response.getStatusCode() > 202) {
            throw new RuntimeException("Unable to create group " + response.getMessageBody());
        }

        return response.getValue();
    }

    private void deleteGroup(String groupId) {
        VinylDNSResponse<Group> response =
                vinylHelper.getVinylDNSClient().deleteGroup(new DeleteGroupRequest(groupId));
        if (response.getStatusCode() > 202) {
            throw new RuntimeException("Unable to delete group " + response.getMessageBody());
        }
    }

    private Collection<RecordSet> listRecordSets(String zoneId, String recordNameFilter) {
        VinylDNSResponse<ListRecordSetsResponse> response =
                vinylHelper
                        .getVinylDNSClient()
                        .listRecordSets(new ListRecordSetsRequest(zoneId, recordNameFilter));
        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Unexpected error listing record sets " + response.getMessageBody());
        } else {
            return response.getValue().getRecordSets();
        }
    }

    private void abandonZone(String zoneId) {
        VinylDNSResponse<ZoneResponse> response =
                vinylHelper.getVinylDNSClient().deleteZone(new ZoneRequest(zoneId));
        if (response.getStatusCode() > 202 && response.getStatusCode() != 404) {
            throw new RuntimeException("Unable to abandon zone " + response.getMessageBody());
        }

        waitUntilTrue(
                () -> {
                    VinylDNSResponse<GetZoneResponse> r =
                            vinylHelper.getVinylDNSClient().getZone(new ZoneRequest(zoneId));
                    return r.getStatusCode() == 404;
                });
    }

    private void waitUntilTrue(Supplier<Boolean> f) {
        int retries = 20;
        while (!f.get()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
            retries -= 1;
            if (retries <= 0) {
                throw new RuntimeException("Timed out waiting for condition to be true");
            }
        }
    }
}
