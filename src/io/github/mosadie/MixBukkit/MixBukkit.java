package io.github.mosadie.MixBukkit;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import io.github.mosadie.MixBukkit.commands.mixbukkit;
import io.github.mosadie.MixBukkit.events.ControlMouseDownInput;
import io.github.mosadie.MixBukkit.events.ControlMouseUpInput;
import io.github.mosadie.MixBukkit.events.ControlMoveInput;

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
import com.mixer.interactive.exception.InteractiveReplyWithErrorException;
import com.mixer.interactive.exception.InteractiveRequestNoReplyException;
import com.mixer.interactive.resources.group.InteractiveGroup;
import com.mixer.interactive.resources.participant.InteractiveParticipant;
import com.mixer.interactive.resources.scene.InteractiveScene;
import com.mixer.interactive.services.GroupServiceProvider;
import com.mixer.interactive.services.ParticipantServiceProvider;
import com.mixer.interactive.services.SceneServiceProvider;

public class MixBukkit extends JavaPlugin {
	public FileConfiguration config = this.getConfig();
	private CommandSender debugCS = null;
	public GameClient gameClient;
	public MixerAPI mixer = null;

	public MixerUser user = null;
	public MixerChat chat = null;
	public MixerChatConnectable chatConnectable = null;
	
	private Set<InteractiveGroup> activeGroups;

	private RefreshTask task;


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
		getLogger().info("Interactive Configured: " + config.getBoolean("configured"));
		getLogger().info("Mixer Project Version: " + config.getString("mixer_project_version"));
		getLogger().info("Using share code: "+ config.getString("mixer_sharecode_needed"));
		getLogger().info("Refresh Token Available: " + (config.contains("refresh_token") ? "Yes" : "No"));

		if (config.contains("refresh_token")) {
			task =new RefreshTask(this, (String) config.get("refresh_token"));
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

	/*
	@Subscribe
	public void onGenericControlInputEvent(ControlInputEvent event) {
		ControlInputEventBase firedEvent = new ControlInputEventBase(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		System.out.println("New GENERIC Event Fired! Thing "+firedEvent.getControlID()+" triggered by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				e.printStackTrace();
			}
		}
	}
	 */

	@Subscribe
	public void onControlMouseUpInputEvent(ControlMouseUpInputEvent event) {
		ControlMouseUpInput firedEvent = new ControlMouseUpInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		if (debugCS != null) debugCS.sendMessage("Control Mouse Up Input Event Fired! Button "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				e.printStackTrace();
			}
		}
	}

	@Subscribe
	public void onControlMoveInputEvent(ControlMoveInputEvent event) {
		ControlMoveInput firedEvent = new ControlMoveInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		if (debugCS != null) debugCS.sendMessage("New Event Fired! Joystick "+firedEvent.getControlID()+" moved by user " + event.getParticipantID() + " to a position of X:"+event.getX()+" Y:"+event.getY());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				e.printStackTrace();
			}
		}
	}

	@Subscribe
	public void onControlMouseDownInputEvent(ControlMouseDownInputEvent event) {
		ControlMouseDownInput firedEvent = new ControlMouseDownInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		if (debugCS != null) debugCS.sendMessage("New Event Fired! Button "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Set<InteractiveScene> getScenes() {
		if (gameClient == null) return null;
		try {
			return gameClient.using(SceneServiceProvider.class).getScenes();
		} catch (InteractiveReplyWithErrorException e) {
			e.printStackTrace();
			return null;
		} catch (InteractiveRequestNoReplyException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean setScene(InteractiveScene scene, InteractiveGroup groupToChange) {
		if (gameClient == null) return false;

		updateGroupArray();
		
		if (activeGroups.isEmpty()) return false;
		
		if (!activeGroups.contains(groupToChange)) {
			try {
				gameClient.using(GroupServiceProvider.class).createGroups(groupToChange);
			} catch (InteractiveReplyWithErrorException | InteractiveRequestNoReplyException e) {
				if (debugCS != null) debugCS.sendMessage("Something went wrong creating new group. Error: " + e.getLocalizedMessage());
				return false;
			}
		}
		try {
			groupToChange.setScene(scene).update(gameClient);
		} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
			if (debugCS != null) debugCS.sendMessage("Something went wrong updating group with new scene. Error: "+ e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	public boolean setGroup(InteractiveGroup group, InteractiveParticipant... users) {
		if (gameClient == null) return false;
		if (users.length == 0) return false;

		for (int i = 0; i < users.length; i++) {
			users[i].changeGroup(group);
		}
		try {
			gameClient.using(ParticipantServiceProvider.class).updateParticipants(users);
		} catch (InteractiveReplyWithErrorException | InteractiveRequestNoReplyException e) {
			if (debugCS != null) debugCS.sendMessage("Something went wrong changing the group of users. Error: " + e.getLocalizedMessage());
			return false;
		}
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
				}
				public void onFailure(Throwable var1) {
					var1.printStackTrace();
				}
			});
		}

	}
	
	public void updateGroupArray() {
		try {
			activeGroups = gameClient.using(GroupServiceProvider.class).getGroups();
		} catch (InteractiveReplyWithErrorException | InteractiveRequestNoReplyException e) {
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
		config.set("refresh_token", json.get("refresh_token"));
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
		cs.sendMessage("Setup of Mixer API finished!");
		if (config.getBoolean("configured")) {
			gameClient = new GameClient(config.getInt("mixer_project_version"));
			if (config.getBoolean("mixer_sharecode_needed")) {
				gameClient.connect(OAuth, config.getString("mixer_sharecode"));
				cs.sendMessage("Setup of Game Client finished!");
				cs.sendMessage("All Done!");
				return;
			} else {
				gameClient.connect(OAuth);
				gameClient.getEventBus().register(this);
				gameClient.ready(true);
				cs.sendMessage("Setup of Game Client finished!");
				cs.sendMessage("All Done!");
			}
		} else {
			cs.sendMessage("All Done!");
			return;
		}
	}
}