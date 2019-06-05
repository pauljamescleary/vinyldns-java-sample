package com.vinyldns.sample.helper;

import com.amazonaws.auth.BasicAWSCredentials;
import io.vinyldns.java.VinylDNSClient;
import io.vinyldns.java.VinylDNSClientConfig;
import io.vinyldns.java.VinylDNSClientImpl;
import io.vinyldns.java.model.batch.BatchResponse;
import io.vinyldns.java.model.batch.CreateBatchRequest;
import io.vinyldns.java.responses.VinylDNSResponse;

/**
 * This is a helper class that makes it easier to work with the VinylDNSClient
 */
public class VinylDNSHelper {
    private final VinylDNSClient vinylDNSClient;

    /**
     * Create a new VinylDNSHelper instance using environment variables
     */
    public VinylDNSHelper() {
        // Default constructor assumes environment variables or will throw an error if not set
        // Note: if any of these are not in the environment, this will fail
        String accessKey = System.getenv("VINYLDNS_ACCESS_KEY");
        String secretKey = System.getenv("VINYLDNS_SECRET_KEY");
        String vinylDNSUrl = System.getenv("VINYLDNS_URL");

        if (accessKey == null || secretKey == null || vinylDNSUrl == null) {
            throw new RuntimeException("Unable to load vinyldns, environment variables not found");
        }

        VinylDNSClientConfig config =
                new VinylDNSClientConfig(vinylDNSUrl, new BasicAWSCredentials(accessKey, secretKey));
        this.vinylDNSClient = new VinylDNSClientImpl(config);
    }

    public VinylDNSClient getVinylDNSClient() {
        return vinylDNSClient;
    }

    /**
     * Create a new VinylDNSHelper instance using the keys and url provided
     *
     * @param accessKey   The access key for the VinylDNS user
     * @param secretKey   The secret key (PRIVATE!) for the VinylDNS user
     * @param vinylDNSUrl The url endpoint for vinyldns
     */
    public VinylDNSHelper(String accessKey, String secretKey, String vinylDNSUrl) {
        if (accessKey == null || secretKey == null || vinylDNSUrl == null) {
            throw new RuntimeException("Unable to load vinyldns, environment variables not found");
        }

        VinylDNSClientConfig config =
                new VinylDNSClientConfig(vinylDNSUrl, new BasicAWSCredentials(accessKey, secretKey));
        this.vinylDNSClient = new VinylDNSClientImpl(config);
    }

    /**
     * Submits a batch request to make multiple DNS record changes
     *
     * <p>If everything is successful, returns the BatchResponse that can be inspected if need be
     *
     * @param request A populated CreateBatchRequest instance
     * @return BatchChangeResponse
     * @throws BatchRequestError in the event that there are any errors with the batch that was
     *                           submitted. These could be "zone does not exist" for example.
     */
    public BatchResponse submitBatchRequest(CreateBatchRequest request) throws BatchRequestError {
        VinylDNSResponse<BatchResponse> response = vinylDNSClient.createBatchChanges(request);
        if (response.getStatusCode() > 202) {
            // we have errors
            throw new BatchRequestError(response.getMessageBody());
        } else {
            return response.getValue();
        }
    }
}
