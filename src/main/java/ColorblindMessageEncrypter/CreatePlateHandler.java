package ColorblindMessageEncrypter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
		JSONObject responseHeaderJson = new JSONObject();
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
				if (hps.get("Content-Type") != null && !((String)hps.get("Content-Type")).equals("application/json")) {
					responseJson.put("statusCode", "415");
					responseHeaderJson.put("exception", "Wrong payload content type. Should be application/json");
				}
			}

			if (event.get("body") != null) {
				JSONObject body = (JSONObject)parser.parse((String)event.get("body"));
				if (body.get("text") != null) {
					params.setText((String)body.get("text"));
					logger.log("Found string: " + params.getText() + "\n");
					if (params.getText().isEmpty()) {
						responseJson.put("statusCode", "400");
						responseHeaderJson.put("exception", "Error: Empty Text");
					} else {
						if (handleSuccessfulParse(logger, responseJson, responseBodyJson, params)) {
							responseJson.put("statusCode", "201");
						} else {
							responseJson.put("statusCode", "500");
						}
					}
				} else {
					responseHeaderJson.put("exception", "Failed to find \"text\" in body.");
					responseJson.put("statusCode", "400");
				}
			}
			responseJson.put("statusCode", "400");
			responseHeaderJson.put("exception", "Error: Text not found in body");
		} catch(ParseException pex) {
			responseBodyJson.put("statusCode", "400");
			responseBodyJson.put("exception", pex);
			if (event != null)
				responseBodyJson.put("input", event.toJSONString());
		}


		responseHeaderJson.put("Access-Control-Allow-Origin", "*");
		responseJson.put("isBase64Encoded", false);

		responseJson.put("headers", responseHeaderJson);
		responseJson.put("body", responseBodyJson.toJSONString());

		logger.log(responseJson.toJSONString());
		@Cleanup OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
		writer.write(responseJson.toJSONString());
	}

	private boolean handleSuccessfulParse(LambdaLogger logger, JSONObject responseJson, JSONObject responseBodyJson, IshiharaParams params)
	{
		logger.log("Successfully Parsed Request\n");
		//Create image
		IshiharaGenerator ishiharaGenerator = new IshiharaGenerator();
		BufferedImage image = ishiharaGenerator.CreateImage(params.getText(), new Rectangle(params.getRequestedWidth(), params.getRequestedHeight()), false, 4);
		String dstKey = org.apache.commons.codec.digest.DigestUtils.sha256Hex(params.getText()) + ".png";

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
