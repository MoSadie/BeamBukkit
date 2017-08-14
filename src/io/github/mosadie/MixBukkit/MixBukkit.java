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
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
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
import io.github.mosadie.MixBukkit.events.ControlInputEventBase;
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
import com.mixer.interactive.event.InteractiveEvent;
import com.mixer.interactive.event.control.input.ControlInputEvent;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.event.control.input.ControlMouseUpInputEvent;
import com.mixer.interactive.event.control.input.ControlMoveInputEvent;
import com.mixer.interactive.exception.InteractiveReplyWithErrorException;
import com.mixer.interactive.exception.InteractiveRequestNoReplyException;
import com.mixer.interactive.gson.ControlInputEventAdapter;

public class MixBukkit extends JavaPlugin {
	public FileConfiguration config = this.getConfig();
	//private CommandSender debugCS = null;
	//private Robot robot;
	public GameClient gameClient;
	public MixerAPI mixer = null;

	public MixerUser user = null;
	public MixerChat chat = null;
	public MixerChatConnectable chatConnectable = null;
	
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
	
	@Subscribe
	public void workaroundEventCallerBecauseEventsAreBroken(ControlInputEvent event) {
		//gameClient.GSON.fromJson(event.getControlInput().getRawInput()., ControlInputEvent.class);
		switch (event.getControlInput().getEvent()) {
        case "mousedown": {
            onControlMouseDownInputEvent(new ControlMouseDownInputEvent(event.getParticipantID(), event.getTransaction() == null ? null : event.getTransaction().getTransactionID(), event.getControlInput()));
        }
        case "mouseup": {
            onControlMouseUpInputEvent(new ControlMouseUpInputEvent(event.getParticipantID(), event.getTransaction() == null ? null : event.getTransaction().getTransactionID(), event.getControlInput()));
        }
        case "move": {
            onControlMoveInputEvent(new ControlMoveInputEvent(event.getParticipantID(), event.getTransaction() == null ? null : event.getTransaction().getTransactionID(), event.getControlInput()));
        }
        default:
            onGenericControlInputEvent(event);
    }
	}
	
	
	public void onGenericControlInputEvent(ControlInputEvent event) {
		ControlInputEventBase firedEvent = new ControlInputEventBase(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		System.out.println("New GENERIC Event Fired! Thing "+firedEvent.getControlID()+" triggered by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void onControlMouseUpInputEvent(ControlMouseUpInputEvent event) {
		ControlMouseUpInput firedEvent = new ControlMouseUpInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		System.out.println("New Event Fired! Button "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void onControlMoveInputEvent(ControlMoveInputEvent event) {
		ControlMoveInput firedEvent = new ControlMoveInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		System.out.println("New Event Fired! Joystick "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void onControlMouseDownInputEvent(ControlMouseDownInputEvent event) {
		ControlMouseDownInput firedEvent = new ControlMouseDownInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
		System.out.println("New Event Fired! Button "+firedEvent.getControlID()+" pressed by user " + event.getParticipantID());
		if (event.getTransaction() != null && !firedEvent.isCancelled()) {
			try {
				event.getTransaction().capture(gameClient);
			} catch (InteractiveRequestNoReplyException | InteractiveReplyWithErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/*
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
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
	
	public void setup(CommandSender cs) {
		SetupThread st = new SetupThread(this,cs);
		st.start();
	}
	
	public void finishSetup(JSONObject json, CommandSender cs) {
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