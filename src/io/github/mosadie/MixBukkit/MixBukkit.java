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
import io.github.mosadie.MixBukkit.events.ControlMouseDownInput;

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
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import com.mixer.interactive.exception.InteractiveReplyWithErrorException;
import com.mixer.interactive.exception.InteractiveRequestNoReplyException;

public class MixBukkit extends JavaPlugin {
	public FileConfiguration config = this.getConfig();
	//private CommandSender debugCS = null;
	//private Robot robot;
	public GameClient gameClient;
	public MixerAPI mixer = null;

	public MixerUser user = null;
	public MixerChat chat = null;
	public MixerChatConnectable chatConnectable = null;


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
			getLogger().severe("Configuration file not marked as configured! Please make sure to change the value of configured to true! You will not be able to use interactive features!");
			return;
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
	public void onControlMouseDownEvent(ControlMouseDownInputEvent event) {
		ControlMouseDownInput firedEvent = new ControlMouseDownInput(this,event);
		Bukkit.getServer().getPluginManager().callEvent(firedEvent);
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
			System.out.println("---------------------");
			System.out.println(((com.mixer.api.http.HttpBadResponseException) e.getCause()).response.status());
		}
		if (chatConnectable == null) {
			chatConnectable = chat.connectable(mixer);
		}
		if (chatConnectable.connect()) {
			chatConnectable.send(AuthenticateMessage.from(user.channel, user, chat.authkey), new ReplyHandler<AuthenticationReply>() {
				public void onSuccess(AuthenticationReply reply) {
					chatConnectable.send(ChatSendMethod.of("MixBukkit connected!"));
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
}