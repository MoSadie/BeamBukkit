package io.github.mosadie.MixBukkit;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.bukkit.command.CommandSender;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mixer.api.MixerAPI;
import com.mixer.interactive.GameClient;

public class SetupThread extends Thread {
	final CommandSender cs;
	final MixBukkit mb;

	public SetupThread(MixBukkit self, CommandSender sender) {
		mb = self;
		cs = sender;
	}

	@Override
	public void run() {
		JSONObject json = new JSONObject();
		json.put("client_id", "dabece39df722e254692a02e4acedf5137b6c34f380a200e");
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
							cs.sendMessage("OAuth token received!");
							String OAuth = (String) codeJSON.get("code");
							System.out.println(OAuth); //FIXME Delete eventually
							mb.mixer = new MixerAPI(OAuth);
							cs.sendMessage("Setup of Mixer API finished!");
							if (mb.config.getBoolean("configured")) {
								mb.gameClient = new GameClient(mb.config.getInt("mixer_project_version"));
								if (mb.config.getBoolean("mixer_sharecode_needed")) {
									mb.gameClient.connect(OAuth, mb.config.getString("mixer_sharecode"));
									cs.sendMessage("Setup of Game Client finished!");
									cs.sendMessage("All Done!");
									return;
								} else {
									mb.gameClient.connect(OAuth);
									cs.sendMessage("Setup of Game Client finished!");
									cs.sendMessage("All Done!");
								}
							} else {
								cs.sendMessage("All Done!");
								return;
							}
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
