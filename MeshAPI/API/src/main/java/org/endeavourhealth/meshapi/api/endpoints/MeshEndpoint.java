package org.endeavourhealth.meshapi.api.endpoints;

import com.google.common.base.Strings;
import org.endeavourhealth.meshapi.common.dal.MeshJDBCDAL;
import org.endeavourhealth.meshapi.common.models.NationalOptoutStatus;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

@Path("mesh")
public class MeshEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(MeshEndpoint.class);
    MeshJDBCDAL meshDAL;

    /**
     *
     * @param nhsNumbersRequest
     * @return
     * @throws Exception
     */
    @POST
    @Path("/refreshNhsNumbers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshNhsNumbers(InputStream nhsNumbersRequest) throws Exception {
        meshDAL = getMeshObject();
        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(nhsNumbersRequest))) {
            String line;
            content = new StringBuilder();

            while ((line = in.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        }

        JSONArray nhsNumbersReqJSON = new JSONArray(content.toString());
        for (int nhsCount = 0; nhsCount < nhsNumbersReqJSON.length(); nhsCount++) {
            boolean insertInd = meshDAL.insertNhsNumbers((String) nhsNumbersReqJSON.get(nhsCount));
        }
        meshDAL.insertAuditLog(content.toString(), "");
        return Response.ok().build();
    }

    /**
     *
     * @param nhsNumbersRequest
     * @return
     * @throws Exception
     */
    @POST
    @Path("/getOptedOutNhsNumbers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOptedOutNhsNumbers(InputStream nhsNumbersRequest) throws Exception {
        meshDAL = getMeshObject();
        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(nhsNumbersRequest))) {
            String line;
            content = new StringBuilder();

            while ((line = in.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        JSONArray nhsNumbersReqJSON = new JSONArray(content.toString());
        JSONArray optOutResJSON = new JSONArray();
        for (int nhsCount = 0; nhsCount < nhsNumbersReqJSON.length(); nhsCount++) {
            /*boolean validInd = isValidNhsNumber((String) nhsNumbersReqJSON.get(nhsCount));
            if(validInd == Boolean.FALSE) {
                optOutResJSON.put(nationalOptoutStatus.getNhsNumber());
            }*/
            NationalOptoutStatus nationalOptoutStatus = meshDAL.getNhsDetails((String) nhsNumbersReqJSON.get(nhsCount));
            if (nationalOptoutStatus == null) {
                meshDAL.insertNhsNumber((String) nhsNumbersReqJSON.get(nhsCount));
                meshDAL.insertAuditLog(content.toString(), "");
                return Response.status(Response.Status.EXPECTATION_FAILED).build();
            } else if (sdf.parse(nationalOptoutStatus.getDtLastRefreshed()).compareTo(new Date()) > 7) {
                meshDAL.updateDate((String) nhsNumbersReqJSON.get(nhsCount));
                meshDAL.insertAuditLog(content.toString(), "");
                return Response.status(Response.Status.EXPECTATION_FAILED).build();
            } else {
                if(nationalOptoutStatus.getOptInStatus() == 0) {
                    optOutResJSON.put(nationalOptoutStatus.getNhsNumber());
                }
            }
        }
        meshDAL.insertAuditLog(content.toString(), optOutResJSON.toString());
        return Response.ok().entity(optOutResJSON.toString()).build();
    }

    /**
     *
     * @param nhsNumbersRequest
     * @return
     * @throws Exception
     */
    @POST
    @Path("/getOptedInNhsNumbers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOptedInNhsNumbers (InputStream nhsNumbersRequest) throws Exception {
        meshDAL = getMeshObject();
        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(nhsNumbersRequest))) {
            String line;
            content = new StringBuilder();

            while ((line = in.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        JSONArray nhsNumbersReqJSON = new JSONArray(content.toString());
        JSONArray optOutResJSON = new JSONArray();
        for (int nhsCount = 0; nhsCount < nhsNumbersReqJSON.length(); nhsCount++) {
            boolean validInd = isValidNhsNumber((String) nhsNumbersReqJSON.get(nhsCount));
            if(validInd == Boolean.FALSE) {
                optOutResJSON.put((String) nhsNumbersReqJSON.get(nhsCount));
            }
            NationalOptoutStatus nationalOptoutStatus = meshDAL.getNhsDetails((String) nhsNumbersReqJSON.get(nhsCount));
            if (nationalOptoutStatus == null) {
                //insert new row
                meshDAL.insertAuditLog(content.toString(), "");
                return Response.status(Response.Status.EXPECTATION_FAILED).build();
            } else if (sdf.parse(nationalOptoutStatus.getDtLastRefreshed()).compareTo(new Date()) > 7) {
                //update date
                meshDAL.insertAuditLog(content.toString(), "");
                return Response.status(Response.Status.EXPECTATION_FAILED).build();
            } else {
                if(nationalOptoutStatus.getOptInStatus() == 1) {
                    optOutResJSON.put(nationalOptoutStatus.getNhsNumber());
                }
            }
        }
        meshDAL.insertAuditLog(content.toString(), optOutResJSON.toString());
        return Response.ok().entity(optOutResJSON.toString()).build();
    }

    MeshJDBCDAL getMeshObject() {
        return new MeshJDBCDAL();
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