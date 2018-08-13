package ColorblindMessageEncrypter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;


public class CreatePlateHandler implements RequestStreamHandler {
    JSONParser parser = new JSONParser();
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of ColorblindMessageEncrypter");

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        String text;
        Long width, height, color;
        String responseCode = "200";

        try {
            JSONObject event = (JSONObject)parser.parse(reader);
            if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject)event.get("queryStringParameters");
                if ( qps.get("name") != null) {
                    text = (String)qps.get("text");
                }

                if (qps.get("width") != null) {
                    width =(Long) qps.get("width");
                }
                if (qps.get("height") != null) {
                    height = (Long) qps.get("height");
                }
                if (qps.get("color") != null) {
                    color = (Long) qps.get("color");
                }
            }

            if (event.get("pathParameters") != null) {
                JSONObject pps = (JSONObject)event.get("pathParameters");
                if ( pps.get("proxy") != null) {
                }
            }

            if (event.get("headers") != null) {
                JSONObject hps = (JSONObject)event.get("headers");
                if ( hps.get("day") != null) {
                }
            }

            if (event.get("body") != null) {
                JSONObject body = (JSONObject)parser.parse((String)event.get("body"));
                if ( body.get("time") != null) {
                }
            }

            //Create image

            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            responseBody.put("image", "https://s3-us-west-2.amazonaws.com/colorblind-message-encrypter-plates/Test.PNG");

            JSONObject headerJson = new JSONObject();
            //headerJson.put("x-custom-header", "my custom header value");

            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

        } catch(ParseException pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }
}
