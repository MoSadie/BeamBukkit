package io.github.mosadie.MixBukkit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.command.CommandSender;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mixer.api.MixerAPI;
import com.mixer.interactive.GameClient;

public class SetupThread extends Thread {
	
	public static final String client_id = "dabece39df722e254692a02e4acedf5137b6c34f380a200e";
	
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
		boolean finished = false;
		try {
			String result = Request.Post("https://mixer.com/api/v1/oauth/shortcode").bodyString(json.toJSONString(), ContentType.APPLICATION_JSON).execute().returnContent().asString();
			JSONParser parser = new JSONParser();
			try {
				JSONObject resultJSON = (JSONObject) parser.parse(result);
				if (!resultJSON.containsKey("code")) {
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
					Content content = Request.Get("https://mixer.com/api/v1/oauth/shortcode/check/"+handle).execute().returnContent();
					String response;
					if (content == null) { 
						response = "{\"statusCode\":204}";
					}
					else {
						response = content.asString();
					}
					JSONObject codeJSON = (JSONObject) parser.parse(response);
					long statusCode;
					if (content == null) {
						statusCode = (long) codeJSON.get("statusCode");
					}
					else {
						statusCode = 200;
					}
					if (statusCode == 200L) {
						if (codeJSON.containsKey("code")) {
							finished = true;
							cs.sendMessage("OAuth token token received!");
							String AuthCode = (String) codeJSON.get("code");
							JSONObject tokenJSON = new JSONObject();
							tokenJSON.put("grant_type", "authorization_code");
							tokenJSON.put("client_id", client_id);
							tokenJSON.put("code", AuthCode);
							//tokenJSON.put("redirect_uri", "https://mosadie.github.io/MixBukkitSuccess");
							ResponseHandler<String> rh = new ResponseHandler<String>() {
								@Override
								public String handleResponse(HttpResponse response)
										throws ClientProtocolException, IOException {
									StatusLine statusLine = response.getStatusLine();
							        HttpEntity entity = response.getEntity();
							        if (entity == null) {
							        	throw new ClientProtocolException("Response contains no content");
							        }
							        String output = IOUtils.toString(entity.getContent());
							        System.out.println("OauthJSON: "+output);
							        return output;
								}
							};
							String oauthJSON = Request.Post("https://mixer.com/api/v1/oauth/token").bodyString(tokenJSON.toJSONString(), ContentType.APPLICATION_JSON).execute().handleResponse(rh);
							//Response debugresponse = Request.Post("http://mixer.com/api/v1/oauth/token").bodyString(tokenJSON.toJSONString(), ContentType.APPLICATION_JSON).execute();
							//String oauthJSON = response.returnContent().asString();
							JSONObject OAuthJson = (JSONObject) parser.parse(oauthJSON);
							if (!OAuthJson.containsKey("access_token")) {
								cs.sendMessage("Something went wrong");
								return;
							}
							mb.config.set("refresh_token", (String) OAuthJson.get("refresh_token"));
							mb.saveConfig();
							mb.finishSetup(OAuthJson,cs);
							finished=true;
							return;
						} else {
							finished = true;
							return;
						}
					} else if (statusCode != 204L) {
						finished = true;
						cs.sendMessage("Something went wrong, please try again.");
						return;
					}
					time -= .5;
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (time <= 0) {
						cs.sendMessage("Please ignore the previous code, it has now expired.");
						SetupThread st = new SetupThread(mb,cs);
						st.start();
					}
				}
				finished = true;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cs.sendMessage("Something went wrong, please try again.");
		return;
	}
}
