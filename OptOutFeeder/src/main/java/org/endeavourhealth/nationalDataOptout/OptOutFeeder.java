package org.endeavourhealth.nationalDataOptout;

import com.fasterxml.jackson.databind.JsonNode;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.utility.MetricsHelper;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class OptOutFeeder {
    private static final Logger LOG = LoggerFactory.getLogger(OptOutFeeder.class);

    public static void main(String[] args) throws Exception {
        try {
            ConfigManager.Initialize("optout-feeder");
            Connection connection = null;
            connection = ConnectionManager.getNonPooledConnection();

            JSONArray nhsNumbers = fetchNHSNumbers(connection);
            callRefreshNhsNumbers(nhsNumbers);
            writeCurrentDataTimeToFile();

            incSize();
        } catch (Exception e) {
            LOG.error("Unable to connect to database", e.getMessage());
        }
    }

    /**
     *
     * @param nhsNumbers
     */
    private static void callRefreshNhsNumbers(JSONArray nhsNumbers) {
        try {
            JsonNode json = ConfigManager.getConfigurationAsJson("batchrunlocation");
            String host = json.get("host").asText();
            URL url = new URL(host+"/api/mesh/refreshNhsNumbers");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write((nhsNumbers.toString()).getBytes());
            os.flush();

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                LOG.debug(output);
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            LOG.error("Exception occured while trying to connect with the Endpoint URL", e.getMessage());
        } catch (IOException e) {
            LOG.error("Exception occured while Input/Output Operation", e.getMessage());
        }
    }

    /**
     *
     */
    private static void writeCurrentDataTimeToFile() {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date today = Calendar.getInstance().getTime();
            String reportDate = df.format(today);
            JsonNode json = ConfigManager.getConfigurationAsJson("batchrunlocation");
            String rootDirectory = json.get("rootdirectory").asText();
            File file = new File(rootDirectory+"OptoutFeederLastRunDate.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(reportDate);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    private static JSONArray fetchNHSNumbers(Connection connection) throws SQLException {
        JSONArray nhsNumbers = new JSONArray();
        DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String date = null;
        try {
            JsonNode json = ConfigManager.getConfigurationAsJson("batchrunlocation");
            String rootDirectory = json.get("rootdirectory").asText();
            File file = new File(rootDirectory+"OptoutFeederLastRunDate.txt");
            if (file.exists()) {
                Scanner sc = new Scanner(file);

                while (sc.hasNextLine())
                    date = sc.nextLine();
            }
        } catch(Exception e) {
            LOG.info("File not found");
        }

        if(date == null || date.isEmpty()) {
            String sql = "SELECT nhs_number FROM patient_search WHERE nhs_number IS NOT NULL";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        nhsNumbers.put(resultSet.getString("nhs_number"));
                    }
                }
            }
        } else {
            String sql = "SELECT nhs_number FROM patient_search WHERE nhs_number IS NOT NULL AND last_updated > ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, date);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        nhsNumbers.put(resultSet.getString("nhs_number"));
                    }
                }
            }
        }
        return nhsNumbers;
    }

    /**
     *
     */
    private static void incSize() {
        MetricsHelper.recordCounter("ConnectionPool.Size").inc();
    }
}
