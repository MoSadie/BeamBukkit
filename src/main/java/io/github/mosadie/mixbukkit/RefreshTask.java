package io.github.mosadie.mixbukkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;

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
		HttpPost httpPost = new HttpPost("https://mixer.com/api/v1/oauth/token");
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("grant_type", "refresh_token"));
		formparams.add(new BasicNameValuePair("refresh_token", refresh_token));
		formparams.add(new BasicNameValuePair("client_id", SetupThread.client_id));
		UrlEncodedFormEntity formentity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
		httpPost.setEntity(formentity);
		HttpClient httpClient = MixBukkit.httpClient;
		try {
			JSONObject json = httpClient.execute(httpPost, plugin.getResponseHander());
						
			if (json != null) {
				if (json.containsKey("access_token")) {
					plugin.finishSetup(json,plugin.getServer().getConsoleSender());
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
