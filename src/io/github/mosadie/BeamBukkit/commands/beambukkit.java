package io.github.mosadie.BeamBukkit.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import io.github.mosadie.BeamBukkit.BeamBukkit;
import pro.beam.interactive.net.packet.Protocol.Report;

public class beambukkit implements CommandExecutor{
	BeamBukkit plugin;

	String[] help = {"How to use the BeamBukkit command:","/beambukkit help: displays this message.","/beambukkit report: Displays the last recived report from Beam","/beambukkit debug <on or off>: Enables/Disables debug mode"};

	public beambukkit(BeamBukkit plugin) {
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
			Report report = plugin.getInteractiveReport();
			if (report == null) {
				sender.sendMessage("ERROR: No report found! Is your interactive game running on Beam?");
				return true;
			}
			List<String> results = new ArrayList<String>();
			results.add("Current Report:");
			results.add("Joysticks:");
			for (int i = 0; i<report.getJoystickCount();i++) results.add("Joystick #" + i);
			results.add("Buttons:");
			for (int i = 0; i<report.getTactileCount();i++) results.add("Button #" + i + " ID: " + report.getTactile(i).getId() + " People Start Press: " + report.getTactile(i).getPressFrequency());
			results.add("Screens:");
			for (int i = 0;i<report.getScreenCount();i++) results.add("Screen #" + i);
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
				plugin.setDebugCS(sender);
				return true;
			case "off":
				plugin.setDebugCS(null);
				return true;
			default:
				sender.sendMessage("/beambukkit debug <on or off>");
				return true;
			}
		default: 
			return false;
		}
	}

}
