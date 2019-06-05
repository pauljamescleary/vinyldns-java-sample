package com.vinyldns.sample.helper;

import io.vinyldns.java.model.batch.AddChangeInput;
import io.vinyldns.java.model.batch.ChangeInput;
import io.vinyldns.java.model.batch.DeleteRecordSetChangeInput;
import io.vinyldns.java.model.record.RecordType;
import io.vinyldns.java.model.record.data.CNAMEData;

import java.util.Arrays;
import java.util.List;

public class CNAMERecordItem implements RecordItem {
    private final String fqdn;
    private final String cname;

    public CNAMERecordItem(String fqdn, String cname) {
        this.fqdn = fqdn;
        this.cname = cname;
    }

    public List<ChangeInput> getAddChanges() {
        return Arrays.asList(new AddChangeInput(fqdn, RecordType.CNAME, 7200L, new CNAMEData(cname)));
    }

    public List<ChangeInput> getDeleteChanges() {
        return Arrays.asList(new DeleteRecordSetChangeInput(fqdn, RecordType.CNAME));
    }
}
