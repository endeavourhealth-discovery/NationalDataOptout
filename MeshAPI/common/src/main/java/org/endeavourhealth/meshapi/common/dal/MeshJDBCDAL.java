package org.endeavourhealth.meshapi.common.dal;

import org.endeavourhealth.meshapi.common.models.NationalOptoutStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.InetAddress;
import java.sql.*;

public class MeshJDBCDAL extends BaseJDBCDAL {
    private static final Logger LOG = LoggerFactory.getLogger(MeshJDBCDAL.class);

    /**
     *
     * @param nhsNumber
     * @return
     * @throws Exception
     */
    public boolean insertNhsNumbers(String nhsNumber) throws Exception {
        String sql = "INSERT IGNORE INTO national_opt_out_status(nhs_number) VALUES (?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public boolean insertAuditLog(String requestBody, String responseBody, long duration, String reqPath) throws Exception {
        InetAddress ip = InetAddress.getLocalHost();
        String sql = "INSERT INTO api_audit VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            statement.setString(1, sdf.format(new Date()));
            statement.setString(2, "");
            statement.setString(3, ip.getHostAddress());
            statement.setString(4, reqPath);
            statement.setString(5, "");
            statement.setString(6, requestBody);
            statement.setInt(7, 0);
            statement.setString(8, responseBody);
            statement.setInt(9, (int) duration);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     *
     * @param nhsNumber
     * @return
     * @throws Exception
     */
    public NationalOptoutStatus getNhsDetails(String nhsNumber) throws Exception {
        String sql = "SELECT nhs_number, opt_in_status, dt_last_refreshed FROM national_opt_out_status WHERE nhs_number = ?";
        NationalOptoutStatus nationalOptoutStatus = new NationalOptoutStatus();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nhsNumber);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    nationalOptoutStatus = new NationalOptoutStatus()
                            .setNhsNumber(resultSet.getString("nhs_number"))
                            .setOptInStatus(resultSet.getInt("opt_in_status"))
                            .setDtLastRefreshed(resultSet.getString("dt_last_refreshed"));
                }
            }
        }
        return nationalOptoutStatus;
    }

    /**
     *
     * @param nhsNumber
     * @return
     * @throws SQLException
     */
    public boolean updateDate(String nhsNumber) throws SQLException {
        String sql = "UPDATE national_opt_out_status SET dt_last_refreshed = NULL WHERE nhs_number = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nhsNumber);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     *
     * @param nhsNumber
     * @return
     * @throws Exception
     */
    public boolean insertNhsNumber(String nhsNumber) throws Exception {
        String sql = "INSERT IGNORE INTO national_opt_out_status VALUES (?, null, null)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, nhsNumber);
            return statement.executeUpdate() == 1;
        }
    }

}
