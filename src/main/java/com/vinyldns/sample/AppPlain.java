package com.vinyldns.sample;

import com.amazonaws.auth.BasicAWSCredentials;
import io.vinyldns.java.VinylDNSClient;
import io.vinyldns.java.VinylDNSClientConfig;
import io.vinyldns.java.VinylDNSClientImpl;
import io.vinyldns.java.model.batch.*;
import io.vinyldns.java.model.membership.CreateGroupRequest;
import io.vinyldns.java.model.membership.DeleteGroupRequest;
import io.vinyldns.java.model.membership.Group;
import io.vinyldns.java.model.membership.MemberId;
import io.vinyldns.java.model.record.RecordType;
import io.vinyldns.java.model.record.data.AData;
import io.vinyldns.java.model.record.data.PTRData;
import io.vinyldns.java.model.record.data.RecordData;
import io.vinyldns.java.model.record.set.ListRecordSetsRequest;
import io.vinyldns.java.model.record.set.ListRecordSetsResponse;
import io.vinyldns.java.model.record.set.RecordSet;
import io.vinyldns.java.model.zone.GetZoneResponse;
import io.vinyldns.java.model.zone.Zone;
import io.vinyldns.java.model.zone.ZoneRequest;
import io.vinyldns.java.model.zone.ZoneResponse;
import io.vinyldns.java.responses.VinylDNSResponse;

import java.util.*;
import java.util.function.Supplier;

public class AppPlain {
    private final VinylDNSClient vinylDNSClient;
    private Group group;
    private Zone forwardZone;
    private Zone reverseZone;

    private AppPlain(VinylDNSClient vinylDNSClient) {
        this.vinylDNSClient = vinylDNSClient;
        setup();
    }

    public static void main(String[] args) {
        // Everything here assumes running docker locally, if you are hitting the dev environment, you
        // will need to use your own
        // accessKey, secretKey, and ownerGroupId and the url should be whatever VinylDNS instance you are hitting
        VinylDNSClientConfig config =
                new VinylDNSClientConfig("http://localhost:9000", new BasicAWSCredentials("testUserAccessKey", "testUserSecretKey"));
        VinylDNSClient vinylDNSClient = new VinylDNSClientImpl(config);

        // Fire up our application
        new AppPlain(vinylDNSClient).run();
    }

