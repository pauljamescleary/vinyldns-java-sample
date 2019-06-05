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

The sample application can all be found in the `App` class.  It handles setup and teardown of the test
artifacts so that the Batch Changes can run.

The `VinylDNSHelper` provides a small (and perhaps unnecessary) wrapper around the `VinylDNSClient` that comes
from the [VinylDNS-Java Library](https://github.com/vinyldns/vinyldns-java).  All it does is look for an
error in the response and throws an Exception if an error was found.

It is important that batch changes _CAN FAIL_ on submit if certain validations fail, so it is important to
understand that your batch change failed.

If the batch change is successfully submitted, you will get a 202 and the batch change will be queued for processing.

### BatchRequestBuilder

This app has a `BatchRequestBuilder` that simplifies the process of making Batch Change requests.  It works
with a set of classes that further simplify things:

* `RecordItem` - this is a single record to be created / updated / deleted in a batch change
* `APtrRecordItem` - represents a change that will work on both an A and it's corresponding PTR record
* `AAAAPtrRecordItem` - represents a change that will work on both a AAAA and it's corresponding PTR record
* `CNAMERecordItem` - represents a change that will work on a CNAME record item


