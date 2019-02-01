package io.github.mosadie.mixbukkit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.bukkit.command.CommandSender;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SetupThread extends Thread {
	
	static final String client_id = "dabece39df722e254692a02e4acedf5137b6c34f380a200e";
	
	final CommandSender cs;
	final MixBukkit mb;
	
	public SetupThread(MixBukkit self, CommandSender sender) {
		mb = self;
		cs = sender;
	}
	
	@Override
	public void run() {
		JSONObject json = new JSONObject();
		json.put("client_id", client_id);
		json.put("scope", "chat:connect chat:chat chat:whisper interactive:robot:self");
		
		HttpClient httpClient = MixBukkit.httpClient;
		boolean finished = false;
		HttpPost httpPost = new HttpPost("https://mixer.com/api/v1/oauth/shortcode");
		StringEntity body = new StringEntity(json.toJSONString(), ContentType.APPLICATION_JSON);
		httpPost.setEntity(body);
		
		try {
			JSONObject resultJSON = httpClient.execute(httpPost, mb.getResponseHander());
			if (resultJSON == null || !resultJSON.containsKey("code")) {
				finished = true;
				cs.sendMessage("Something went wrong, please try again.");
				return;
			}
			String handle = (String) resultJSON.get("handle");
			String code = (String) resultJSON.get("code");
			double time = Double.parseDouble(((Long) resultJSON.get("expires_in")).toString());
			cs.sendMessage("----------------------------------------------------");
			cs.sendMessage("Please go to https://mixer.com/go and type in the code: " + code);
			cs.sendMessage("----------------------------------------------------");
			while (!finished && time > 0) {
				HttpGet httpGet = new HttpGet("https://mixer.com/api/v1/oauth/shortcode/check/" + handle);
				String authCode = httpClient.execute(httpGet, new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse response) throws IOException {
						StatusLine statusLine = response.getStatusLine();
						HttpEntity entity = response.getEntity();
						if (statusLine.getStatusCode() != 200) {
							return "Error: " + statusLine.getStatusCode();
						}
						if (entity == null) {
							return "Error: " + statusLine.getStatusCode();
						}
						JSONParser parser = new JSONParser();
						ContentType contentType = ContentType.getOrDefault(entity);
						Charset charset = contentType.getCharset();
						Reader reader = new InputStreamReader(entity.getContent(), charset);
						JSONObject json;
						try {
							json = (JSONObject) parser.parse(reader);
						} catch (ParseException e) {
							return "Error: " + statusLine.getStatusCode();
						}
						if (json.containsKey("code")) {
							return (String) json.get("code");
						} else {
							return "Error: " + statusLine.getStatusCode();
						}
					}
				});

				if (authCode.startsWith("Error: ")) {
					String statusCode = authCode.split(" ")[1];
					if (statusCode != "204") {
						finished = true;
						cs.sendMessage("Something went wrong, please try again.");
						return;
					}
				} else {
					finished = true;
					cs.sendMessage("OAuth token token received!");
					JSONObject tokenJSON = new JSONObject();
					tokenJSON.put("grant_type", "authorization_code");
					tokenJSON.put("client_id", client_id);
					tokenJSON.put("code", authCode);

					HttpPost oauthPost = new HttpPost("https://mixer.com/api/v1/oauth/token");
					StringEntity oauthEntity = new StringEntity(tokenJSON.toJSONString(), ContentType.APPLICATION_JSON);
					oauthPost.setEntity(oauthEntity);
					JSONObject OAuthJson = httpClient.execute(oauthPost, mb.getResponseHander());

					if (OAuthJson == null || !OAuthJson.containsKey("access_token")) {
						cs.sendMessage("Something went wrong");
						return;
					}
					mb.getConfig().set("refresh_token", (String) OAuthJson.get("refresh_token"));
					mb.saveConfig();
					mb.finishSetup(OAuthJson, cs);
					finished = true;
					return;
				}

				time -= .5;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (time <= 0) {
					cs.sendMessage("Please ignore the previous code, it has now expired.");
					SetupThread st = new SetupThread(mb, cs);
					st.start();
				}
			}
			finished = true;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		cs.sendMessage("Something went wrong, please try again.");
		return;
	}
}
