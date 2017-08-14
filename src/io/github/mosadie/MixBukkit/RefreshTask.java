package io.github.mosadie.MixBukkit;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RefreshTask extends BukkitRunnable {

	private MixBukkit plugin;
	private final String refresh_token;
	
	public RefreshTask(MixBukkit mb, String refresh_token) {
		plugin = mb;
		this.refresh_token = refresh_token;
	}

	@Override
	public void run() {
		plugin.getLogger().info("Attempting to use refresh token...");
		JSONObject data = new JSONObject();
		data.put("grant_type", "refresh_token");
		data.put("refresh_token", refresh_token);
		data.put("client_id", SetupThread.client_id);
		try {
			JSONObject json = Request.Post("https://mixer.com/api/v1/oauth/token").bodyString(data.toJSONString(), ContentType.APPLICATION_JSON).execute().handleResponse(new ResponseHandler<JSONObject>() {
				public JSONObject handleResponse(final HttpResponse response) throws IOException {
					HttpEntity entity = response.getEntity();
					if (entity == null) return null;
					String data = IOUtils.toString(entity.getContent());
					JSONParser parser = new JSONParser();
					try {
						return (JSONObject)parser.parse(data);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				return null;
				}
			});
			
			if (json != null) {
				if (json.containsKey("access_token")) {
					plugin.finishSetup(json,plugin.getServer().getConsoleSender());
				}
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
