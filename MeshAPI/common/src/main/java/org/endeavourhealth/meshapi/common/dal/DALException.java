package org.endeavourhealth.meshapi.common.dal;

class DALException extends RuntimeException {
    public DALException(String message) {
        super(message);
    }
    public DALException(String message, Throwable err) {
        super(message, err);
    }
}
