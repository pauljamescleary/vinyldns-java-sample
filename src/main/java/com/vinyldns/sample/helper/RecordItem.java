package com.vinyldns.sample.helper;

import io.vinyldns.java.model.batch.ChangeInput;

import java.util.List;

public interface RecordItem {
    List<ChangeInput> getAddChanges();

    List<ChangeInput> getDeleteChanges();
}
