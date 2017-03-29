import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import net.sf.json.*;

public class simplifyNEM {
    public static void main(String[] args) {
        String results = GetResults(accountAddress);
        JSONObject resultJSON = JSONObject.fromObject(results);
        System.out.println(String.format("JSONObject Value: %s", resultJSON.get("meta")));
        System.out.println(String.format("JSONObject Value: %s", resultJSON.get("account")));
        System.out.println(String.format("Actual Results: %s", results));

        results = PostResults(accountPrivateKey);
        resultJSON = JSONObject.fromObject(results);
        System.out.println(String.format("JSONObject Value: %s", resultJSON.get("data")));
        System.out.println(String.format("Actual Results: %s", results));

        //results = CreateAccountDeposit(accountPrivateKey, 50000);
        //resultJSON = JSONObject.fromObject(results);
        //System.out.println(String.format("JSONObject Value: %s", resultJSON.toString()));
        //System.out.println(String.format("Actual Results: %s", results));
    }

    private static String GetResults(String accountAddress) {
        CloseableHttpResponse response = null;
        String result = "";

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet method = new HttpGet("http://127.0.0.1:7890/account/get?address=" + accountAddress);
            response = httpClient.execute(method);
            result = EntityUtils.toString(response.getEntity());
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String PostResults(String accountPrivateKey) {
        CloseableHttpResponse response = null;
        String result = "";

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost method = new HttpPost("http://127.0.0.1:7890/local/account/transfers/all");
            StringEntity paramJSON = new StringEntity("{\"value\":\""+ accountPrivateKey +"\"}");
            method.setHeader("content-type", "application/json");
            method.setEntity(paramJSON);

            response = httpClient.execute(method);
            result = EntityUtils.toString(response.getEntity());
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String CreateAccountDeposit(String fromPrivateKey, int Amount) {
        CloseableHttpResponse response = null;
        String result = "";

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet getMethod = new HttpGet("http://127.0.0.1:7890/account/generate");
            response = httpClient.execute(getMethod);
            result = EntityUtils.toString(response.getEntity());
            JSONObject resultJSON = JSONObject.fromObject(result);

            HttpPost postMethod = new HttpPost("http://127.0.0.1:7890/transaction/prepare-announce");
            JSONObject paramJSONObj = new JSONObject();
            JSONObject transactionJSONObj = new JSONObject();
            paramJSONObj.put("transaction", transactionJSONObj);
            paramJSONObj.put("privateKey", fromPrivateKey);
            postMethod.setHeader("content-type", "application/json");
            postMethod.setEntity(new StringEntity(paramJSONObj.toString()));

            response = httpClient.execute(postMethod);
            response = httpClient.execute(getMethod);
            result = EntityUtils.toString(response.getEntity());
            httpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}