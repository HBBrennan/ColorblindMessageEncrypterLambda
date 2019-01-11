package ColorblindMessageEncrypter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.security.MessageDigest;

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

import javax.imageio.ImageIO;

import static java.lang.Math.min;

public class CreatePlateHandler implements RequestStreamHandler {
    JSONParser parser = new JSONParser();
    private String DST_BUCKET = System.getenv("S3_BUCKET");

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of ColorblindMessageEncrypter\n");

        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        IshiharaParams params = new IshiharaParams();
        JSONObject event = null;
        try {
            event = (JSONObject)parser.parse(reader);
            logger.log("Received event: " + event.toJSONString() + "\n");
            if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject)event.get("queryStringParameters");
                if ( qps.get("text") != null) {

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
                if ( body.get("text") != null) {
                    params.text = (String)body.get("text");
                    logger.log("Found string: " + params.text);
                }
            }

        } catch(ParseException pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
            if (event != null)
                responseJson.put("input", event.toJSONString());
        }

        if (params.text != "") {
            handleSuccessfulParse(logger, responseJson, params);
        } else {
            handleFailedParse(logger, responseJson);
        }

        logger.log(responseJson.toJSONString());
        @Cleanup OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
    }

    void handleFailedParse(LambdaLogger logger, JSONObject responseJson) {

    }

    void handleSuccessfulParse(LambdaLogger logger, JSONObject responseJson, IshiharaParams params)
    {
        //Create image
        IshiharaGenerator ishiharaGenerator = new IshiharaGenerator();
        BufferedImage image = ishiharaGenerator.CreateImage(params.text, new Rectangle(params.requestedWidth, params.requestedHeight), false, 4);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        String dstKey = Base64.getEncoder().withoutPadding().encodeToString(hash);

        //Check database if this image has already been generated

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
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

        logger.log("Writing to: " + DST_BUCKET + "/" + dstKey + "\n");
        JSONObject responseBody = new JSONObject();
        try {
            s3Client.putObject(DST_BUCKET, dstKey, is, meta);
            logger.log("Successfully created Ishihara plate with uploaded to " + DST_BUCKET + "/" + dstKey + "\n");
            String resultUrl = "https://s3-us-west-2.amazonaws.com/"
                    + DST_BUCKET
                    + "/"
                    + dstKey;
            responseBody.put("image", resultUrl);
        }
        catch (SdkClientException e) {
            logger.log(e.toString());
            responseBody.put("error", "Failed to upload image to S3");
        }

        JSONObject headerJson = new JSONObject();
        headerJson.put("Access-Control-Allow-Origin", "*");

        responseJson.put("isBase64Encoded", false);
        responseJson.put("statusCode", "201");
        responseJson.put("headers", headerJson);
        responseJson.put("body", responseBody.toString());
        responseJson.put("Content-Type", "application/json");
    }
}
