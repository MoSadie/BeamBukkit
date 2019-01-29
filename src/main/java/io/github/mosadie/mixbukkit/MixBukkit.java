package io.github.mosadie.mixbukkit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.eventbus.Subscribe;
import com.mixer.api.MixerAPI;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.methods.WhisperMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import com.mixer.interactive.GameClient;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.event.control.input.ControlMouseUpInputEvent;
import com.mixer.interactive.event.control.input.ControlMoveInputEvent;
import com.mixer.interactive.resources.group.InteractiveGroup;
import com.mixer.interactive.resources.participant.InteractiveParticipant;
import com.mixer.interactive.resources.scene.InteractiveScene;
import com.mixer.interactive.services.GroupServiceProvider;
import com.mixer.interactive.services.ParticipantServiceProvider;
import com.mixer.interactive.services.SceneServiceProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.github.mosadie.mixbukkit.commands.mixbukkit;
import io.github.mosadie.mixbukkit.events.ControlMouseDownInput;
import io.github.mosadie.mixbukkit.events.ControlMouseUpInput;
import io.github.mosadie.mixbukkit.events.ControlMoveInput;

public class MixBukkit extends JavaPlugin {	
	private CloseableHttpClient httpClient = HttpClients.custom()
	.setDefaultRequestConfig(RequestConfig.custom()
	.setCookieSpec(CookieSpecs.STANDARD).build())
	.build();
	
