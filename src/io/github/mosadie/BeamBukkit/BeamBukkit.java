package io.github.mosadie.BeamBukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.mosadie.BeamBukkit.commands.beambukkit;
import pro.beam.api.BeamAPI;
import pro.beam.interactive.net.packet.Protocol;
import pro.beam.interactive.net.packet.Protocol.Report;
import pro.beam.interactive.net.packet.Protocol.Report.JoystickInfo;
import pro.beam.interactive.net.packet.Protocol.Report.ScreenInfo;
import pro.beam.interactive.net.packet.Protocol.Report.TactileInfo;
import pro.beam.interactive.robot.Robot;
import pro.beam.interactive.robot.RobotBuilder;

public class BeamBukkit extends JavaPlugin {
	private FileConfiguration config = this.getConfig();
	private boolean twoFactor;
	private int channelID;
	private Report InteractiveReport;
	private CommandSender debugCS = null;
	private Robot robot;


	@Override
	public void onEnable() {
		this.getCommand("beambukkit").setExecutor(new beambukkit(this));
		
		config.addDefault("configured", false);
		config.addDefault("beam_username", "Username");
		config.addDefault("beam_password", "Password");
		config.addDefault("beam_twofactorrequired", false);
		config.addDefault("beam_twofactorcode", "abcdef");
		config.options().copyDefaults(true);
		saveConfig();

		getLogger().info("Configuration:");
		getLogger().info("Configured: " + config.getBoolean("configured"));
		getLogger().info("Beam Username: " + config.getString("beam_username"));
		getLogger().info("Beam Password: (NOT GOING TO PRINT)");
		getLogger().info("Using Two Factor auth for Beam: " + config.getBoolean("beam_twofactorrequired"));
		twoFactor = config.getBoolean("beam_twofactorrequired");
		getLogger().info("Two Factor Code: (ALSO NOT GOING TO PRINT)");
		
		if (!config.getBoolean("configured")) {
			getLogger().severe("Configuration file not marked as configured! Please make sure to change the value of configured to true!");
			return;
		}
		
		try {
			URL api = new URL("https://beam.pro/api/v1/channels/"+config.getString("beam_username")+"?fields=id");

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

		try {
			BeamAPI beam = new BeamAPI();
			robot = null;
			if (!twoFactor) {
				robot = new RobotBuilder()
						.username(config.getString("beam_username"))
						.password(config.getString("beam_password"))
						.channel(channelID)
						.build(beam)
						.get();
			} else if (twoFactor && config.getString("beam_twofactorcode").length() == 6) {
				robot = new RobotBuilder()
						.username(config.getString("beam_username"))
						.password(config.getString("beam_password"))
						.channel(channelID)
						.twoFactor(config.getString("beam_twofactorcode"))
						.build(beam)
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
			getLogger().warning(e.toString());
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
}