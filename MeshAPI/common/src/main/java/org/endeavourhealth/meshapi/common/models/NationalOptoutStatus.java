package org.endeavourhealth.meshapi.common.models;

public class NationalOptoutStatus {

    private String nhsNumber;
    private Integer optInStatus;
    private String dtLastRefreshed;
    private String localId;

    public String getNhsNumber() {
        return nhsNumber;
    }

    public NationalOptoutStatus setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
        return this;
    }

    public Integer getOptInStatus() {
        return optInStatus;
    }

    public NationalOptoutStatus setOptInStatus(Integer optInStatus) {
        this.optInStatus = optInStatus;
        return this;
    }

    public String getDtLastRefreshed() {
        return dtLastRefreshed;
    }

    public NationalOptoutStatus setDtLastRefreshed(String dtLastRefreshed) {
        this.dtLastRefreshed = dtLastRefreshed;
        return this;
    }

    public String getLocalId() {
        return localId;
    }

    public NationalOptoutStatus setLocalId(String localId) {
        this.localId = localId;
        return this;
    }

}

