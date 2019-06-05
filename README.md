# VinylDNS Sample Java App

This is a sample java application that demonstrates how to make Batch Changes against VinylDNS.

## Getting Started

Before you can run the application, you must have a running local instance of VinylDNS.

### Starting up a local VinylDNS instance
To start up a local instance of VinylDNS on your machine with docker:

1. Ensure that you have [docker](https://docs.docker.com/install/) and [docker-compose](https://docs.docker.com/compose/install/)
1. Clone the repo: `git clone https://github.com/vinyldns/vinyldns.git`
1. Navigate to repo: `cd vinyldns`
1. Run `.bin/docker-up-vinyldns.sh`. This will start up the api at http://localhost:9000 and the portal at http://localhost:9001
1. See [Developer Guide](DEVELOPER_GUIDE.md#loading-test-data) for how to load a test DNS zone
1. To stop the local setup, run `./bin/remove-vinyl-containers.sh`.

### Run the example app

From the root directory, execute the script `./run-sample.sh`

This is just a wrapper around the command `mvn compile exec:java -Dexec.mainClass=com.vinyldns.sample.App -Dexec.cleanupDaemonThreads=false`

## Description

The example application is designed to setup some VinylDNS entities (groups and zones).  It then proceeds
to run through a few batch changes.

1. Issue a Batch Change that will create a few A+PTR records
1. Issue a Batch Change that will _REPLACE_ the A+PTR records that were just created
1. Issue a Batch Change that will _DELETE_ the A+PTR records that were created.

## An overview of the source code.

There is an `AppPlain` class that shows how to directly use the low level vinyldns-java client for
submitting batch changes.  You can compare it directly to the `App` class.

The sample application can all be found in the `App` class.  It handles setup and teardown of the test
artifacts so that the Batch Changes can run.  The `App` class uses the `VinylDNSHelper` to help simplify
creating batch requests.

The `VinylDNSHelper` provides a small (and perhaps unnecessary) wrapper around the `VinylDNSClient` that comes
from the [VinylDNS-Java Library](https://github.com/vinyldns/vinyldns-java).  All it does is look for an
error in the response and throws an Exception if an error was found.

It is important that batch changes _CAN FAIL_ on submit if certain validations fail, so it is important to
understand that your batch change failed.

If the batch change is successfully submitted, you will get a 202 and the batch change will be queued for processing.

### Connecting to VinylDNS

To connect to VinylDNS, you need your credentials (available via the user portal).  Once you have your credentials, 
you can setup the client.

```java
// Assumes these are in your environment, but you can get them anyway you want...
String accessKey = System.getenv("VINYLDNS_ACCESS_KEY"); // from your credentials
String secretKey = System.getenv("VINYLDNS_SECRET_KEY"); // from your credentials
String vinylDNSUrl = System.getenv("VINYLDNS_URL"); // the full url to the vinyldns instance you are hitting

if (accessKey == null || secretKey == null || vinylDNSUrl == null) {
    throw new RuntimeException("Unable to load vinyldns, environment variables not found");
}

// Actually connects to Vinyl using vinyldns-java
VinylDNSClientConfig config =
        new VinylDNSClientConfig(vinylDNSUrl, new BasicAWSCredentials(accessKey, secretKey));
this.vinylDNSClient = new VinylDNSClientImpl(config);
```

### vinyldns-java BatchChange API

When working with Batch Changes, you have to keep in mind when keeping A+PTR records together...

1. _CREATING_ an A+PTR record requires _TWO_ changes, an ADD for the A, and an ADD for the PTR
1. _CHANGING THE IP_ for an A+PTR requires _FOUR_ changes: DELETE A, DELETE PTR, ADD A, ADD PTR
1. _DELETING_ an A+PTR requires _TWO_ changes, a DELETE A and DELETE PTR

As an example, to CREATE an A+PTR record the following code would build the request...

```java
List<ChangeInput> changes = new ArrayList<>();

// The forward A record
AData adata = new AData("1.2.3.4");
AddChangeInput addInput = new AddChangeInput("www.example.com", RecordType.A, 300L, adata);

// The reverse PTR record, note how it is the mirror inverse of the A record
PTRData ptrdata = new PTRData("www.example.com")
AddChangeInput ptrInput = new AddChangeInput("1.2.3.4", RecordType.PTR, 7200L, ptrdata));
changes.add(addInput);
changes.add(ptrInput);

CreateBatchRequest batchRequest = new CreateBatchRequest(changes);
batchRequest.setOwnerGroupId(ownerGroupId);

VinylDNSResponse<BatchResponse> response = vinylDNSClient.createBatchChanges(request);

```

### BatchRequestBuilder

This app has a `BatchRequestBuilder` that simplifies the process of making Batch Change requests.  This is totally optional.

* `RecordItem` - this is a single record to be created / updated / deleted in a batch change
* `APtrRecordItem` - represents a change that will work on both an A and it's corresponding PTR record
* `AAAAPtrRecordItem` - represents a change that will work on both a AAAA and it's corresponding PTR record
* `CNAMERecordItem` - represents a change that will work on a CNAME record item


