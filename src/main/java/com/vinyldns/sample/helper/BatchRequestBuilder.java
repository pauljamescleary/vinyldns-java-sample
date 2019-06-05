package com.vinyldns.sample.helper;

import io.vinyldns.java.model.batch.ChangeInput;
import io.vinyldns.java.model.batch.CreateBatchRequest;

import java.util.ArrayList;
import java.util.List;

public class BatchRequestBuilder {
    private final List<ChangeInput> changes;
    private final String ownerGroupId;
    private String comments;

    public BatchRequestBuilder(String ownerGroupId) {
        this.ownerGroupId = ownerGroupId;
        this.changes = new ArrayList<>();
    }

    public BatchRequestBuilder withAddOne(RecordItem item) {
        changes.addAll(item.getAddChanges());
        return this;
    }

    public BatchRequestBuilder withDeleteOne(RecordItem item) {
        changes.addAll(item.getDeleteChanges());
        return this;
    }

    public <T extends RecordItem> BatchRequestBuilder withReplaceOne(T oldItem, T newItem) {
        changes.addAll(oldItem.getDeleteChanges());
        changes.addAll(newItem.getAddChanges());
        return this;
    }

    public BatchRequestBuilder withAddMany(List<RecordItem> itemsToAdd) {
        for (RecordItem item : itemsToAdd) {
            changes.addAll(item.getAddChanges());
        }
        return this;
    }

    public BatchRequestBuilder withDeleteMany(List<RecordItem> itemsToDelete) {
        for (RecordItem item : itemsToDelete) {
            changes.addAll(item.getDeleteChanges());
        }
        return this;
    }

    public BatchRequestBuilder withComments(String comments) {
        this.comments = comments;
        return this;
    }

    public CreateBatchRequest build() {
        CreateBatchRequest request = new CreateBatchRequest(changes);
        request.setOwnerGroupId(ownerGroupId);

        if (comments != null) {
            request.setComments(comments);
        }
        return request;
    }
}
