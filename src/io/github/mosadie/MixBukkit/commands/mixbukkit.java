package io.github.mosadie.MixBukkit.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.mixer.api.MixerAPI;
import com.mixer.interactive.GameClient;

import io.github.mosadie.MixBukkit.MixBukkit;
//import pro.beam.interactive.net.packet.Protocol.Report;

public class mixbukkit implements CommandExecutor{
	MixBukkit plugin;

	String[] help = {"How to use the MixBukkit command:","/mixbukkit help -> displays this message.","/mixbukkit report -> Displays the last recived report from Mixer","/mixbukkit debug <on or off> -> Enables/Disables debug mode","/mixbukkit setup -> Starts the setup wizard for the Mixer connection","/mixbukkit chat <message> -> Sends a message to Mixer chat as the connected user"};

	public mixbukkit(MixBukkit plugin) {
		this.plugin = plugin;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length==0) {
			sender.sendMessage(help);
			return true;
		}
		switch(args[0]) {
		case "help":
			sender.sendMessage(help);
			return true;
		case "report":
			//Report report = plugin.getInteractiveReport();
			//if (report == null) {
			//	sender.sendMessage("ERROR: No report found! Is your interactive game running on Mixer?");
			//	return true;
			//}
			List<String> results = new ArrayList<String>();
			results.add("Current Report:");
			results.add("Joysticks:");
			//for (int i = 0; i<report.getJoystickCount();i++) results.add("Joystick #" + i);
			results.add("Buttons:");
			//for (int i = 0; i<report.getTactileCount();i++) results.add("Button #" + i + " ID: " + report.getTactile(i).getId() + " People Start Press: " + report.getTactile(i).getPressFrequency());
			results.add("Screens:");
			//for (int i = 0;i<report.getScreenCount();i++) results.add("Screen #" + i);
			results.add("End of report");
			sender.sendMessage(results.toArray(new String[0]));
			return true;
		case "debug":
			if (args.length == 1) {
				sender.sendMessage(help);
				return true;
			}
			switch (args[1]) {
			case "on":
				//plugin.setDebugCS(sender);
				return true;
			case "off":
				//plugin.setDebugCS(null);
				return true;
			default:
				sender.sendMessage("/mixbukkit debug <on or off>");
				return true;
			}
		case "setup":
			if (plugin.mixer != null) {
				plugin.mixer = null;
			}
			if (plugin.gameClient != null) {
				plugin.gameClient.disconnect();
				plugin.gameClient = null;
			}
			plugin.setup(sender);
			return true;
		case "chat":
			if (plugin.mixer == null) {
				sender.sendMessage("Please run /mixbukkit setup before running this command!");
				return true;
			}
			if (args.length == 1) {
				sender.sendMessage(help);
				return true;
			}
			plugin.setupChatIfNotDoneAlready();
			String message = "";
			for (int i = 2; i > args.length; i++) {
				if (i == 2) message = args[i];
				else message = message+" "+args[i];
			}
			plugin.chat(message);
			sender.sendMessage("<"+plugin.user.username+" via Mixer> "+message);
			return true;
		case "status":
			sender.sendMessage("Status of MixBukkit plugin:");
			sender.sendMessage("Mixer API setup: " + (plugin.mixer == null ? "No" : "Yes"));
			sender.sendMessage("Game Client setup: " + (plugin.gameClient == null ? "No" : "Yes"));
			sender.sendMessage("Mixer User found: " + (plugin.user == null ? "No" : "Yes"));
			sender.sendMessage("Mixer Chat found: " + (plugin.chat == null ? "No" : "Yes"));
			sender.sendMessage("Mixer Chat Connection setup: " + (plugin.chatConnectable == null ? "No" : "Yes"));
			return true;
		default: 
			return false;
		}
	}

}
