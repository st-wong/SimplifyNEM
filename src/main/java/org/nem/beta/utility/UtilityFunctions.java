package org.nem.beta.utility;

import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;

import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.model.FeeUnitAwareTransactionFeeCalculator;
import org.nem.core.model.mosaic.MosaicFeeInformation;
import org.nem.core.model.mosaic.MosaicFeeInformationLookup;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.Supply;
import org.nem.core.time.SystemTimeProvider;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class UtilityFunctions implements MosaicFeeInformationLookup {

    private static CloseableHttpClient httpClient;
    private static String urlNode;
    private static final String API_MOSAIC_DEFINITION_PAGE = "/namespace/mosaic/definition/page";
    public FeeUnitAwareTransactionFeeCalculator TransactionFeeCalculator = new FeeUnitAwareTransactionFeeCalculator(Amount.fromMicroNem(50_000L), this);

    public UtilityFunctions(String url) {
        urlNode = url;

        try {
            httpClient = HttpClients.createDefault();
        } catch (Exception e) {
            System.out.println(String.format("Exception caught: %s", e.getMessage()));
            e.printStackTrace();
        }
    }

    public static void Disconnect() {
        try {
            httpClient.close();
        } catch (Exception e) {
            System.out.println(String.format("Exception caught: %s", e.getMessage()));
            e.printStackTrace();
        }
    }

    //Present results in a more readable manner
    public static void DecodeMessage(JSONObject transactionJSON) {
        StringBuilder outMessage = new StringBuilder();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        outMessage.append(String.format("%nSender Address: %s", Address.fromPublicKey(PublicKey.fromHexString(transactionJSON.getString("signer"))).toString()));
        outMessage.append(String.format("%nTransaction Amount: %d XEM", Amount.fromMicroNem(transactionJSON.getLong("amount")).getNumNem()));
        outMessage.append(String.format("%nTimestamp: %s", dateFormat.format(new Date((transactionJSON.getLong("timeStamp") * 1000) + SystemTimeProvider.getEpochTimeMillis()))));

        // Get Non encrypted message
        if (transactionJSON.getJSONObject("message").containsKey("type") && transactionJSON.getJSONObject("message").getInt("type") == 1) {
            JSONObject messageJSON = transactionJSON.getJSONObject("message");
            outMessage.append(String.format("%nMessage: %s", new String(ByteUtils.fromHexString(messageJSON.getString("payload")))));
        } else {
            outMessage.append(String.format("%nNo Message"));
        }

        System.out.println(outMessage.toString());
    }

    //HTTP Post call
    public static String PostResults(String APItoCall, JSONObject paramsJSON) {
        CloseableHttpResponse response;
        String result = "";

        try {
            HttpPost method = new HttpPost(urlNode + APItoCall);
            StringEntity params = new StringEntity(paramsJSON.toString());
            method.setHeader("content-type", "application/json");
            method.setEntity(params);
            response = httpClient.execute(method);
            result = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            System.out.println(String.format("Exception caught: %s", e.getMessage()));
            e.printStackTrace();
        }

        return result;
    }

    //HTTP Get call
    public static String GetResults(String APItoCall, String ParamstoPass) {
        CloseableHttpResponse response;
        String result = "";

        try {
            HttpGet method = new HttpGet(urlNode + APItoCall + "?" + ParamstoPass);
            response = httpClient.execute(method);
            result = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            System.out.println(String.format("Exception caught: %s", e.getMessage()));
            e.printStackTrace();
        }

        return result;
    }

    //HTTP Get Mosaic Fee Information
    public MosaicFeeInformation findById(final MosaicId id) {
        CloseableHttpResponse response = null;
        String result = "";

        try {
            HttpGet method = new HttpGet(urlNode + API_MOSAIC_DEFINITION_PAGE + "?namespace=" + id.getNamespaceId().toString());
            response = httpClient.execute(method);
            result = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            System.out.println(String.format("Exception caught: %s", e.getMessage()));
            e.printStackTrace();
        }

        JSONArray jsonArr = JSONObject.fromObject(result).getJSONArray("data");

        for (int i = 0; i < jsonArr.size(); i++) {
            JSONObject mosaic = jsonArr.getJSONObject(i).getJSONObject("mosaic");

            if (id.getName().equals(jsonArr.getJSONObject(i).getJSONObject("mosaic").getJSONObject("id").getString("name"))) {
                JSONArray properties = mosaic.getJSONArray("properties");
                String initialSupply = "";
                String divisibility = "";

                for (int j = 0; j < properties.size(); j++) {
                    JSONObject property = properties.getJSONObject(j);

                    if ("initialSupply".equals(property.getString("name"))) {
                        initialSupply = property.getString("value");
                    } else if ("divisibility".equals(property.getString("name"))) {
                        divisibility = property.getString("value");
                    }
                }

                if (!"".equals(initialSupply) && !"".equals(divisibility)) {
                    return new MosaicFeeInformation(Supply.fromValue(Long.valueOf(initialSupply)), Integer.valueOf(divisibility));
                }
            }
        }

        return null;
    }
}