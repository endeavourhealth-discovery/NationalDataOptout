package org.endeavourhealth.nationalDataOptout;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "dtsControl")
public class DTSControl {

    private String workFlowId;
    private String toDTS;
    private String localId;

    public String getWorkFlowId() {
        return workFlowId;
    }

    @XmlElement
    public DTSControl setWorkFlowId(String workFlowId) {
        this.workFlowId = workFlowId;
        return this;
    }

    public String getToDTS() {
        return toDTS;
    }

    @XmlElement
    public DTSControl setToDTS(String toDTS) {
        this.toDTS = toDTS;
        return this;
    }

    public String getLocalId() {
        return localId;
    }

    @XmlElement
    public DTSControl setLocalId(String localId) {
        this.localId = localId;
        return this;
    }

}

