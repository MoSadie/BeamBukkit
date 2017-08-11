package io.github.mosadie.MixBukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.github.mosadie.MixBukkit.commands.mixbukkit;

import com.google.common.eventbus.Subscribe;
import com.mixer.api.MixerAPI;
import com.mixer.interactive.GameClient;
import com.mixer.interactive.event.InteractiveEvent;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.exception.InteractiveReplyWithErrorException;
import com.mixer.interactive.exception.InteractiveRequestNoReplyException;

public class MixBukkit extends JavaPlugin {
	public FileConfiguration config = this.getConfig();
	//private int channelID;
	//private Report InteractiveReport;
	//private CommandSender debugCS = null;
	//private Robot robot;
	public GameClient gameClient;
	public MixerAPI mixer = null;


	@Override
	public void onEnable() {
		this.getCommand("mixbukkit").setExecutor(new mixbukkit(this));

		config.addDefault("configured", false);
		config.addDefault("mixer_project_version", 0);
		config.addDefault("mixer_sharecode_needed", false);
		config.addDefault("mixer_sharecode", "ShareCode");
		config.options().copyDefaults(true);
		saveConfig();

		getLogger().info("Configuration:");
		getLogger().info("Configured: " + config.getBoolean("configured"));
		getLogger().info("Mixer Project Version: " + config.getString("mixer_project_version"));
		getLogger().info("Using share code: "+ config.getString("mixer_sharecode_needed"));
		if (!config.getBoolean("configured")) {
			getLogger().severe("Configuration file not marked as configured! Please make sure to change the value of configured to true!");
			return;
		}
	}
		@Subscribe
		public void onControlMouseDownEvent(ControlMouseDownInputEvent event) {
			if (event.getTransaction() != null) {
				try {
					event.getTransaction().capture(gameClient);
				} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		/*
		try {
			URL api = new URL("https://mixer.com/api/v1/channels/"+config.getString("mixer_username")+"?fields=id");

			URLConnection connection = api.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String response = in.readLine();
			in.close();
			if (response != null) {
				String[] responses = response.split(":");
				if (responses[0].equals("{\"message\"")) {
					getLogger().severe("There was a problem getting the channel ID:" + responses[1].split(",")[0]);
					Bukkit.getServer().getPluginManager().disablePlugin(this);
					return;
				}
				if (responses[0].equals("{\"id\"")) channelID = Integer.parseInt(responses[1].split("}")[0]);
			}
		} catch (Exception e) {
			getLogger().warning(e.toString());
		}
		getLogger().info("Channel ID: "+channelID);
		try {
			MixerAPI mixer = new MixerAPI();
			robot = null;
			if (!twoFactor) {
				robot = new RobotBuilder()
						.username(config.getString("mixer_username"))
						.password(config.getString("mixer_password"))
						.channel(channelID)
						.build(mixer)
						.get();
			} else if (twoFactor && config.getString("mixer_twofactorcode").length() == 6) {
				robot = new RobotBuilder()
						.username(config.getString("mixer_username"))
						.password(config.getString("mixer_password"))
						.channel(channelID)
						.twoFactor(config.getString("mixer_twofactorcode"))
						.build(mixer)
						.get();
			}
			if (robot!=null)
				robot.on(Protocol.Report.class, report -> {
					InteractiveReport = report;
					if (debugCS != null) {
						List<String> results = new ArrayList<String>();
						results.add("Current Report:");
						results.add("Joysticks:");
						for (int i = 0; i<report.getJoystickCount();i++) results.add("Joystick #" + i);
						results.add("Buttons:");
						for (int i = 0; i<report.getTactileCount();i++) results.add("Button #" + i + " ID: " + report.getTactile(i).getId() + " People Start Press: " + report.getTactile(i).getPressFrequency());
						results.add("Screens:");
						for (int i = 0;i<report.getScreenCount();i++) results.add("Screen #" + i);
						results.add("End of report");
						debugCS.sendMessage(results.toArray(new String[0]));
					}
					ReportEvent reportEvent = new ReportEvent(report, this);
					Bukkit.getServer().getPluginManager().callEvent(reportEvent);
				});
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public Report getInteractiveReport() {
		if (InteractiveReport != null)
			return InteractiveReport;
		else
			return null;
	}

	public void setDebugCS(CommandSender cs) {
		debugCS = cs;
	}

	public void updateState(String state) {
        if (robot != null) {
            Protocol.ProgressUpdate.Builder progressBuilder = Protocol.ProgressUpdate.newBuilder();
            progressBuilder.setState(state);

            try {
                if (robot.isOpen()) {
                    robot.write(progressBuilder.build());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
		 */

	public String getOAuthToken(CommandSender cs) {
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
					return "ERROR";
				}
				String handle = (String) resultJSON.get("handle");
				String code = (String) resultJSON.get("code");
				double time = Double.parseDouble(((Long) resultJSON.get("expires_in")).toString());
				cs.sendMessage("----------------------------------------------------------------");
				cs.sendMessage("Please go to https://mixer.com/go and type in the code: " + code);
				cs.sendMessage("----------------------------------------------------------------");
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
							return (String) codeJSON.get("code");
						} else {
							finished = true;
							return "ERROR";
						}
					} else if (statusCode != 204L) {
						finished = true;
						return "ERROR";
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
						return getOAuthToken(cs);
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
		return "ERROR";
	}
}