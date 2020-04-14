package org.endeavourhealth.nationalDataOptout;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.apache.commons.lang3.RandomStringUtils;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.utility.MetricsHelper;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MeshWorker {
    private static final Logger LOG = LoggerFactory.getLogger(MeshWorker.class);

    public static void main(String[] args) throws Exception {

        try {
            ConfigManager.Initialize("mesh-api");
            Connection connection = null;
            connection = ConnectionManager.getNonPooledConnection();

            JSONArray nhsNumbers = fetchNHSNumbers(connection);
            writeNhsNumbersToFile(nhsNumbers, connection);
            JSONArray optedInNhsNumbers = readNhsNumbersFromFile(connection);
            for(int nhsCount=0; nhsCount<optedInNhsNumbers.length(); nhsCount++) {
                saveStatusToDB((String) optedInNhsNumbers.get(nhsCount), connection);
            }

            incSize();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    private static void incSize() {
        MetricsHelper.recordCounter("ConnectionPool.Size").inc();
    }

    /**
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    private static JSONArray fetchNHSNumbers(Connection connection) throws SQLException {
        JSONArray nhsNumbers = new JSONArray();

        String sql = "SELECT nhs_number FROM national_opt_out_status WHERE (dt_last_refreshed < CURRENT_DATE - INTERVAL 4 DAY OR dt_last_refreshed is NULL) AND local_id is NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    nhsNumbers.put(resultSet.getString("nhs_number"));
                }
            }
        }
        return nhsNumbers;
    }

    /**
     *
     * @param nhsNumbers
     */
    private static void writeNhsNumbersToFile(JSONArray nhsNumbers, Connection connection) {
        try {
            JsonNode json = ConfigManager.getConfigurationAsJson("meshpath");
            String rootDirectory = json.get("rootdirectory").asText();
            String localId = RandomStringUtils.randomAlphabetic(5);
            File directory = new File(rootDirectory.concat("MESH_Outbox"));
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            Date date = new Date();
            String localIdAndDate = localId+"_"+dateFormat.format(date);
            File file  = new File(directory.getAbsolutePath().concat("//"+localId+".dat"));
            File controlFile  = new File(directory.getAbsolutePath().concat("//"+localId+".ctl"));
            if(!directory.exists()) {
                directory.mkdir();
                if(!file.exists()) {
                    file.createNewFile();
                    controlFile.createNewFile();
                }
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            for(int nhsCount=0; nhsCount<nhsNumbers.length(); nhsCount++) {
                String nhsNum = (String) nhsNumbers.get(nhsCount);
                bw.write(nhsNum+",\r\n");
                updateLocalIdToDB(nhsNum, localId, connection);
            }
            bw.close();

            JsonNode jsonConfig = ConfigManager.getConfigurationAsJson("meshconfig");
            String workFlowId = jsonConfig.get("WorkflowId").asText();
            String toDTS = jsonConfig.get("ToDTS").asText();

            DTSControl dtsControl = new DTSControl();
            dtsControl.setWorkFlowId(workFlowId);
            dtsControl.setToDTS(toDTS);
            dtsControl.setLocalId(localIdAndDate);

            FileWriter fwCf = new FileWriter(controlFile.getAbsoluteFile());
            BufferedWriter bwCf = new BufferedWriter(fwCf);
            bwCf.write(jaxbObjectToXML(dtsControl)+",\r\n");
            bwCf.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param dtsControl
     * @return
     */
    private static String jaxbObjectToXML(DTSControl dtsControl) {
        StringWriter sw = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DTSControl.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.marshal(dtsControl, sw);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return sw.toString();
    }

    /**
     *
     * @return
     */
    private static JSONArray readNhsNumbersFromFile(Connection connection) throws Exception {
        JSONArray nhsNumbers = new JSONArray();
        JsonNode json = ConfigManager.getConfigurationAsJson("meshpath");
        String rootDirectory = json.get("rootdirectory").asText();
        File directory = new File(rootDirectory.concat("MESH_Inbox"));
        File[] listingAllFiles = directory.listFiles();
        ArrayList<File> allFiles = iterateOverFiles(listingAllFiles);

        for (File file : allFiles) {
            if(file != null) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.substring(0, line.length() - 1);
                        nhsNumbers.put(line);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(file.renameTo(new File(rootDirectory+"MESH_Inbox_Archive\\"+file.getName()))) {
                    file.delete();
                }
            }

            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            File controlFile  = new File(directory.getAbsolutePath().concat("//"+fileName+".ctl"));
            if(controlFile != null) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(controlFile);

                doc.getDocumentElement().normalize();
                NodeList nList = doc.getElementsByTagName("DTSControl");

                if(nList != null) {
                    Node elemNode = nList.item(0);
                    Element eElement = (Element) elemNode;
                    String localId = eElement.getElementsByTagName("LocalId").item(0).getTextContent();
                    String[] local = localId.split("_");
                    saveStatusToDBLocalId(local[0], (local[1].substring(0,4))+"-"+(local[1].substring(4,6))+"-"+(local[1].substring(6,8)), connection);
                }
                if(controlFile.renameTo(new File(rootDirectory+"MESH_Inbox_Archive\\"+controlFile.getName()))) {
                    controlFile.delete();
                }
            }
        }
        return nhsNumbers;
    }

    /**
     *
     * @param files
     * @return
     */
    public static ArrayList<File> iterateOverFiles(File[] files) {
        ArrayList<File> al = new ArrayList<File>();
        for (File file : files) {
            if(file.getName().toLowerCase().endsWith("dat")) {
                al.add(file);
            }
        }
        return al;
    }

    /**
     *
     * @param nhsNumber
     * @param connection
     * @return
     * @throws SQLException
     */
    private static boolean saveStatusToDB(String nhsNumber, Connection connection) throws SQLException {
        String sql = "UPDATE national_opt_out_status SET opt_in_status = 1 WHERE nhs_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nhsNumber);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     *
     * @param localId
     * @param date
     * @param connection
     * @return
     * @throws SQLException
     */
    private static boolean saveStatusToDBLocalId(String localId, String date, Connection connection) throws SQLException {
        String sql = "UPDATE national_opt_out_status SET opt_in_status = 0, dt_last_refreshed = ?, local_id = NULL WHERE local_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, date);
            stmt.setString(2, localId);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     *
     * @param nhsNum
     * @param localId
     * @param connection
     * @return
     * @throws SQLException
     */
    private static boolean updateLocalIdToDB(String nhsNum, String localId, Connection connection) throws SQLException {
        String sql = "UPDATE national_opt_out_status SET local_id = ? WHERE nhs_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, localId);
            stmt.setString(2, nhsNum);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * validates if a String is a valid NHS number
     * expects NHS numbers without any delimiters or spacers (NNNNNNNNNN)
     *
     * returns null if empty, true if valid, false if not valid
     */
    public static Boolean isValidNhsNumber(String toTest) {
        if (Strings.isNullOrEmpty(toTest)) {
            return null;
        }

        if (toTest.length() != 10) {
            return Boolean.FALSE;
        }

        int sum = 0;
        char[] chars = toTest.toCharArray();
        for (int i=0; i<9; i++) {
            char c = chars[i];

            if (!Character.isDigit(c)) {
                return Boolean.FALSE;
            }

            int val = Character.getNumericValue(c);
            int weight = 10 - i;
            int m = val * weight;
            sum += m;
        }

        int remainder = sum % 11;
        int check = 11 - remainder;
        if (check == 11) {
            check = 0;
        }
        if (check == 10) {
            return Boolean.FALSE;
        }

        char lastChar = chars[9];
        int actualCheck = Character.getNumericValue(lastChar);
        if (check != actualCheck) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}