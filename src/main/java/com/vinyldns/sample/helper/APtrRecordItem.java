package com.vinyldns.sample.helper;

import io.vinyldns.java.model.batch.AddChangeInput;
import io.vinyldns.java.model.batch.ChangeInput;
import io.vinyldns.java.model.batch.DeleteRecordSetChangeInput;
import io.vinyldns.java.model.record.RecordType;
import io.vinyldns.java.model.record.data.AData;
import io.vinyldns.java.model.record.data.PTRData;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class APtrRecordItem implements RecordItem {
    private final String fqdn;
    private final String hostAddress;

    public APtrRecordItem(String fqdn, InetAddress address) {
        this.fqdn = fqdn;
        this.hostAddress = address.getHostAddress();
    }

    public List<ChangeInput> getAddChanges() {
        return Arrays.asList(
                new AddChangeInput(fqdn, RecordType.A, 7200L, new AData(hostAddress)),
                new AddChangeInput(hostAddress, RecordType.PTR, 7200L, new PTRData(fqdn)));
    }

    public List<ChangeInput> getDeleteChanges() {
        return Arrays.asList(
                new DeleteRecordSetChangeInput(fqdn, RecordType.A),
                new DeleteRecordSetChangeInput(hostAddress, RecordType.PTR));
    }
}
