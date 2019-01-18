package ColorblindMessageEncrypter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import lombok.Cleanup;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;


public class CreatePlateHandler implements RequestStreamHandler {
    JSONParser parser = new JSONParser();
    private String DST_BUCKET = System.getenv("DST_BUCKET");

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of ColorblindMessageEncrypter\n");
        if (DST_BUCKET == null || DST_BUCKET.isEmpty()) {
            logger.log("Failed to load DST_BUCKET environment variable. Using Default\n");
            DST_BUCKET = "colorblind-message-encrypter-plates";
        }
        logger.log("DST_BUCKET: " + DST_BUCKET + "\n");

        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        JSONObject responseBodyJson = new JSONObject();
        IshiharaParams params = new IshiharaParams();
        JSONObject event = null;
        try {
            event = (JSONObject)parser.parse(reader);
            logger.log("Received event: " + event.toJSONString() + "\n");
            if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject)event.get("queryStringParameters");
            }

            if (event.get("pathParameters") != null) {
                JSONObject pps = (JSONObject)event.get("pathParameters");
            }

            if (event.get("headers") != null) {
                JSONObject hps = (JSONObject)event.get("headers");
            }

            if (event.get("body") != null) {
                JSONObject body = (JSONObject)parser.parse((String)event.get("body"));
                if (body.get("text") != null) {
                    params.text = (String)body.get("text");
                    logger.log("Found string: " + params.text + "\n");
                }
            }

        } catch(ParseException pex) {
            responseBodyJson.put("statusCode", "400");
            responseBodyJson.put("exception", pex);
            if (event != null)
                responseBodyJson.put("input", event.toJSONString());
        }

        JSONObject responseHeaderJson = new JSONObject();

        if (params.text != null && params.text != "") {
            boolean result = handleSuccessfulParse(logger, responseJson, responseBodyJson, params);
            if (result) {
                responseJson.put("statusCode", "201");
            } else {
                responseJson.put("statusCode", "500");
            }
        } else {
            responseHeaderJson.put("exception", "Failed to find text to encrypt in body.");
            responseJson.put("statusCode", "400");
        }

        responseHeaderJson.put("Access-Control-Allow-Origin", "*");
        responseJson.put("isBase64Encoded", false);
        responseJson.put("Content-Type", "application/json");

        responseJson.put("headers", responseHeaderJson);
        responseJson.put("body", responseBodyJson);

        logger.log(responseJson.toJSONString());
        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
    }

    private boolean handleSuccessfulParse(LambdaLogger logger, JSONObject responseJson, JSONObject responseBodyJson, IshiharaParams params)
    {
        logger.log("Successfully Parsed Request\n");
        //Create image
        IshiharaGenerator ishiharaGenerator = new IshiharaGenerator();
        BufferedImage image = ishiharaGenerator.CreateImage(params.text, new Rectangle(params.requestedWidth, params.requestedHeight), false, 4);
        String dstKey = org.apache.commons.codec.digest.DigestUtils.sha256Hex(params.text) + ".png";

        //TODO Check database if this image has already been generated

        final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.log(e.toString());
            responseJson.put("exception", e);
            return false;
        }
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength((long)os.size());
        meta.setContentType("image/png");

        try {
            s3.putObject(DST_BUCKET, dstKey, is, meta);
            logger.log("Successfully uploaded Ishihara plate\n");
            String resultUrl = "https://s3-us-west-2.amazonaws.com/"
                    + DST_BUCKET
                    + "/"
                    + dstKey;
            logger.log("Uploaded to: " + resultUrl + "\n");
            responseBodyJson.put("image", resultUrl);
            return true;
        }
        catch (SdkClientException e) {
            logger.log("Failed to upload image SDKClientException: " + e + "\n");
            responseJson.put("exception", e);
            return false;
        }
    }
}
