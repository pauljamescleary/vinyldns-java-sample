package com.vinyldns.sample.helper;

import io.vinyldns.java.model.batch.AddChangeInput;
import io.vinyldns.java.model.batch.ChangeInput;
import io.vinyldns.java.model.batch.DeleteRecordSetChangeInput;
import io.vinyldns.java.model.record.RecordType;
import io.vinyldns.java.model.record.data.AAAAData;
import io.vinyldns.java.model.record.data.PTRData;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class AAAAPtrRecordItem implements RecordItem {
    private final String fqdn;
    private final String hostAddress;

    public AAAAPtrRecordItem(String fqdn, InetAddress address) {
        this.fqdn = fqdn;
        this.hostAddress = address.getHostAddress();
    }

    public List<ChangeInput> getAddChanges() {
        return Arrays.asList(
                new AddChangeInput(fqdn, RecordType.A, 7200L, new AAAAData(hostAddress)),
                new AddChangeInput(hostAddress, RecordType.PTR, 7200L, new PTRData(fqdn)));
    }

    public List<ChangeInput> getDeleteChanges() {
        return Arrays.asList(
                new DeleteRecordSetChangeInput(fqdn, RecordType.AAAA),
                new DeleteRecordSetChangeInput(hostAddress, RecordType.PTR));
    }
}
