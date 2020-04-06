package org.endeavourhealth.meshapi.common.models;

public class ApiAudit {

    private String timeStamp;
    private Character userUuid;
    private String remoteAddress;
    private String requestPath;
    private String requestHeaders;
    private String requestBody;
    private Integer responseCode;
    private String responseBody;
    private Integer duration;

    public String getTimeStamp() {
        return timeStamp;
    }

    public ApiAudit setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public Character getUserUuid() {
        return userUuid;
    }

    public ApiAudit setUserUuid(Character userUuid) {
        this.userUuid = userUuid;
        return this;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public ApiAudit setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public ApiAudit setRequestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public ApiAudit setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public ApiAudit setRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public ApiAudit setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public ApiAudit setResponseBody(String responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public ApiAudit setDuration(Integer duration) {
        this.duration = duration;
        return this;
    }

}

