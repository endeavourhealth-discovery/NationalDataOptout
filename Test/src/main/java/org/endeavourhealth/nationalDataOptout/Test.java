package org.endeavourhealth.nationalDataOptout;

import org.endeavourhealth.common.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {
    private static final Logger LOG = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) throws Exception {

        ConfigManager.Initialize("NationalDataOptoutTest");
        LOG.debug("Test");
    }
}
