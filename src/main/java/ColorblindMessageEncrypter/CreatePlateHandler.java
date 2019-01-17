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
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
            if (event != null)
                responseJson.put("input", event.toJSONString());
        }

        if (params.text != null && params.text != "") {
            handleSuccessfulParse(logger, responseJson, params);
        } else {
            logger.log("Failed to Parse Request\n");
            handleError(logger, responseJson, "Failed to find text to encrypt in body.");
        }

        JSONObject headerJson = new JSONObject();
        headerJson.put("Access-Control-Allow-Origin", "*");
        responseJson.put("isBase64Encoded", false);
        responseJson.put("statusCode", "201");
        responseJson.put("headers", headerJson);
        responseJson.put("body", responseJson.toString());
        responseJson.put("Content-Type", "application/json");

        logger.log("Response JSON:\n" +  responseJson.toJSONString() + "\n");
        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
    }

    private void handleError(LambdaLogger logger, JSONObject responseJson, String error) {
        responseJson.put("Error", error);
    }

    private void handleSuccessfulParse(LambdaLogger logger, JSONObject responseJson, IshiharaParams params)
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
        }
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength((long)os.size());
        meta.setContentType("image/png");

        JSONObject responseBody = new JSONObject();
        try {
            s3.putObject(DST_BUCKET, dstKey, is, meta);
            logger.log("Successfully uploaded Ishihara plate\n");
            String resultUrl = "https://s3-us-west-2.amazonaws.com/"
                    + DST_BUCKET
                    + "/"
                    + dstKey;
            logger.log("Uploaded to: " + resultUrl + "\n");
            responseBody.put("image", resultUrl);
        }
        catch (SdkClientException e) {
            logger.log("Failed to upload image SDKClientException: " + e + "\n");
            handleError(logger, responseJson, "Failed to upload image to S3");
        }
    }
}
