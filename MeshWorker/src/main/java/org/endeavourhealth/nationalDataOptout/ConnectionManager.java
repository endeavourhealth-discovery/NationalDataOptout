package org.endeavourhealth.nationalDataOptout;

import com.fasterxml.jackson.databind.JsonNode;
import org.endeavourhealth.common.config.ConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class ConnectionManager {

    /**
     *
     * @return
     * @throws Exception
     */
    public static Connection getNonPooledConnection() throws Exception {
        JsonNode json = ConfigManager.getConfigurationAsJson("database");
        String url = json.get("url").asText();
        String user = json.get("username").asText();
        String pass = json.get("password").asText();
        String driver = json.get("class") == null ? null : json.get("class").asText();

        if (driver != null && !driver.isEmpty())
            Class.forName(driver);

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);

        Connection connection = DriverManager.getConnection(url, props);
        connection.setAutoCommit(false); //so this matches the pooled connections
        return connection;
    }

}