    /* THIS IS THE ONLY THING TO LOOK AT FOR MAKING BATCH CHANGES */
    private void run() {
        try {
            System.out.println("\r\n!!! STEP ONE, ADD SOME TEST RECORDS !!!");
            // Important!  This is how you would build a DNS request to submit changes
            // Here, we are doing multiple adds, we have 2 changes A+PTR per add
            AddChangeInput addInput1 = new AddChangeInput("test-java-1.ok.", RecordType.A, 300L, new AData("192.0.2.110"));
            AddChangeInput ptrInput1 = new AddChangeInput("192.0.2.110", RecordType.PTR, 7200L, new PTRData("test-java-1.ok."));

            AddChangeInput addInput2 = new AddChangeInput("test-java-2.ok.", RecordType.A, 300L, new AData("192.0.2.111"));
            AddChangeInput ptrInput2 = new AddChangeInput("192.0.2.111", RecordType.PTR, 7200L, new PTRData("test-java-2.ok."));

            List<ChangeInput> changes = new ArrayList<>();
            changes.add(addInput1);
            changes.add(ptrInput1);
            changes.add(addInput2);
            changes.add(ptrInput2);

            CreateBatchRequest request1 = new CreateBatchRequest(changes);
            request1.setOwnerGroupId(group.getId());
            VinylDNSResponse<BatchResponse> batchResponse1 = vinylDNSClient.createBatchChanges(request1);

            // Important!  Actually runs the request, submitting it to VinylDNS
            // At this point, it may not be complete, but as long as you don't get an error you are good!
            System.out.println("Batch change 1 submitted!");

            // NEVER DO THIS IN PRODUCTION, only useful for this test
            waitUntilBatchChangeComplete(batchResponse1.getValue());

            // NEVER DO THIS IN PRODUCTION, Just does a print out of records in the zone
            printRecordsInZone(forwardZone);

            // Let's replace our record sets changing their IP addresses using A+PTR
            System.out.println("\r\n!!! STEP TWO, REPLACE OUR TEST RECORDS WITH NEW IP ADDRESSES !!!");

            // Create another batch request, this time REPLACING one record with another
            // We need 4 changes for every replace!

            // First, remove the A and PTR for the two we just created
            DeleteRecordSetChangeInput deleteA1 = new DeleteRecordSetChangeInput("test-java-1.ok.", RecordType.A);
            DeleteRecordSetChangeInput deletePtr1 = new DeleteRecordSetChangeInput("192.0.2.110", RecordType.PTR);
            DeleteRecordSetChangeInput deleteA2 = new DeleteRecordSetChangeInput("test-java-2.ok.", RecordType.A);
            DeleteRecordSetChangeInput deletePtr2 = new DeleteRecordSetChangeInput("192.0.2.111", RecordType.PTR);

            // Next, we have to re-create the records, only thing changed is the IP address...
            AddChangeInput replaceA1 = new AddChangeInput("test-java-1.ok.", RecordType.A, 300L, new AData("192.0.2.115"));
            AddChangeInput replacePtr1 = new AddChangeInput("192.0.2.115", RecordType.PTR, 7200L, new PTRData("test-java-1.ok."));
            AddChangeInput replaceA2 = new AddChangeInput("test-java-2.ok.", RecordType.A, 300L, new AData("192.0.2.116"));
            AddChangeInput replacePtr2 = new AddChangeInput("192.0.2.116", RecordType.PTR, 7200L, new PTRData("test-java-2.ok."));

            List<ChangeInput> changes2 = new ArrayList<>();
            changes2.add(deleteA1);
            changes2.add(deletePtr1);
            changes2.add(deleteA2);
            changes2.add(deletePtr2);
            changes2.add(replaceA1);
            changes2.add(replacePtr1);
            changes2.add(replaceA2);
            changes2.add(replacePtr2);

            CreateBatchRequest request2 = new CreateBatchRequest(changes2);
            request1.setOwnerGroupId(group.getId());
            VinylDNSResponse<BatchResponse> batchResponse2 = vinylDNSClient.createBatchChanges(request2);
            System.out.println("Batch change 2 submitted!");

            // NEVER DO THIS IN PRODUCTION, only useful for this test
            waitUntilBatchChangeComplete(batchResponse2.getValue());
            printRecordsInZone(forwardZone);

            // Let's go ahead and clean up after ourselves, this demonstrates how to do a delete
            DeleteRecordSetChangeInput cleanA1 = new DeleteRecordSetChangeInput("test-java-1.ok.", RecordType.A);
            DeleteRecordSetChangeInput cleanPtr1 = new DeleteRecordSetChangeInput("192.0.2.115", RecordType.PTR);
            DeleteRecordSetChangeInput cleanA2 = new DeleteRecordSetChangeInput("test-java-2.ok.", RecordType.A);
            DeleteRecordSetChangeInput cleanPtr2 = new DeleteRecordSetChangeInput("192.0.2.116", RecordType.PTR);

            List<ChangeInput> changes3 = new ArrayList<>();
            changes3.add(cleanA1);
            changes3.add(cleanPtr1);
            changes3.add(cleanA2);
            changes3.add(cleanPtr2);

            CreateBatchRequest request3 = new CreateBatchRequest(changes3);
            request1.setOwnerGroupId(group.getId());
            VinylDNSResponse<BatchResponse> batchResponse3 = vinylDNSClient.createBatchChanges(request3);
            System.out.println("Batch change 3 submitted!");

            // NEVER DO THIS IN PRODUCTION, only useful for this test
            waitUntilBatchChangeComplete(batchResponse3.getValue());
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
                            vinylDNSClient.getBatchChanges(batch.getId());
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
        VinylDNSResponse<ZoneResponse> response = vinylDNSClient.createZone(z);

        if (response.getStatusCode() > 202) {
            throw new RuntimeException("Unable to connect to zone: " + response.getMessageBody());
        }

        // We have to wait until the zone exists as it is an async operation
        String zoneId = response.getValue().getZone().getId();
        waitUntilTrue(
                () -> {
                    VinylDNSResponse<GetZoneResponse> r =
                            vinylDNSClient.getZone(new ZoneRequest(zoneId));
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
                vinylDNSClient
                        .createGroup(new CreateGroupRequest(groupName, groupEmail, memberIds, adminIds));
        if (response.getStatusCode() > 202) {
            throw new RuntimeException("Unable to create group " + response.getMessageBody());
        }

        return response.getValue();
    }

    private void deleteGroup(String groupId) {
        VinylDNSResponse<Group> response =
                vinylDNSClient.deleteGroup(new DeleteGroupRequest(groupId));
        if (response.getStatusCode() > 202) {
            throw new RuntimeException("Unable to delete group " + response.getMessageBody());
        }
    }

    private Collection<RecordSet> listRecordSets(String zoneId, String recordNameFilter) {
        VinylDNSResponse<ListRecordSetsResponse> response =
                vinylDNSClient
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
                vinylDNSClient.deleteZone(new ZoneRequest(zoneId));
        if (response.getStatusCode() > 202 && response.getStatusCode() != 404) {
            throw new RuntimeException("Unable to abandon zone " + response.getMessageBody());
        }

        waitUntilTrue(
                () -> {
                    VinylDNSResponse<GetZoneResponse> r =
                            vinylDNSClient.getZone(new ZoneRequest(zoneId));
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