	private ResponseHandler<JSONObject> responseHander = new ResponseHandler<JSONObject>() {
		@Override
		public JSONObject handleResponse(final HttpResponse response) throws IOException {
			StatusLine statusLine = response.getStatusLine();
			HttpEntity entity = response.getEntity();
			if (statusLine.getStatusCode() >= 300) {
				throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
			}
			if (entity == null) {
				throw new ClientProtocolException("Response contains no content");
			}
			JSONParser parser = new JSONParser();
			ContentType contentType = ContentType.getOrDefault(entity);
        	Charset charset = contentType.getCharset();
			Reader reader = new InputStreamReader(entity.getContent(), charset);
			try {
			return (JSONObject)parser.parse(reader);
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
	};
	
	private CommandSender debugCS = null;
	private GameClient gameClient;
	private MixerAPI mixer = null;
	
	private MixerUser user = null;
	private MixerChat chat = null;
	private MixerChatConnectable chatConnectable = null;
	
	private Set<InteractiveGroup> activeGroups;
	
	private RefreshTask task;
	
	public GameClient getGameClient() {
		return gameClient;
	}
	
	public void disposeGameClient() {
		gameClient.disconnect();
		gameClient = null;
	}
	
	public MixerAPI getMixerApi() {
		return mixer;
	}
	
	public void disposeMixerApi() {
		mixer = null;
	}
	
	public MixerUser getMixerUser() {
		return user;
	}
	
	public MixerChat getMixerChat() {
		return chat;
	}
	
	public MixerChatConnectable getMixerChatConnectable() {
		return chatConnectable;
	}
	
	public boolean isMixerApiSetup() {
		return mixer != null;
	}
	
	public boolean isGameClientSetup() {
		return gameClient != null;
	}
	
	
	@Override
	public void onEnable() {
		this.getCommand("mixbukkit").setExecutor(new mixbukkit(this));
		
		getConfig().addDefault("configured", false);
		getConfig().addDefault("mixer_project_version", 0);
		getConfig().addDefault("mixer_sharecode_needed", false);
		getConfig().addDefault("mixer_sharecode", "ShareCode");
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		getLogger().info("Configuration:");
		getLogger().info("Interactive Configured: " + getConfig().getBoolean("configured"));
		getLogger().info("Mixer Project Version: " + getConfig().getString("mixer_project_version"));
		getLogger().info("Using share code: "+ getConfig().getString("mixer_sharecode_needed"));
		getLogger().info("Refresh Token Available: " + (getConfig().contains("refresh_token") ? "Yes" : "No"));
		
		if (getConfig().contains("refresh_token")) {
			task =new RefreshTask(this, (String) getConfig().get("refresh_token"));
			task.run();
		}
	}
	
	@Override 
	public void onDisable() {
		if (gameClient != null) {
			gameClient.ready(false);
			gameClient.disconnect();
		}
		if (chatConnectable != null) {
			chatConnectable.disconnect();
		}
	}
	
	public boolean chat(String message) {
		if (mixer == null) return false;
		setupChatIfNotDoneAlready();
		chatConnectable.send(ChatSendMethod.of(message));
		return true;
	}
	
	public boolean whisper(MixerUser user, String message) {
		if (mixer == null) return false;
		setupChatIfNotDoneAlready();
		chatConnectable.send(WhisperMethod.builder().to(user).send(message).build());
		return true;
	}
	
	@Subscribe
	public void onControlMouseUpInputEvent(ControlMouseUpInputEvent event) {
		ControlMouseUpInput firedEvent = new ControlMouseUpInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		if (debugCS != null) debugCS.sendMessage("Control Mouse Up Input Event Fired! Button "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			event.getTransaction().capture(gameClient);
		}
	}
	
	@Subscribe
	public void onControlMoveInputEvent(ControlMoveInputEvent event) {
		ControlMoveInput firedEvent = new ControlMoveInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		if (debugCS != null) debugCS.sendMessage("New Event Fired! Joystick "+firedEvent.getControlID()+" moved by user " + event.getParticipantID() + " to a position of X:"+event.getX()+" Y:"+event.getY());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			event.getTransaction().capture(gameClient);
		}
	}
	
	@Subscribe
	public void onControlMouseDownInputEvent(ControlMouseDownInputEvent event) {
		ControlMouseDownInput firedEvent = new ControlMouseDownInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		if (debugCS != null) debugCS.sendMessage("New Event Fired! Button "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			event.getTransaction().capture(gameClient);
		}
	}
	
	public Set<InteractiveScene> getScenes() {
		if (gameClient == null) return null;
		try {
			return gameClient.using(SceneServiceProvider.class).getScenes().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	public boolean setScene(InteractiveScene scene, InteractiveGroup groupToChange) {
		if (gameClient == null) return false;
		
		updateGroupArray();
		
		if (activeGroups.isEmpty()) return false;
		
		if (!activeGroups.contains(groupToChange)) {
			gameClient.using(GroupServiceProvider.class).create(groupToChange);
			
		}
		
		groupToChange.setScene(scene).update(gameClient);
		
		return true;
	}
	
	public boolean setGroup(InteractiveGroup group, InteractiveParticipant... users) {
		if (gameClient == null) return false;
		if (users.length == 0) return false;
		
		for (int i = 0; i < users.length; i++) {
			users[i].changeGroup(group);
		}
		
		gameClient.using(ParticipantServiceProvider.class).update(users);
		
		return true;
	}
	
	public void setDebugCS(CommandSender cs) {
		debugCS = cs;
	}
	
	public void setupChatIfNotDoneAlready() {
		if (user != null && chat != null && chatConnectable != null) return;
		setupChat();
	}
	
	public void setupChat() {
		if (mixer == null) return;
		if (user != null) user = null;
		if (chat != null) chat = null;
		if (chatConnectable != null) {
			chatConnectable.disconnect();
			chatConnectable = null;
		}
		if (debugCS != null) debugCS.sendMessage("Setting up Mixer chat connection.");
		try {
			user = mixer.use(UsersService.class).getCurrent().get();
			chat = mixer.use(ChatService.class).findOne(user.channel.id).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		if (chatConnectable == null) {
			chatConnectable = chat.connectable(mixer);
		}
		if (chatConnectable.connect()) {
			chatConnectable.send(AuthenticateMessage.from(user.channel, user, chat.authkey), new ReplyHandler<AuthenticationReply>() {
				public void onSuccess(AuthenticationReply reply) {
					//chatConnectable.send(ChatSendMethod.of("MixBukkit connected!"));
					if (debugCS != null) debugCS.sendMessage("Mixer chat connnected!");
				}
				public void onFailure(Throwable var1) {
					if (debugCS != null) debugCS.sendMessage("Something went wrong connecting to Mixer chat, please try again. Error: " + var1.getMessage());
					var1.printStackTrace();
				}
			});
		}
		
	}
	
	public void updateGroupArray() {
		try {
			activeGroups = gameClient.using(GroupServiceProvider.class).getGroups().get();
		} catch (InterruptedException | ExecutionException e) {
			if (debugCS != null) debugCS.sendMessage("Something went wrong updating group array. Error: " + e.getLocalizedMessage());
		}
		
		if (debugCS != null) {
			debugCS.sendMessage("[MixBukkit] Group array updated. List currently:");
			for (InteractiveGroup group : activeGroups) {
				debugCS.sendMessage("GroupID: "+ group.getGroupID() + " Current SceneID: " + group.getSceneID());
			}
		}
	}
	
	public void setup(CommandSender cs) {
		SetupThread st = new SetupThread(this,cs);
		st.start();
	}
	
	void finishSetup(JSONObject json, CommandSender cs) {
		String OAuth = (String) json.get("access_token");
		getConfig().set("refresh_token", json.get("refresh_token"));
		task = new RefreshTask(this, (String) json.get("refresh_token"));
		task.runTaskLater(this, 19*((Long) json.get("expires_in")));
		saveConfig();
		if (mixer != null) mixer = null;
		if (gameClient != null) {
			gameClient.ready(false);
			gameClient.disconnect();
			gameClient = null;
		}
		mixer = new MixerAPI(OAuth);
		setupChat();
		cs.sendMessage("Setup of Mixer API finished!");
		if (getConfig().getBoolean("configured")) {
			gameClient = new GameClient(getConfig().getInt("mixer_project_version"), SetupThread.client_id);
			if (getConfig().getBoolean("mixer_sharecode_needed")) {
				gameClient.connect(OAuth, getConfig().getString("mixer_sharecode"));
				cs.sendMessage("Setup of Game Client finished!");
				cs.sendMessage("All Done!");
				return;
			} else {
				try {
					gameClient.connect(OAuth).get();
					gameClient.getEventBus().register(this);
					gameClient.ready(true);
					cs.sendMessage("Setup of Game Client finished!");
					cs.sendMessage("All Done!");
				} catch (Exception e) {
					cs.sendMessage("Something went wrong finishing setup, please try again later.");
					e.printStackTrace(System.out);
				}
			}
		} else {
			cs.sendMessage("All Done!");
			return;
		}
	}
	
	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	public ResponseHandler<JSONObject> getResponseHander() {
		return responseHander;
	}
}