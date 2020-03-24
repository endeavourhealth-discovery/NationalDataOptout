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
import java.sql.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OptOutFeeder {
    private static final Logger LOG = LoggerFactory.getLogger(OptOutFeeder.class);

    public static void main(String[] args) throws Exception {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            ConfigManager.Initialize("optout-feeder");
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

            Connection connection = DriverManager.getConnection(url, props);    // NOSONAR

            JSONArray nhsNumbers = fetchNHSNumbers(connection);
            writeCurrentDataTimeToFile();
            callRefreshNhsNumbers(nhsNumbers);

            incSize();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     *
     * @param nhsNumbers
     */
    private static void callRefreshNhsNumbers(JSONArray nhsNumbers) {
        try {
            URL url = new URL("http://localhost:8080/api/mesh/refreshNhsNumbers");
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
                System.out.println(output);
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
            File file = new File("d://reportDate.txt");
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
            File file = new File("d://reportDate.txt");
            Scanner sc = new Scanner(file);

            while (sc.hasNextLine())
                date = sc.nextLine();
        } catch(FileNotFoundException e) {
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
